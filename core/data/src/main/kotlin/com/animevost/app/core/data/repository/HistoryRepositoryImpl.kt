package com.animevost.app.core.data.repository

import com.animevost.app.core.data.db.HistoryDao
import com.animevost.app.core.data.mapper.toAnimePreview
import com.animevost.app.core.data.mapper.toHistoryEntity
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.repository.HistoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao,
) : HistoryRepository {

    override suspend fun getHistory(): List<AnimePreview> {
        // Deduplicate by animeId keeping the most-recent watch entry per anime
        return historyDao.getAllList()
            .distinctBy { it.animeId }
            .map { it.toAnimePreview() }
    }

    override suspend fun addToHistory(anime: AnimePreview, episode: Episode) {
        historyDao.deleteByEpisode(anime.id, episode.videoId)
        historyDao.insert(anime.toHistoryEntity(episode))
    }

    override suspend fun clearHistory() {
        historyDao.clearAll()
    }
}
