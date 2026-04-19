package com.animevost.app.core.data.repository

import com.animevost.app.core.data.db.HistoryDao
import com.animevost.app.core.data.mapper.toAnimePreview
import com.animevost.app.core.data.mapper.toHistoryEntity
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.model.HistoryEntry
import com.animevost.app.core.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao,
) : HistoryRepository {

    override suspend fun getHistory(): List<AnimePreview> {
        return historyDao.getAllList().deduplicateHistory().map { it.anime }
    }

    override fun getHistoryFlow(): Flow<List<HistoryEntry>> =
        historyDao.getAll().map { list -> list.deduplicateHistory() }

    override suspend fun addToHistory(anime: AnimePreview, episode: Episode) {
        historyDao.deleteByEpisode(anime.id, episode.videoId)
        historyDao.insert(anime.toHistoryEntity(episode))
    }

    override suspend fun clearHistory() {
        historyDao.clearAll()
    }

    private fun List<com.animevost.app.core.data.db.HistoryEntity>.deduplicateHistory(): List<HistoryEntry> =
        distinctBy { it.animeId }.map {
            HistoryEntry(
                anime = it.toAnimePreview(),
                watchedAt = it.watchedAt,
            )
        }
}
