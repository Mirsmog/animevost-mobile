package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.FavoriteEntry
import kotlinx.coroutines.flow.Flow

/** Manages the user's personal favourites list backed by the local Room database. */
interface FavoriteRepository {
    /** Returns a reactive stream of all favourited anime. */
    fun getAllFavorites(): Flow<List<AnimePreview>>

    /** Returns a reactive stream of all favourite entries with local metadata when available. */
    fun getAllFavoriteEntries(): Flow<List<FavoriteEntry>>

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
     * One-time merge performed on login: pushes local-only items to the remote server,
     * clears the local Room DB, and loads all remote favorites into in-memory state.
     */
    suspend fun syncOnLogin(): com.animevost.app.core.domain.util.Result<Unit>

    /**
     * Fetches all pages of remote favorites and updates the in-memory state.
     * No-op when not logged in.
     */
    suspend fun loadRemoteFavorites(): com.animevost.app.core.domain.util.Result<Unit>

    /** Clears local favorites and their account ownership on explicit logout. */
    suspend fun clearLocal()
}
