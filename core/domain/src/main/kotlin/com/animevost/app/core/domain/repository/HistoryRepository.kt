package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

/** Persists and retrieves the user's watch history backed by the local Room database. */
interface HistoryRepository {
    /** Returns all previously watched anime, most-recent first. */
    suspend fun getHistory(): List<AnimePreview>

    /** Returns a reactive stream of deduplicated history entries, most-recent first. */
    fun getHistoryFlow(): Flow<List<HistoryEntry>>

    /** Records [episode] of [anime] as watched. */
    suspend fun addToHistory(anime: AnimePreview, episode: Episode)

    /** Removes all history entries. */
    suspend fun clearHistory()
}
