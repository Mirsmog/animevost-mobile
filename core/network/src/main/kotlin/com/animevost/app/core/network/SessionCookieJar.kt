package com.animevost.app.core.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionCookieJar @Inject constructor(
    private val storage: CookieStorage,
) : CookieJar {

    private val memoryCache = mutableMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val domain = url.host
        val existing = memoryCache.getOrPut(domain) { mutableListOf() }
        cookies.forEach { newCookie ->
            existing.removeAll { it.name == newCookie.name }
            existing.add(newCookie)
        }
        storage.saveCookies(domain, existing)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val domain = url.host
        return memoryCache.getOrPut(domain) {
            storage.loadCookies(domain).toMutableList()
        }
    }

    fun clear() {
        memoryCache.clear()
        storage.clearCookies()
    }
}
