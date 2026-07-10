@file:Suppress("DEPRECATION")

package com.animevost.app.core.data.sdk

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.animevost.sdk.http.AnimeVostCookieStore
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedAnimeVostCookieStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : AnimeVostCookieStore {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val lock = Any()

    override fun save(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val existing = loadAll().toMutableList()
            cookies.forEach { cookie ->
                existing.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
                if (cookie.expiresAt > now && cookie.value != "deleted") {
                    existing += cookie
                }
            }
            existing.removeAll { it.expiresAt <= now || it.value == "deleted" }
            saveAll(existing)
        }
    }

    override fun load(url: HttpUrl): List<Cookie> =
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val active = loadAll().filter { it.expiresAt > now && it.value != "deleted" }
            saveAll(active)
            active.filter { it.matches(url) }
        }

    override fun get(name: String): String? =
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val active = loadAll().filter { it.expiresAt > now && it.value != "deleted" }
            saveAll(active)
            active.asSequence()
                .firstOrNull { it.name == name && it.expiresAt > now && it.value != "deleted" }
                ?.value
        }

    override fun clear() {
        synchronized(lock) {
            prefs.edit().clear().apply()
        }
    }

    private fun loadAll(): List<Cookie> = prefs.all.entries
        .asSequence()
        .filter { it.value is Set<*> }
        .flatMap { (host, value) ->
            val url = "https://$host/".toHttpUrlOrNull() ?: return@flatMap emptySequence()
            @Suppress("UNCHECKED_CAST")
            (value as Set<String>).asSequence().mapNotNull { Cookie.parse(url, it) }
        }
        .distinctBy { Triple(it.name, it.domain, it.path) }
        .toList()

    private fun saveAll(cookies: List<Cookie>) {
        val editor = prefs.edit().clear()
        cookies.groupBy { cookie -> cookie.domain }.forEach { (domain, values) ->
            editor.putStringSet(domain, values.map { cookie -> cookie.toString() }.toSet())
        }
        editor.apply()
    }

    private companion object {
        const val PREFS_NAME = "cookie_storage"
    }
}
