package com.animevost.app.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.animevost.app.core.network.CookieStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsCookieStorage @Inject constructor(
    @ApplicationContext context: Context,
) : CookieStorage {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cookie_storage", Context.MODE_PRIVATE)

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
}
