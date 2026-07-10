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
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the user's anime watch list using the animevost profile `info` field as remote storage.
 *
 * **Remote format** (stored Base64-encoded in the `info` textarea):
 * ```json
 * {"v":1,"s":{"tip/tv/3774-slug.html":"w","tip/ona/5001-slug.html":"p"}}
 * ```
 * Base64 is required because DLE BBCode-processes the field and mangles raw JSON brackets.
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

    override fun getStatusFlow(animeUrl: String): Flow<AnimeStatus?> =
        dao.getStatusFlow(animeUrl.toRelativePath())
            .map { code -> code?.let { AnimeStatus.fromCode(it) } }

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
        val relUrl = animeUrl.toRelativePath()
        if (status == null) {
            dao.deleteByUrl(relUrl)
        } else {
            dao.upsert(
                UserListEntity(
                    animeUrl = relUrl,
                    animeId = preview?.id ?: extractIdFromUrl(relUrl),
                    title = preview?.title ?: "",
                    posterUrl = preview?.posterUrl ?: "",
                    status = status.code,
                ),
            )
        }
        setPendingUpload(true)
        scope.launch {
            try {
                flushToRemote()
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                Timber.w(e, "UserList remote upload failed — will retry on next sync")
            }
        }
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    /**
     * Synchronises the watch list with the server.
     *
     * - If there are pending local changes, they are flushed first so no data is lost.
     * - After a successful flush the server state is read back and any entries that only
     *   exist remotely (e.g. from another device) are merged into the local DB.
     * - Without pending changes, the server is treated as authoritative.
     */
    override suspend fun syncFromRemote() = mutex.withLock {
        val user = authRepository.getCurrentUser() ?: return@withLock

        if (hasPendingUpload()) {
            uploadNow(user.name)
            val profile = client.getProfile(user.name)
            val remote = UserListPayloadCodec.decode(profile.info.orEmpty())
            addMissingFromRemote(remote)
            profile.avatarUrl?.takeIf { it.isNotBlank() }?.let { authRepository.saveAvatarUrl(it) }
        } else {
            val profile = client.getProfile(user.name)
            val remote = UserListPayloadCodec.decode(profile.info.orEmpty())
            overwriteLocalWithRemote(remote)
            profile.avatarUrl?.takeIf { it.isNotBlank() }?.let { authRepository.saveAvatarUrl(it) }
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Reads fresh profile form data, then writes local Room state back to the server.
     * Always re-fetches form fields (hash, fullname, land, email) to avoid posting stale data.
     * Must be called within [mutex].
     */
    private suspend fun uploadNow(username: String) {
        val profile = client.getProfile(username)
        if (!profile.canEdit) {
            Timber.w("UserList upload skipped: could not read dle_allow_hash (not logged in?)")
            return
        }
        val entries = dao.getAll()
        val updatedInfo = UserListPayloadCodec.merge(
            profileInfo = profile.info.orEmpty(),
            statuses = entries.associate { it.animeUrl to it.status },
        )
        client.updateProfile(
            username = username,
            update = UserProfileUpdate(info = updatedInfo),
        )
        setPendingUpload(false)
        Timber.d("UserList uploaded ${entries.size} entries")
    }

    /** Background flush — acquires mutex and calls [uploadNow]. */
    private suspend fun flushToRemote() = mutex.withLock {
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
    private suspend fun overwriteLocalWithRemote(remote: Map<String, String>) {
        val local = dao.getAll().associateBy { it.animeUrl }
        remote.forEach { (relUrl, code) ->
            val existing = local[relUrl]
            if (existing?.status != code) {
                dao.upsert(
                    UserListEntity(
                        animeUrl = relUrl,
                        animeId = existing?.animeId ?: extractIdFromUrl(relUrl),
                        title = existing?.title ?: "",
                        posterUrl = existing?.posterUrl ?: "",
                        status = code,
                    ),
                )
            }
        }
        local.keys.filter { it !in remote }.forEach { dao.deleteByUrl(it) }
    }

    /**
     * After a flush, add remote entries that don't exist locally yet (from another device).
     * Existing local entries are not touched (they were just uploaded and are correct).
     */
    private suspend fun addMissingFromRemote(remote: Map<String, String>) {
        val localUrls = dao.getAll().map { it.animeUrl }.toSet()
        remote.forEach { (relUrl, code) ->
            if (relUrl !in localUrls) {
                dao.upsert(
                    UserListEntity(
                        animeUrl = relUrl,
                        animeId = extractIdFromUrl(relUrl),
                        title = "",
                        posterUrl = "",
                        status = code,
                    ),
                )
            }
        }
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    override suspend fun enrichPreview(animeUrl: String, preview: AnimePreview) {
        dao.enrichIfBlank(
            url = animeUrl.toRelativePath(),
            title = preview.title,
            posterUrl = preview.posterUrl,
            animeId = preview.id,
        )
    }

    private companion object {
        val KEY_PENDING_UPLOAD = booleanPreferencesKey("user_list_pending_upload")

        private val ID_IN_URL = Regex("""/(\d+)-[^/]+\.html""")

        /** Strips scheme + host, returning a path like "tip/tv/3774-slug.html". */
        fun String.toRelativePath(): String = try {
            URI(this).path.trimStart('/')
        } catch (_: Exception) {
            trimStart('/')
        }

        fun extractIdFromUrl(relUrl: String): Int =
            ID_IN_URL.find("/$relUrl")?.groupValues?.get(1)?.toIntOrNull() ?: 0

        fun UserListEntity.toPreview() = AnimePreview(
            id = animeId,
            title = title,
            titleOriginal = "",
            posterUrl = posterUrl,
            episodeInfo = "",
            url = animeUrl,
        )
    }
}
