package com.animevost.app.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.animevost.app.core.network.CookieStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsCookieStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) : CookieStorage {

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

    override fun loadCookies(domain: String): List<Cookie> {
        val cookieSet = prefs.getStringSet(domain, emptySet()) ?: emptySet()
        val url = "https://$domain/".toHttpUrlOrNull() ?: return emptyList()
        return cookieSet.mapNotNull { Cookie.parse(url, it) }
    }

    override fun saveCookies(domain: String, cookies: List<Cookie>) {
        val cookieStrings = cookies.map { it.toString() }.toSet()
        prefs.edit().putStringSet(domain, cookieStrings).apply()
    }

    override fun clearCookies() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "cookie_storage"
    }
}
