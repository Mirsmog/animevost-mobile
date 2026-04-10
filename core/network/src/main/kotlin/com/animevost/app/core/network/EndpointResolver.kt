package com.animevost.app.core.network

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class EndpointResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("resolver") private val httpClient: OkHttpClient,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val mutex = Mutex()

    @Volatile
    var currentBaseUrl: String = loadCachedUrl() ?: DleEndpoints.BASE_URL
        private set

    init {
        // Kick off background resolution immediately; first requests use the cached/primary URL.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { ensureResolved() }
    }

    /** Re-resolves only if the cached result is older than [CACHE_TTL_MS]. Safe to call anytime. */
    suspend fun ensureResolved() = mutex.withLock {
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_TIMESTAMP, 0L) < CACHE_TTL_MS) return

        val resolved = resolveActiveUrl()
        currentBaseUrl = resolved
        prefs.edit()
            .putString(KEY_URL, resolved)
            .putLong(KEY_TIMESTAMP, now)
            .apply()
        Timber.i("EndpointResolver: active base = $resolved")
    }

    private suspend fun resolveActiveUrl(): String = withContext(Dispatchers.IO) {
        if (isReachable(DleEndpoints.BASE_URL)) return@withContext DleEndpoints.BASE_URL

        Timber.w("Primary URL unreachable, fetching mirror.json")
        val mirrorUrl = fetchMirrorConfig()?.mirror ?: DleEndpoints.MIRROR_URL

        if (isReachable(mirrorUrl)) return@withContext mirrorUrl

        Timber.w("Mirror also unreachable, keeping fallback: $mirrorUrl")
        mirrorUrl
    }

    private fun isReachable(url: String): Boolean = try {
        val req = Request.Builder().url(url).head().build()
        httpClient.newCall(req).execute().use { it.isSuccessful }
    } catch (e: Exception) {
        Timber.d("Unreachable: $url — ${e.message}")
        false
    }

    private fun fetchMirrorConfig(): MirrorConfig? = try {
        val req = Request.Builder().url(DleEndpoints.MIRROR_JSON_URL).build()
        httpClient.newCall(req).execute().use { response ->
            if (!response.isSuccessful) return null
            val json = response.body?.string() ?: return null
            Gson().fromJson(json, MirrorConfig::class.java)
        }
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch mirror.json")
        null
    }

    private fun loadCachedUrl(): String? {
        if (System.currentTimeMillis() - prefs.getLong(KEY_TIMESTAMP, 0L) > CACHE_TTL_MS) return null
        return prefs.getString(KEY_URL, null)
    }

    data class MirrorConfig(
        val primary: String,
        val mirror: String,
        @SerializedName("updated_at") val updatedAt: String,
    )

    companion object {
        private const val PREFS_NAME = "endpoint_resolver"
        private const val KEY_URL = "active_url"
        private const val KEY_TIMESTAMP = "resolved_at"
        const val CACHE_TTL_MS = 30L * 60 * 1000 // 30 minutes
    }
}
