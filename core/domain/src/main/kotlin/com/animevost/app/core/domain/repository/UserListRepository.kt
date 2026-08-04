package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeStatus
import com.animevost.app.core.domain.model.UserListEntry
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

    /** Reactive list of all locally cached watch-list entries, ordered by last update. */
    fun getAllEntries(): Flow<List<UserListEntry>>

    /** Sync watch-list from the remote profile `info` field. No-op when not logged in. */
    suspend fun syncFromRemote()

    /**
     * Backfill display metadata (title, posterUrl) for an entry that was synced from remote
     * without local preview data. Only updates Room; does NOT trigger a remote upload.
     */
    suspend fun enrichPreview(animeUrl: String, preview: AnimePreview)

    /** Resolve and cache display metadata for a compact remote entry when it becomes visible. */
    suspend fun hydratePreview(newsId: Int)
}
