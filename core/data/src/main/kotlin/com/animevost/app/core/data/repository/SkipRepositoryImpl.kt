package com.animevost.app.core.data.repository

import com.animevost.app.core.data.db.MalMappingDao
import com.animevost.app.core.data.db.MalMappingEntity
import com.animevost.app.core.data.db.SkipSegmentDao
import com.animevost.app.core.data.mapper.toDomain
import com.animevost.app.core.data.mapper.toEntity
import com.animevost.app.core.domain.model.SkipSegment
import com.animevost.app.core.domain.model.SkipSource
import com.animevost.app.core.domain.model.SkipType
import com.animevost.app.core.domain.repository.SkipRepository
import com.animevost.app.core.domain.util.Result
import com.animevost.app.core.network.AniSkipApi
import com.animevost.app.core.network.JikanAnime
import com.animevost.app.core.network.JikanApi
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements [SkipRepository] by combining a local Room cache with the
 * remote AniSkip API (for automated intro/outro detection) and the Jikan
 * API (for MAL ID resolution).
 *
 * Resolution order for [getSkipSegments]:
 * 1. USER-marked segments (always take priority per type).
 * 2. Cached ANISKIP segments (used when episodeLengthMs == 0).
 * 3. Fresh AniSkip fetch (always when episodeLengthMs > 0, for accuracy).
 * 4. Any cached ANISKIP on network failure.
 */
@Singleton
class SkipRepositoryImpl @Inject constructor(
    private val aniSkipApi: AniSkipApi,
    private val jikanApi: JikanApi,
    private val malMappingDao: MalMappingDao,
    private val skipSegmentDao: SkipSegmentDao,
) : SkipRepository {

    override suspend fun getSkipSegments(
        animeId: Int,
        episodeName: String,
        episodeLengthMs: Long,
    ): Result<List<SkipSegment>> {
        return try {
            val allLocal = skipSegmentDao.getSegments(animeId, episodeName)
            val userSegments = allLocal.filter { it.source == "USER" }.map { it.toDomain() }
            val coveredByUser = userSegments.map { it.type }.toSet()

            val missingTypes = ALL_TYPES.filter { it !in coveredByUser }
            if (missingTypes.isEmpty()) return Result.Success(userSegments)

            // With real duration: re-fetch AniSkip for accuracy (skips USER-covered types).
            // Without duration: use cached ANISKIP to avoid unnecessary network calls.
            val cachedAniSkip = allLocal
                .filter { it.source == "ANISKIP" }
                .map { it.toDomain() }
                .filter { it.type in missingTypes }

            val aniSkipSegments = if (episodeLengthMs <= 0L && cachedAniSkip.isNotEmpty()) {
                cachedAniSkip
            } else {
                fetchFromAniSkip(animeId, episodeName, missingTypes, episodeLengthMs)
                    .ifEmpty { cachedAniSkip }
            }

            Result.Success(userSegments + aniSkipSegments)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    private suspend fun fetchFromAniSkip(
        animeId: Int,
        episodeName: String,
        types: List<SkipType>,
        episodeLengthMs: Long,
    ): List<SkipSegment> {
        val episodeNumber = parseEpisodeNumber(episodeName) ?: return emptyList()
        val malId = getCachedMalId(animeId) ?: return emptyList()
        val episodeLengthSec = if (episodeLengthMs > 0L) episodeLengthMs / 1000L else 0L

        return try {
            val response = aniSkipApi.getSkipTimes(
                malId = malId,
                episode = episodeNumber,
                types = types.map { if (it == SkipType.INTRO) "op" else "ed" },
                episodeLength = episodeLengthSec,
            )
            if (!response.found) return emptyList()

            response.results.mapNotNull { result ->
                val type = when (result.skipType) {
                    "op" -> SkipType.INTRO
                    "ed" -> SkipType.OUTRO
                    else -> null
                } ?: return@mapNotNull null
                if (type !in types) return@mapNotNull null

                SkipSegment(
                    type = type,
                    startMs = (result.interval.startTime * 1000).toLong(),
                    endMs = (result.interval.endTime * 1000).toLong(),
                    source = SkipSource.ANISKIP,
                ).also { skipSegmentDao.insertSegment(it.toEntity(animeId, episodeName)) }
            }
        } catch (e: Exception) {
            Timber.w(e, "AniSkip fetch failed for animeId=$animeId ep=$episodeName")
            emptyList()
        }
    }

    override suspend fun saveSkipSegment(
        animeId: Int,
        episodeName: String,
        segment: SkipSegment,
    ): Result<Unit> {
        return try {
            skipSegmentDao.insertSegment(segment.toEntity(animeId, episodeName))
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    /**
     * Resolves a MAL ID by searching Jikan, scored by Dice coefficient similarity.
     * Falls back to [title] (Russian) if [titleOriginal] yields a weak match.
     * Results cached for [MAL_MAPPING_TTL_MS]; on change, stale ANISKIP data is cleared.
     */
    suspend fun resolveMalId(
        animeId: Int,
        titleOriginal: String,
        title: String,
        year: String?,
        type: String?,
    ): Int? {
        getCachedMalId(animeId)?.let { return it }

        val yearInt = year?.filter { it.isDigit() }?.take(4)?.toIntOrNull()
        val primaryQuery = titleOriginal.ifBlank { title }
        if (primaryQuery.isBlank()) return null

        return try {
            val malId = searchAndScore(primaryQuery, yearInt, type)
                ?: if (titleOriginal.isNotBlank() && title.isNotBlank() && titleOriginal != title) {
                    searchAndScore(title, yearInt, type)
                } else null

            malId?.also { id ->
                val existing = malMappingDao.getMapping(animeId)
                if (existing?.malId != id) {
                    // MAL ID changed — invalidate stale ANISKIP segments
                    skipSegmentDao.deleteAniSkipSegments(animeId)
                }
                malMappingDao.insertMapping(MalMappingEntity(animeId, id, primaryQuery))
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun searchAndScore(query: String, yearInt: Int?, type: String?): Int? {
        val response = jikanApi.searchAnime(query, limit = 10)
        if (response.data.isEmpty()) return null

        data class Scored(val malId: Int, val score: Double)

        val scored = response.data.map { anime ->
            Scored(anime.mal_id, scoreCandidate(query, anime, yearInt, type))
        }.sortedByDescending { it.score }

        val best = scored.firstOrNull() ?: return null
        val second = scored.getOrNull(1)

        // Accept when score is strong enough and clearly better than second candidate
        val gap = best.score - (second?.score ?: 0.0)
        val strongYear = yearInt != null && response.data.find { it.mal_id == best.malId }?.year == yearInt
        val strongType = type != null && response.data.find { it.mal_id == best.malId }
            ?.type?.equals(type, ignoreCase = true) == true

        return when {
            best.score >= MIN_TITLE_SCORE && gap >= MIN_SCORE_GAP -> best.malId
            best.score >= MIN_TITLE_SCORE && (strongYear || strongType) -> best.malId
            else -> null
        }
    }

    private fun scoreCandidate(query: String, anime: JikanAnime, yearInt: Int?, type: String?): Double {
        val titleScore = maxOf(
            titleSimilarity(query, anime.title),
            titleSimilarity(query, anime.title_english ?: ""),
            titleSimilarity(query, anime.title_japanese ?: ""),
        )
        val yearBonus = if (yearInt != null && anime.year == yearInt) 0.2 else 0.0
        val typeBonus = if (type != null && anime.type?.equals(type, ignoreCase = true) == true) 0.1 else 0.0
        return titleScore + yearBonus + typeBonus
    }

    /**
     * Dice coefficient (Sørensen–Dice): 2|A∩B| / (|A| + |B|).
     * More lenient than Jaccard when the query is a short subset of a longer title,
     * e.g. "Re:Zero" (2 tokens) vs "Re:Zero - Starting Life in Another World" (7 tokens)
     * scores 0.44 instead of Jaccard's 0.29.
     */
    private fun titleSimilarity(a: String, b: String): Double {
        val tokensA = a.lowercase().split(Regex("\\W+")).filter { it.length > 1 }.toSet()
        val tokensB = b.lowercase().split(Regex("\\W+")).filter { it.length > 1 }.toSet()
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0
        val intersection = tokensA.intersect(tokensB).size.toDouble()
        return 2.0 * intersection / (tokensA.size + tokensB.size)
    }

    private suspend fun getCachedMalId(animeId: Int): Int? {
        val mapping = malMappingDao.getMapping(animeId) ?: return null
        if (System.currentTimeMillis() - mapping.updatedAt > MAL_MAPPING_TTL_MS) return null
        return mapping.malId
    }

    companion object {
        private val ALL_TYPES = listOf(SkipType.INTRO, SkipType.OUTRO)

        // Priority 1: explicit "ep N" / "episode N" / "e03" / "S2E3" markers
        private val EXPLICIT_EP_REGEX = Regex("""(?:ep(?:isode)?|ova)\.?\s*(\d+)""", RegexOption.IGNORE_CASE)
        private val SEASON_EP_REGEX = Regex("""s\d+\s*e(\d+)""", RegexOption.IGNORE_CASE)

        // Tokens to strip before fallback number extraction
        private val QUALITY_PATTERN = Regex("""\b(2160|1080|720|480|360)p?\b""", RegexOption.IGNORE_CASE)
        private val YEAR_PATTERN = Regex("""\b(19|20)\d{2}\b""")
        private val RANGE_PATTERN = Regex("""(\d+)\s*[-–]\s*\d+""")

        private const val MIN_TITLE_SCORE = 0.30
        private const val MIN_SCORE_GAP = 0.10
        private const val MAL_MAPPING_TTL_MS = 30L * 24 * 60 * 60 * 1000 // 30 days

        fun parseEpisodeNumber(episodeName: String): Int? {
            // Priority 1: explicit episode markers (ep3, episode 3, e03, s2e3)
            EXPLICIT_EP_REGEX.find(episodeName)?.groupValues?.get(1)?.toIntOrNull()
                ?.let { return it }
            SEASON_EP_REGEX.find(episodeName)?.groupValues?.get(1)?.toIntOrNull()
                ?.let { return it }

            // Priority 2: strip quality/year noise, then first number (handle ranges)
            val cleaned = episodeName
                .replace(QUALITY_PATTERN, " ")
                .replace(YEAR_PATTERN, " ")
            RANGE_PATTERN.find(cleaned)?.groupValues?.get(1)?.toIntOrNull()
                ?.let { return it }

            return Regex("""(\d+)""").find(cleaned)?.groupValues?.get(1)?.toIntOrNull()
        }
    }
}


