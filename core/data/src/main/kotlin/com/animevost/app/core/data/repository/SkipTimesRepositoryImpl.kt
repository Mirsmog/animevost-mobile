package com.animevost.app.core.data.repository

import com.animevost.app.core.data.db.SkipTimeDao
import com.animevost.app.core.data.db.SkipTimeEntity
import com.animevost.app.core.data.db.YummyMappingDao
import com.animevost.app.core.data.db.YummyMappingEntity
import com.animevost.app.core.domain.model.SkipInterval
import com.animevost.app.core.domain.model.SkipType
import com.animevost.app.core.domain.repository.SkipTimesRepository
import com.animevost.app.core.network.alloha.AllohaSkipClient
import com.animevost.app.core.network.alloha.AllohaSkipType
import com.animevost.app.core.network.alloha.YummyAnimeSearchClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Resolves skip intervals via yummyanime.tv's Alloha JSON player.
 *
 * Pipeline:
 *  1. Hit the local Room cache (`skip_times`).
 *  2. Resolve animeVost id → yummyanime id (cached in `yummy_mapping`).
 *  3. Run [AllohaSkipClient] which loads the iframe, builds the Borth header
 *     and calls `POST /bnsi/movies/{idFile}` to get the raw `skipTime` string.
 *  4. Persist resolved intervals back to the cache.
 */
class SkipTimesRepositoryImpl @Inject constructor(
    private val searchClient: YummyAnimeSearchClient,
    private val skipClient: AllohaSkipClient,
    private val yummyMappingDao: YummyMappingDao,
    private val skipTimeDao: SkipTimeDao,
) : SkipTimesRepository {

    override suspend fun getSkipTimes(
        animeId: Int,
        episodeNumber: Int,
        titleOriginal: String,
        titleAlternative: String,
    ): List<SkipInterval> = withContext(Dispatchers.IO) {
        val cached = skipTimeDao.get(animeId, episodeNumber)
        if (cached.isNotEmpty()) {
            Timber.d("Alloha cache hit: animeId=%d ep=%d (%d entries)", animeId, episodeNumber, cached.size)
            return@withContext cached.toSkipIntervals()
        }

        val yummyId = resolveYummyId(animeId, titleOriginal, titleAlternative)
        if (yummyId == null) {
            Timber.w("Alloha: could not resolve yummyanime id for animeId=%d", animeId)
            return@withContext emptyList()
        }

        val ranges = try {
            skipClient.loadSkipIntervals(yummyId, episodeNumber)
        } catch (error: CancellationException) {
            throw error
        } catch (e: Exception) {
            Timber.w(e, "Alloha skip request failed (yummyId=%d ep=%d)", yummyId, episodeNumber)
            emptyList()
        }
        if (ranges.isEmpty()) return@withContext emptyList()

        val entities = ranges.map { range ->
            SkipTimeEntity(
                animeId = animeId,
                episode = episodeNumber,
                type = when (range.type) {
                    AllohaSkipType.OPENING -> "OP"
                    AllohaSkipType.ENDING -> "ED"
                },
                startMs = range.startMs,
                endMs = range.endMs,
            )
        }
        // Multiple ranges of the same type collapse via PK → keep the latest.
        skipTimeDao.insertAll(entities)
        Timber.d("Alloha saved %d skip intervals for animeId=%d ep=%d", entities.size, animeId, episodeNumber)
        entities.toSkipIntervals()
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

    private fun List<SkipTimeEntity>.toSkipIntervals() = mapNotNull { e ->
        val type = when (e.type) {
            "OP" -> SkipType.OP
            "ED" -> SkipType.ED
            else -> return@mapNotNull null
        }
        SkipInterval(type = type, startMs = e.startMs, endMs = e.endMs)
    }
}
