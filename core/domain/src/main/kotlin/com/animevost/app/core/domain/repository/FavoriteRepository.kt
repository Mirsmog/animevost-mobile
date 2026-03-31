package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.AnimePreview
import kotlinx.coroutines.flow.Flow

/** Manages the user's personal favourites list backed by the local Room database. */
interface FavoriteRepository {
    /** Returns a reactive stream of all favourited anime. */
    fun getAllFavorites(): Flow<List<AnimePreview>>

    /** Returns a single page of favourites (for non-reactive use). */
    suspend fun getFavorites(page: Int): List<AnimePreview>

    /** Returns `true` if the anime identified by [newsId] is favourited. */
    suspend fun isFavorite(newsId: Int): Boolean

    /** Returns a reactive stream of the favourite status for [newsId]. */
    fun isFavoriteFlow(newsId: Int): Flow<Boolean>

    /**
     * Toggles the favourite status for [newsId].
     * @return the new favourite state (`true` = added, `false` = removed).
     */
    suspend fun toggleFavorite(newsId: Int, preview: AnimePreview? = null): Boolean

    /**
     * Synchronises local favourites with the remote server.
     * Pulls remote-only items into Room; pushes local-only IDs to the server.
     * Requires the user to be authenticated — returns [Result.Error] if not logged in.
     */
    suspend fun syncWithRemote(): com.animevost.app.core.domain.util.Result<Unit>
}
