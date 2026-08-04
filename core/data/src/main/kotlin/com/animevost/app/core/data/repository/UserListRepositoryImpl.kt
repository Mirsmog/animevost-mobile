package com.animevost.app.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.animevost.app.core.data.db.UserListDao
import com.animevost.app.core.data.db.UserListEntity
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeStatus
import com.animevost.app.core.domain.model.UserListEntry
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.domain.repository.UserListRepository
import com.animevost.sdk.AnimeVostClient
import com.animevost.sdk.model.UserProfile
import com.animevost.sdk.model.UserProfileUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the user's anime watch list using the animevost profile `info` field as remote storage.
 *
 * The remote payload stores sorted DLE news IDs as compact deltas plus one of five status codes.
 * The binary data is protected by CRC32 and Base64-encoded for safe storage in the profile
 * `info` field. Legacy JSON payloads containing full URLs are decoded and migrated automatically.
 *
 * **Write strategy**: Every [setStatus] call updates Room immediately (optimistic), marks
 * the state as dirty, and launches a background flush. The flush re-reads the profile page
 * before writing to prevent stale `fullname`/`land`/`email` from overwriting user profile edits.
 *
 * **Sync strategy**: [syncFromRemote] re-reads the server. If there are unsent local changes
 * (persisted pending upload marker), it uploads them first before accepting
 * the server state as authoritative.
 */
@Singleton
class UserListRepositoryImpl @Inject constructor(
    private val dao: UserListDao,
    private val authRepository: AuthRepository,
    private val client: AnimeVostClient,
    private val dataStore: DataStore<Preferences>,
) : UserListRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val hydrationInFlight = ConcurrentHashMap.newKeySet<Int>()

    init {
        scope.launch {
            authRepository.isLoggedInFlow.collectLatest { loggedIn ->
                if (loggedIn) {
                    try {
                        syncFromRemote()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (e: Exception) {
                        Timber.w(e, "UserList initial sync failed")
                    }
                } else {
                    mutex.withLock {
                        dao.deleteAll()
                        setPendingUpload(false)
                    }
                }
            }
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    override fun getStatusFlow(animeUrl: String): Flow<AnimeStatus?> {
        val newsId = extractAnimeNewsId(animeUrl)
        val statusFlow = if (newsId == null) {
            dao.getStatusFlow(normalizeAnimePath(animeUrl))
        } else {
            dao.getStatusByNewsIdFlow(newsId)
        }
        return statusFlow
            .map { code -> code?.let { AnimeStatus.fromCode(it) } }
    }

    override fun getByStatus(status: AnimeStatus): Flow<List<AnimePreview>> =
        dao.getByStatus(status.code).map { list -> list.map { it.toPreview() } }

    override fun getAllEntries(): Flow<List<UserListEntry>> =
        dao.getAllFlow().map { list ->
            list.mapNotNull { entity ->
                val status = AnimeStatus.fromCode(entity.status) ?: return@mapNotNull null
                UserListEntry(
                    anime = entity.toPreview(),
                    status = status,
                    updatedAt = entity.updatedAt,
                )
            }
        }

    // ── Write ─────────────────────────────────────────────────────────────────

    override suspend fun setStatus(animeUrl: String, status: AnimeStatus?, preview: AnimePreview?) {
        mutex.withLock {
            val relativeUrl = normalizeAnimePath(animeUrl)
            val newsId = preview?.id?.takeIf { it > 0 }
                ?: extractAnimeNewsId(relativeUrl)
            if (status == null) {
                if (newsId == null) {
                    dao.deleteByUrl(relativeUrl)
                } else {
                    dao.deleteByNewsId(newsId)
                }
            } else {
                val existing = newsId?.let { dao.getByNewsId(it) }
                val storedUrl = if (
                    relativeUrl.startsWith(NEWS_ID_RESOLVER_PREFIX) && existing != null
                ) {
                    existing.animeUrl
                } else {
                    relativeUrl
                }
                val entity = UserListEntity(
                    animeUrl = storedUrl,
                    animeId = newsId ?: 0,
                    title = preview?.title?.takeIf { it.isNotBlank() } ?: existing?.title.orEmpty(),
                    posterUrl = preview?.posterUrl?.takeIf { it.isNotBlank() }
                        ?: existing?.posterUrl.orEmpty(),
                    status = status.code,
                )
                if (newsId == null) {
                    dao.upsert(entity)
                } else {
                    dao.upsertByNewsId(entity)
                }
            }
            setPendingUpload(true)
        }
        scope.launch {
            try {
                flushToRemote()
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                Timber.w(e, "UserList remote upload failed: will retry on next sync")
            }
        }
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    /**
     * Synchronises the watch list with the server.
     *
     * - If there are pending local changes, they are flushed first so no data is lost.
     * - A successful flush is confirmed by reading back the exact compact payload.
     * - Without pending changes, the server is treated as authoritative.
     */
    override suspend fun syncFromRemote() = mutex.withLock {
        val user = authRepository.getCurrentUser() ?: return@withLock

        if (hasPendingUpload()) {
            val profile = uploadNow(user.name)
            profile.avatarUrl?.takeIf { it.isNotBlank() }?.let { authRepository.saveAvatarUrl(it) }
        } else {
            val profile = client.getProfile(user.name)
            val remote = UserListPayloadCodec.decode(profile.info.orEmpty())
            overwriteLocalWithRemote(remote.statuses)
            profile.avatarUrl?.takeIf { it.isNotBlank() }?.let { authRepository.saveAvatarUrl(it) }
            if (remote.requiresMigration) {
                setPendingUpload(true)
                uploadNow(user.name, profile)
            }
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Reads fresh profile form data when it was not already fetched by the current sync, then
     * writes local Room state back to the server without overwriting unrelated profile fields.
     * Must be called within [mutex].
     */
    private suspend fun uploadNow(
        username: String,
        currentProfile: UserProfile? = null,
    ): UserProfile {
        val profile = currentProfile ?: client.getProfile(username)
        if (!profile.canEdit) {
            throw UserListSyncException("Profile is not editable")
        }
        val entries = dao.getAll()
        val localStatuses = entries.toRemoteStatuses()
        val updatedInfo = UserListPayloadCodec.merge(
            profileInfo = profile.info.orEmpty(),
            statuses = localStatuses,
        )
        client.updateProfile(
            current = profile,
            update = UserProfileUpdate(info = updatedInfo),
        )

        val confirmedProfile = client.getProfile(username)
        val confirmed = UserListPayloadCodec.decode(confirmedProfile.info.orEmpty())
        if (
            confirmed.source != UserListPayloadCodec.Source.MARKER_V2 ||
            confirmed.statuses != localStatuses
        ) {
            throw UserListSyncException("Server did not persist the user-list payload")
        }
        setPendingUpload(false)
        Timber.d("UserList uploaded ${entries.size} entries")
        return confirmedProfile
    }

    /** Background flush acquires mutex and calls [uploadNow] only while local changes are pending. */
    private suspend fun flushToRemote() = mutex.withLock {
        if (!hasPendingUpload()) return@withLock
        val user = authRepository.getCurrentUser() ?: return@withLock
        uploadNow(user.name)
    }

    private suspend fun hasPendingUpload(): Boolean =
        dataStore.data.first()[KEY_PENDING_UPLOAD] ?: false

    private suspend fun setPendingUpload(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_PENDING_UPLOAD] = value
        }
    }

    /**
     * Remote is authoritative: remove local entries not on remote, update status for changed ones,
     * but preserve existing title/posterUrl display data.
     */
    private suspend fun overwriteLocalWithRemote(remote: Map<Int, String>) {
        val localEntries = dao.getAll()
        val local = localEntries.mapNotNull { entity ->
            entity.newsIdOrNull()?.let { it to entity }
        }.toMap()
        remote.forEach { (newsId, code) ->
            val existing = local[newsId]
            if (existing?.status != code) {
                dao.upsertByNewsId(
                    UserListEntity(
                        animeUrl = existing?.animeUrl ?: animeResolverPath(newsId),
                        animeId = newsId,
                        title = existing?.title ?: "",
                        posterUrl = existing?.posterUrl ?: "",
                        status = code,
                    ),
                )
            }
        }
        localEntries
            .filter { it.newsIdOrNull()?.let { newsId -> newsId !in remote } == true }
            .forEach { dao.deleteByUrl(it.animeUrl) }
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    override suspend fun enrichPreview(animeUrl: String, preview: AnimePreview) {
        val newsId = preview.id.takeIf { it > 0 } ?: extractAnimeNewsId(animeUrl)
        if (newsId == null) {
            dao.enrichIfBlank(
                url = normalizeAnimePath(animeUrl),
                title = preview.title,
                posterUrl = preview.posterUrl,
                animeId = 0,
            )
        } else {
            dao.enrichByNewsIdIfBlank(
                newsId = newsId,
                title = preview.title,
                posterUrl = preview.posterUrl,
            )
        }
    }

    override suspend fun hydratePreview(newsId: Int) {
        if (newsId <= 0 || !hydrationInFlight.add(newsId)) return
        try {
            val existing = dao.getByNewsId(newsId) ?: return
            if (existing.title.isNotBlank()) return

            val details = client.getAnimeDetails(animeResolverPath(newsId))
            if (details.title.isBlank()) return
            mutex.withLock {
                val current = dao.getByNewsId(newsId) ?: return@withLock
                if (current.title.isNotBlank()) return@withLock
                val resolvedUrl = normalizeAnimePath(details.url)
                    .takeIf { it.isNotBlank() }
                    ?: current.animeUrl
                dao.upsertByNewsId(
                    current.copy(
                        animeUrl = resolvedUrl,
                        animeId = newsId,
                        title = details.title,
                        posterUrl = details.posterUrl.orEmpty(),
                    ),
                )
            }
        } finally {
            hydrationInFlight.remove(newsId)
        }
    }

    private companion object {
        val KEY_PENDING_UPLOAD = booleanPreferencesKey("user_list_pending_upload")
        const val NEWS_ID_RESOLVER_PREFIX = "index.php?newsid="

        fun List<UserListEntity>.toRemoteStatuses(): Map<Int, String> {
            val result = linkedMapOf<Int, String>()
            forEach { entity ->
                val newsId = entity.newsIdOrNull()
                    ?: throw UserListSyncException(
                        "Cannot sync user-list entry without an AnimeVost newsId",
                    )
                val previous = result.put(newsId, entity.status)
                if (previous != null && previous != entity.status) {
                    throw UserListSyncException(
                        "Conflicting statuses found for AnimeVost newsId $newsId",
                    )
                }
            }
            return result
        }

        fun UserListEntity.newsIdOrNull(): Int? =
            animeId.takeIf { it > 0 } ?: extractAnimeNewsId(animeUrl)

        fun UserListEntity.toPreview() = AnimePreview(
            id = newsIdOrNull() ?: 0,
            title = title,
            titleOriginal = "",
            posterUrl = posterUrl,
            episodeInfo = "",
            url = animeUrl,
        )
    }
}

private class UserListSyncException(message: String) : IllegalStateException(message)
