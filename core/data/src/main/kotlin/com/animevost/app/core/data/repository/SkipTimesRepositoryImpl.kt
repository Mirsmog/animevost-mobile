package com.animevost.app.core.data.repository

import com.animevost.app.core.data.db.MalMappingDao
import com.animevost.app.core.data.db.MalMappingEntity
import com.animevost.app.core.data.db.SkipTimeDao
import com.animevost.app.core.data.db.SkipTimeEntity
import com.animevost.app.core.domain.model.SkipInterval
import com.animevost.app.core.domain.model.SkipType
import com.animevost.app.core.domain.repository.SkipTimesRepository
import com.animevost.app.core.network.AniSkipApi
import com.animevost.app.core.network.JikanApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SkipTimesRepositoryImpl @Inject constructor(
    private val jikanApi: JikanApi,
    private val aniSkipApi: AniSkipApi,
    private val malMappingDao: MalMappingDao,
    private val skipTimeDao: SkipTimeDao,
) : SkipTimesRepository {

    override suspend fun getSkipTimes(
        animeId: Int,
        episodeNumber: Int,
        titleOriginal: String,
        titleAlternative: String,
    ): List<SkipInterval> = withContext(Dispatchers.IO) {
        val cached = skipTimeDao.get(animeId, episodeNumber)
        if (cached.isNotEmpty()) return@withContext cached.toSkipIntervals()

        val malId = getMalId(animeId, titleOriginal, titleAlternative)
            ?: return@withContext emptyList()

        return@withContext try {
            val response = aniSkipApi.getSkipTimes(malId, episodeNumber)
            if (!response.found || response.results.isEmpty()) return@withContext emptyList()
            val entities = response.results.mapNotNull { result ->
                val type = when (result.skip_type) {
                    "op" -> "OP"
                    "ed" -> "ED"
                    else -> return@mapNotNull null
                }
                SkipTimeEntity(
                    animeId = animeId,
                    episode = episodeNumber,
                    type = type,
                    startMs = (result.interval.start_time * 1000).toLong(),
                    endMs = (result.interval.end_time * 1000).toLong(),
                )
            }
            if (entities.isNotEmpty()) skipTimeDao.insertAll(entities)
            entities.toSkipIntervals()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun getMalId(
        animeId: Int,
        titleOriginal: String,
        titleAlternative: String,
    ): Int? {
        malMappingDao.get(animeId)?.let { return it.malId }

        val candidates = buildSearchCandidates(titleOriginal, titleAlternative)
        for (query in candidates) {
            if (query.isBlank()) continue
            try {
                val resp = jikanApi.searchAnime(query)
                if (resp.data.isNotEmpty()) {
                    val malId = resp.data[0].mal_id
                    malMappingDao.insert(MalMappingEntity(animeId = animeId, malId = malId))
                    return malId
                }
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    private fun buildSearchCandidates(
        titleOriginal: String,
        titleAlternative: String,
    ): List<String> {
        val candidates = mutableListOf<String>()
        if (titleOriginal.isNotBlank()) {
            candidates.add(titleOriginal)
            if (titleOriginal.contains(":")) {
                candidates.add(titleOriginal.substringBefore(":").trim())
                candidates.add(titleOriginal.substringAfter(":").trim())
            }
            titleOriginal.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
                if (it !in candidates) candidates.add(it)
            }
        }
        if (titleAlternative.isNotBlank() && titleAlternative !in candidates) {
            candidates.add(titleAlternative)
        }
        return candidates
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
