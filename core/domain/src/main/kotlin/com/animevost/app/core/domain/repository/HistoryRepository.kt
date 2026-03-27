package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.Episode

interface HistoryRepository {
    suspend fun getHistory(): List<AnimePreview>
    suspend fun addToHistory(anime: AnimePreview, episode: Episode)
    suspend fun clearHistory()
}
