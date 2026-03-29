package com.animevost.app.core.data.repository

import com.animevost.app.core.data.db.MalMappingDao
import com.animevost.app.core.data.db.MalMappingEntity
import com.animevost.app.core.data.db.SkipSegmentDao
import com.animevost.app.core.data.db.SkipSegmentEntity
import com.animevost.app.core.domain.model.SkipSegment
import com.animevost.app.core.domain.model.SkipSource
import com.animevost.app.core.domain.model.SkipType
import com.animevost.app.core.domain.repository.SkipRepository
import com.animevost.app.core.network.AniSkipApi
import com.animevost.app.core.network.JikanApi
import javax.inject.Inject
import javax.inject.Singleton

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
    ): List<SkipSegment> {
        // 1. Try cached local segments first (user-marked always take priority)
        val local = skipSegmentDao.getSegments(animeId, episodeName)
            .filter { it.source == "USER" }
        if (local.isNotEmpty()) return local.map { it.toDomain() }

        // 2. Try AniSkip via MAL ID
        val episodeNumber = parseEpisodeNumber(episodeName) ?: return emptyList()
        val malId = resolveMalId(animeId) ?: return emptyList()

        return try {
            val response = aniSkipApi.getSkipTimes(malId, episodeNumber)
            if (!response.found) return emptyList()

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
        } catch (_: Exception) {
            // Fallback to any cached AniSkip segments
            skipSegmentDao.getSegments(animeId, episodeName).map { it.toDomain() }
        }
    }

    override suspend fun saveSkipSegment(
        animeId: Int,
        episodeName: String,
        segment: SkipSegment,
    ) {
        skipSegmentDao.insertSegment(segment.toEntity(animeId, episodeName))
    }

    private suspend fun resolveMalId(animeId: Int): Int? {
        // Check cache
        malMappingDao.getMapping(animeId)?.let { return it.malId }
        return null // MAL ID must be resolved externally and cached
    }

    /**
     * Resolve MAL ID by searching Jikan with the anime title.
     * Called from ViewModel which has access to AnimeDetail with title info.
     */
    suspend fun resolveMalId(
        animeId: Int,
        titleOriginal: String,
        title: String,
        year: String?,
        type: String?,
    ): Int? {
        // Check cache first
        malMappingDao.getMapping(animeId)?.let { return it.malId }

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

    companion object {
        private val EPISODE_NUMBER_REGEX = Regex("""(\d+)""")

        fun parseEpisodeNumber(episodeName: String): Int? {
            return EPISODE_NUMBER_REGEX.find(episodeName)?.groupValues?.get(1)?.toIntOrNull()
        }
    }
}

private fun SkipSegmentEntity.toDomain() = SkipSegment(
    type = when (type) {
        "INTRO" -> SkipType.INTRO
        else -> SkipType.OUTRO
    },
    startMs = startMs,
    endMs = endMs,
    source = when (source) {
        "USER" -> SkipSource.USER
        else -> SkipSource.ANISKIP
    },
)

private fun SkipSegment.toEntity(animeId: Int, episodeName: String) = SkipSegmentEntity(
    animeId = animeId,
    episodeName = episodeName,
    type = type.name,
    startMs = startMs,
    endMs = endMs,
    source = source.name,
)
