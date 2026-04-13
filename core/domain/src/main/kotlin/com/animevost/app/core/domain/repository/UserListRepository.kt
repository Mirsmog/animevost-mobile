package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeStatus
import kotlinx.coroutines.flow.Flow

interface UserListRepository {
    /** Reactive status for a single anime URL (null = not in list). */
    fun getStatusFlow(animeUrl: String): Flow<AnimeStatus?>

    /**
     * Set or remove the watch status for an anime.
     * [preview] provides display data cached locally; pass null only to remove.
     * [status] = null removes the entry from the list.
     */
    suspend fun setStatus(animeUrl: String, status: AnimeStatus?, preview: AnimePreview?)

    /** Reactive list of previews with the given status, ordered by last update. */
    fun getByStatus(status: AnimeStatus): Flow<List<AnimePreview>>

    /** Sync watch-list from the remote profile `info` field. No-op when not logged in. */
    suspend fun syncFromRemote()
}
