package com.animevost.app.core.data.repository

import com.animevost.app.core.data.db.WatchProgressDao
import com.animevost.app.core.data.db.WatchProgressEntity
import com.animevost.app.core.domain.model.WatchProgress
import com.animevost.app.core.domain.repository.WatchProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WatchProgressRepositoryImpl @Inject constructor(
    private val dao: WatchProgressDao,
) : WatchProgressRepository {

    override fun observeLatestPerAnime(): Flow<List<WatchProgress>> =
        dao.observeLatestPerAnime().map { list -> list.map { it.toDomain() } }

    override suspend fun saveProgress(progress: WatchProgress) {
        dao.upsert(progress.toEntity())
    }

    override suspend fun getProgress(animeId: Int, episodeVideoId: String): WatchProgress? =
        dao.get(animeId, episodeVideoId)?.toDomain()

    override suspend fun getAllProgress(animeId: Int): List<WatchProgress> =
        dao.getAllForAnime(animeId).map { it.toDomain() }

    override suspend fun getLastProgress(animeId: Int): WatchProgress? =
        dao.getLastForAnime(animeId)?.toDomain()

    override suspend fun clearProgress(animeId: Int) {
        dao.clearForAnime(animeId)
    }

    private fun WatchProgress.toEntity() = WatchProgressEntity(
        animeId = animeId,
        episodeVideoId = episodeVideoId,
        episodeName = episodeName,
        episodeIndex = episodeIndex,
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAt = updatedAt,
    )

    private fun WatchProgressEntity.toDomain() = WatchProgress(
        animeId = animeId,
        episodeVideoId = episodeVideoId,
        episodeName = episodeName,
        episodeIndex = episodeIndex,
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAt = updatedAt,
    )
}
