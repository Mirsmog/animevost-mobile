package com.animevost.app.core.data.repository

import com.animevost.app.core.data.db.MalMappingDao
import com.animevost.app.core.data.db.MalMappingEntity
import com.animevost.app.core.data.db.SkipTimeDao
import com.animevost.app.core.data.db.SkipTimeEntity
import com.animevost.app.core.data.db.YummyMappingDao
import com.animevost.app.core.data.db.YummyMappingEntity
import com.animevost.app.core.domain.model.SkipInterval
import com.animevost.app.core.domain.model.SkipType
import com.animevost.app.core.domain.repository.SkipTimesRepository
import com.animevost.app.core.network.alloha.AllohaSkipClient
import com.animevost.app.core.network.alloha.AllohaSkipRange
import com.animevost.app.core.network.alloha.YummyAnimeSearchClient
import com.animevost.app.core.network.aniskip.AniSkipClient
import com.animevost.app.core.network.aniskip.AniSkipLookup
import com.animevost.app.core.network.animethemes.AnimeThemesClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Resolves skip intervals via yummyanime.tv's Alloha JSON player and uses
 * AniSkip metadata to distinguish openings from endings.
 *
 * Pipeline:
 *  1. Hit the local Room cache (`skip_times`).
 *  2. Resolve animeVost id → yummyanime id (cached in `yummy_mapping`).
 *  3. Run [AllohaSkipClient] which loads the iframe, builds the Borth header
 *     and calls `POST /bnsi/movies/{idFile}` to get the raw `skipTime` string.
 *  4. Match the raw Alloha ranges against AniSkip labels without replacing
 *     the Alloha timing.
 *  5. Persist resolved intervals back to the cache.
 */
class SkipTimesRepositoryImpl @Inject constructor(
    private val searchClient: YummyAnimeSearchClient,
    private val skipClient: AllohaSkipClient,
    private val aniSkipClient: AniSkipClient,
    private val animeThemesClient: AnimeThemesClient,
    private val yummyMappingDao: YummyMappingDao,
    private val malMappingDao: MalMappingDao,
    private val skipTimeDao: SkipTimeDao,
) : SkipTimesRepository {

    override suspend fun getSkipTimes(
        animeId: Int,
        episodeNumber: Int,
        titleOriginal: String,
        titleAlternative: String,
        year: Int?,
    ): List<SkipInterval> = withContext(Dispatchers.IO) {
        val cached = skipTimeDao.getBySource(
            animeId,
            episodeNumber,
            SkipTimeEntity.SOURCE_ALLOHA_ANISKIP,
        )
        if (cached.isNotEmpty()) {
            Timber.d(
                "Alloha/AniSkip cache hit: animeId=%d ep=%d (%d entries)",
                animeId,
                episodeNumber,
                cached.size,
            )
            return@withContext cached.toSkipIntervals()
        }

        val (ranges, aniSkipLookup) = supervisorScope {
            val allohaDeferred = async {
                loadAllohaRanges(
                    animeId = animeId,
                    episodeNumber = episodeNumber,
                    titleOriginal = titleOriginal,
                    titleAlternative = titleAlternative,
                )
            }
            val aniSkipDeferred = async {
                loadAniSkipLookup(
                    animeId = animeId,
                    episodeNumber = episodeNumber,
                    titleOriginal = titleOriginal,
                    titleAlternative = titleAlternative,
                    year = year,
                )
            }
            val loadedRanges = allohaDeferred.await()
            if (loadedRanges.isEmpty()) {
                aniSkipDeferred.cancel()
                return@supervisorScope emptyList<AllohaSkipRange>() to AniSkipLookup.EMPTY
            }
            loadedRanges to aniSkipDeferred.await()
        }
        if (ranges.isEmpty()) return@withContext emptyList()

        val intervals = SkipIntervalClassifier.classify(ranges, aniSkipLookup)
        if (intervals.isEmpty()) return@withContext emptyList()

        val entities = intervals.map { interval ->
            SkipTimeEntity(
                animeId = animeId,
                episode = episodeNumber,
                type = interval.type.name,
                startMs = interval.startMs,
                endMs = interval.endMs,
                source = SkipTimeEntity.SOURCE_ALLOHA_ANISKIP,
            )
        }
        skipTimeDao.insertAll(entities)
        Timber.d(
            "Alloha/AniSkip saved: animeId=%d ep=%d raw=%s hints=%s result=%s",
            animeId,
            episodeNumber,
            ranges.map { "${it.startMs}-${it.endMs}" },
            aniSkipLookup.hints.map { "${it.type}:${it.startMs}-${it.endMs}" },
            entities.map { "${it.type}:${it.startMs}-${it.endMs}" },
        )
        entities.toSkipIntervals()
    }

    private suspend fun loadAllohaRanges(
        animeId: Int,
        episodeNumber: Int,
        titleOriginal: String,
        titleAlternative: String,
    ): List<AllohaSkipRange> = try {
        val yummyId = resolveYummyId(animeId, titleOriginal, titleAlternative)
        if (yummyId == null) {
            Timber.w("Alloha: could not resolve yummyanime id for animeId=%d", animeId)
            emptyList()
        } else {
            skipClient.loadSkipIntervals(yummyId, episodeNumber)
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Timber.w(error, "Alloha skip request failed: animeId=%d ep=%d", animeId, episodeNumber)
        emptyList()
    }

    private suspend fun loadAniSkipLookup(
        animeId: Int,
        episodeNumber: Int,
        titleOriginal: String,
        titleAlternative: String,
        year: Int?,
    ): AniSkipLookup {
        val lookup = try {
            withTimeoutOrNull(ANI_SKIP_LOOKUP_TIMEOUT_MS) {
                val malId = resolveMalId(
                    animeId = animeId,
                    titleOriginal = titleOriginal,
                    titleAlternative = titleAlternative,
                    year = year,
                ) ?: return@withTimeoutOrNull AniSkipLookup.EMPTY
                aniSkipClient.loadSkipHints(malId, episodeNumber)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w(error, "AniSkip lookup failed: animeId=%d ep=%d", animeId, episodeNumber)
            AniSkipLookup.EMPTY
        }
        if (lookup == null) {
            Timber.w(
                "AniSkip lookup timed out after %d ms: animeId=%d ep=%d",
                ANI_SKIP_LOOKUP_TIMEOUT_MS,
                animeId,
                episodeNumber,
            )
            return AniSkipLookup.EMPTY
        }
        return lookup
    }

    private suspend fun resolveYummyId(
        animeId: Int,
        titleOriginal: String,
        titleAlternative: String,
    ): Int? {
        yummyMappingDao.get(animeId)?.let { return it.yummyId }
        val candidates = buildSearchCandidates(titleOriginal, titleAlternative)
        val resolved = searchClient.searchFirstId(candidates) ?: return null
        yummyMappingDao.insert(YummyMappingEntity(animeId = animeId, yummyId = resolved))
        return resolved
    }

    private suspend fun resolveMalId(
        animeId: Int,
        titleOriginal: String,
        titleAlternative: String,
        year: Int?,
    ): Int? {
        malMappingDao.get(animeId)?.let { return it.malId }
        val candidates = buildAniSkipSearchCandidates(titleOriginal, titleAlternative)
        val resolved = animeThemesClient.findMyAnimeListId(candidates, year) ?: return null
        malMappingDao.insert(MalMappingEntity(animeId = animeId, malId = resolved))
        return resolved
    }

    private fun buildSearchCandidates(
        titleOriginal: String,
        titleAlternative: String,
    ): List<String> {
        val out = LinkedHashSet<String>()
        // Russian title is the strongest match on yummyanime.tv.
        if (titleAlternative.isNotBlank()) out.add(titleAlternative.trim())
        if (titleOriginal.isNotBlank()) {
            val original = titleOriginal.trim()
            out.add(original)
            if (original.contains(":")) {
                out.add(original.substringBefore(":").trim())
            }
            original.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach(out::add)
        }
        return out.toList()
    }

    private fun buildAniSkipSearchCandidates(
        titleOriginal: String,
        titleAlternative: String,
    ): List<String> {
        val out = LinkedHashSet<String>()
        if (titleOriginal.isNotBlank()) {
            val original = titleOriginal.trim()
            out.add(original)
            original.split(",").map(String::trim).filter(String::isNotBlank).forEach(out::add)
            if (original.contains(":")) {
                out.add(original.substringBefore(":").trim())
            }
        }
        titleAlternative.trim().takeIf(String::isNotBlank)?.let(out::add)
        return out.toList()
    }

    private fun List<SkipTimeEntity>.toSkipIntervals() = mapNotNull { e ->
        val type = when (e.type) {
            "OP" -> SkipType.OP
            "ED" -> SkipType.ED
            else -> return@mapNotNull null
        }
        SkipInterval(type = type, startMs = e.startMs, endMs = e.endMs)
    }

    private companion object {
        const val ANI_SKIP_LOOKUP_TIMEOUT_MS = 8_000L
    }
}
