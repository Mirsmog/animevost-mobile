package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.WatchProgress

interface WatchProgressRepository {
    /** Upserts progress for an episode. */
    suspend fun saveProgress(progress: WatchProgress)

    /** Returns progress for a specific episode, or null if none recorded. */
    suspend fun getProgress(animeId: Int, episodeVideoId: String): WatchProgress?

    /** Returns all episode progress records for the given anime. */
    suspend fun getAllProgress(animeId: Int): List<WatchProgress>

    /** Returns the most recently updated progress entry for the anime, or null. */
    suspend fun getLastProgress(animeId: Int): WatchProgress?

    /** Deletes all progress entries for the anime. */
    suspend fun clearProgress(animeId: Int)
}
