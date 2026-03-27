package com.animevost.app.core.data.repository

import com.animevost.app.core.data.db.HistoryDao
import com.animevost.app.core.data.db.HistoryEntity
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
        return historyDao.getAllList().map { entity ->
            AnimePreview(
                id = entity.animeId,
                title = entity.title,
                titleOriginal = entity.titleOriginal,
                posterUrl = entity.posterUrl,
                episodeInfo = entity.episodeInfo,
                url = entity.url,
            )
        }
    }

    override suspend fun addToHistory(anime: AnimePreview, episode: Episode) {
        historyDao.deleteByEpisode(anime.id, episode.videoId)
        historyDao.insert(
            HistoryEntity(
                animeId = anime.id,
                title = anime.title,
                titleOriginal = anime.titleOriginal,
                posterUrl = anime.posterUrl,
                episodeInfo = anime.episodeInfo,
                url = anime.url,
                episodeName = episode.name,
                episodeVideoId = episode.videoId,
            ),
        )
    }

    override suspend fun clearHistory() {
        historyDao.clearAll()
    }
}
