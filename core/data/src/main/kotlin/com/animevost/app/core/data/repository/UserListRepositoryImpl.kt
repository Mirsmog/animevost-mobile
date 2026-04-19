package com.animevost.app.core.data.repository

import android.util.Base64
import com.animevost.app.core.data.db.UserListDao
import com.animevost.app.core.data.db.UserListEntity
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeStatus
import com.animevost.app.core.domain.model.UserListEntry
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.domain.repository.UserListRepository
import com.animevost.app.core.network.EndpointResolver
import com.animevost.app.core.network.HtmlFetcher
import com.animevost.app.core.network.parser.UserInfoParser
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
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
 * (`pendingUpload == true`), it uploads them first (local wins on conflict) before accepting
 * the server state as authoritative.
 */
@Singleton
class UserListRepositoryImpl @Inject constructor(
    private val dao: UserListDao,
    private val htmlFetcher: HtmlFetcher,
    private val userInfoParser: UserInfoParser,
    private val authRepository: AuthRepository,
    private val endpointResolver: EndpointResolver,
) : UserListRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    /**
     * True when Room has changes that have not yet been confirmed written to the server.
     * Guards against startup sync overwriting unsent local changes.
     */
    @Volatile private var pendingUpload = false

    init {
        scope.launch {
            authRepository.isLoggedInFlow.collect { loggedIn ->
                if (loggedIn) {
                    try { syncFromRemote() } catch (e: Exception) {
                        Timber.w(e, "UserList initial sync failed")
                    }
                } else {
                    mutex.withLock {
                        dao.deleteAll()
                        pendingUpload = false
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
        pendingUpload = true
        scope.launch {
            try {
                flushToRemote()
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
        val profileUrl = profileUrlFor(user.name)

        if (pendingUpload) {
            uploadNow(profileUrl)
            val html = htmlFetcher.fetch(profileUrl)
            val parsed = userInfoParser.parse(html, profileUrl)
            val remote = decodePayload(parsed.rawInfo)
            addMissingFromRemote(remote)
            if (parsed.avatarUrl.isNotBlank()) authRepository.saveAvatarUrl(parsed.avatarUrl)
        } else {
            val html = htmlFetcher.fetch(profileUrl)
            val parsed = userInfoParser.parse(html, profileUrl)
            val remote = decodePayload(parsed.rawInfo)
            overwriteLocalWithRemote(remote)
            if (parsed.avatarUrl.isNotBlank()) authRepository.saveAvatarUrl(parsed.avatarUrl)
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Reads fresh profile form data, then writes local Room state back to the server.
     * Always re-fetches form fields (hash, fullname, land, email) to avoid posting stale data.
     * Must be called within [mutex].
     */
    private suspend fun uploadNow(profileUrl: String) {
        val html = htmlFetcher.fetch(profileUrl)
        val parsed = userInfoParser.parse(html, profileUrl)
        if (parsed.hash.isBlank()) {
            Timber.w("UserList upload skipped — could not read dle_allow_hash (not logged in?)")
            return
        }
        val entries = dao.getAll()
        val b64 = encodePayload(entries).toBase64()
        htmlFetcher.fetchMultipart(
            url = profileUrl,
            parts = buildOf(
                "doaction" to "adduserinfo",
                "id" to parsed.userId,
                "dle_allow_hash" to parsed.hash,
                "fullname" to parsed.fullname,
                "land" to parsed.land,
                "email" to parsed.email,
                "info" to b64,
            ),
        )
        pendingUpload = false
        Timber.d("UserList uploaded ${entries.size} entries")
    }

    /** Background flush — acquires mutex and calls [uploadNow]. */
    private suspend fun flushToRemote() = mutex.withLock {
        val user = authRepository.getCurrentUser() ?: return@withLock
        uploadNow(profileUrlFor(user.name))
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

    private fun profileUrlFor(username: String): String =
        endpointResolver.currentBaseUrl + "user/$username/"

    // ── Serialization ─────────────────────────────────────────────────────────

    private fun decodePayload(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return try {
            val json = String(Base64.decode(raw, Base64.NO_WRAP), Charsets.UTF_8)
            val obj = JsonParser.parseString(json).asJsonObject
            if (obj.get("v")?.asInt != PAYLOAD_VERSION) return emptyMap()
            val s = obj.getAsJsonObject("s") ?: return emptyMap()
            s.entrySet().mapNotNull { (url, el) ->
                val code = runCatching { el.asString }.getOrNull() ?: return@mapNotNull null
                if (AnimeStatus.fromCode(code) != null) url to code else null
            }.toMap()
        } catch (e: Exception) {
            Timber.d("UserList payload decode skipped (may be plain-text info field): ${e.message}")
            emptyMap()
        }
    }

    private fun encodePayload(entries: List<UserListEntity>): String {
        val s = JsonObject()
        entries.forEach { s.addProperty(it.animeUrl, it.status) }
        return JsonObject().apply {
            addProperty("v", PAYLOAD_VERSION)
            add("s", s)
        }.toString()
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
        const val PAYLOAD_VERSION = 1

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

        fun String.toBase64(): String =
            Base64.encodeToString(toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        fun buildOf(vararg pairs: Pair<String, String>): Map<String, String> = mapOf(*pairs)
    }
}
