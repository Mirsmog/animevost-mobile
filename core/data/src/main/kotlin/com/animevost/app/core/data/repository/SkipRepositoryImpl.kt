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
import com.animevost.app.core.network.JikanApi
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements [SkipRepository] by combining a local Room cache with the
 * remote AniSkip API (for automated intro/outro detection) and the Jikan
 * API (for MAL ID resolution).
 *
 * Resolution order for [getSkipSegments]:
 * 1. USER-marked segments cached in Room (always take priority).
 * 2. AniSkip segments fetched via MAL ID (cached for offline use).
 * 3. Any previously cached AniSkip segments on network failure.
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
    ): Result<List<SkipSegment>> {
        return try {
            // 1. Try cached local segments first (user-marked always take priority)
            val local = skipSegmentDao.getSegments(animeId, episodeName)
                .filter { it.source == "USER" }
            if (local.isNotEmpty()) return Result.Success(local.map { it.toDomain() })

            // 2. Try AniSkip via MAL ID
            val episodeNumber = parseEpisodeNumber(episodeName)
                ?: return Result.Success(emptyList())
            val malId = getCachedMalId(animeId)
                ?: return Result.Success(emptyList())

            val segments = try {
                val response = aniSkipApi.getSkipTimes(malId, episodeNumber)
                if (!response.found) {
                    emptyList()
                } else {
                    response.results.mapNotNull { result ->
                        val type = when (result.skipType) {
                            "op" -> SkipType.INTRO
                            "ed" -> SkipType.OUTRO
                            else -> null
                        } ?: return@mapNotNull null

                        val segment = SkipSegment(
                            type = type,
                            startMs = (result.interval.startTime * 1000).toLong(),
                            endMs = (result.interval.endTime * 1000).toLong(),
                            source = SkipSource.ANISKIP,
                        )
                        // Cache for offline use
                        skipSegmentDao.insertSegment(segment.toEntity(animeId, episodeName))
                        segment
                    }
                }
            } catch (_: Exception) {
                // Fallback to any cached AniSkip segments
                skipSegmentDao.getSegments(animeId, episodeName).map { it.toDomain() }
            }

            Result.Success(segments)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
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
     * Resolves a MAL ID by searching Jikan with the anime title.
     * Results are cached in [MalMappingDao] for subsequent calls.
     * Called from the ViewModel, which has access to the full [AnimeDetail].
     *
     * @return the MAL ID, or `null` if the title is blank or the search yields no results.
     */
    suspend fun resolveMalId(
        animeId: Int,
        titleOriginal: String,
        title: String,
        year: String?,
        type: String?,
    ): Int? {
        getCachedMalId(animeId)?.let { return it }

        // Search Jikan by original title (usually romaji/japanese)
        val query = titleOriginal.ifBlank { title }
        if (query.isBlank()) return null

        return try {
            val response = jikanApi.searchAnime(query, limit = 5)
            val yearInt = year?.filter { it.isDigit() }?.take(4)?.toIntOrNull()

            // Find best match: prefer same year and type
            val match = response.data.firstOrNull { anime ->
                val yearMatch = yearInt == null || anime.year == null || anime.year == yearInt
                val typeMatch = type == null || anime.type == null ||
                    anime.type.equals(type, ignoreCase = true)
                yearMatch && typeMatch
            } ?: response.data.firstOrNull()

            match?.mal_id?.also { malId ->
                malMappingDao.insertMapping(
                    MalMappingEntity(
                        animeId = animeId,
                        malId = malId,
                        title = query,
                    ),
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Returns the cached MAL ID for [animeId], or `null` if not yet resolved. */
    private suspend fun getCachedMalId(animeId: Int): Int? =
        malMappingDao.getMapping(animeId)?.malId

    companion object {
        private val EPISODE_NUMBER_REGEX = Regex("""(\d+)""")

        fun parseEpisodeNumber(episodeName: String): Int? {
            return EPISODE_NUMBER_REGEX.find(episodeName)?.groupValues?.get(1)?.toIntOrNull()
        }
    }
}
