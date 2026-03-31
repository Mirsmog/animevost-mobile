package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.Episode

/** Persists and retrieves the user's watch history backed by the local Room database. */
interface HistoryRepository {
    /** Returns all previously watched anime, most-recent first. */
    suspend fun getHistory(): List<AnimePreview>

    /** Records [episode] of [anime] as watched. */
    suspend fun addToHistory(anime: AnimePreview, episode: Episode)

    /** Removes all history entries. */
    suspend fun clearHistory()
}
