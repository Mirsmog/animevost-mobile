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
    private val lock = Any()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val domain = url.host
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val existing = memoryCache.getOrPut(domain) { mutableListOf() }
            cookies.forEach { newCookie ->
                existing.removeAll { it.name == newCookie.name }
                if (newCookie.expiresAt >= now) {
                    existing.add(newCookie)
                }
            }
            existing.removeAll { it.expiresAt < now }
            storage.saveCookies(domain, existing.toList())
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val domain = url.host
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val cookies = memoryCache.getOrPut(domain) {
                storage.loadCookies(domain).toMutableList()
            }
            cookies.removeAll { it.expiresAt < now }
            return cookies.toList()
        }
    }

    /** Returns the value of cookie [name] for [domain], checking memory cache then persistent storage. Expired cookies are excluded. */
    fun getCookieValue(domain: String, name: String): String? = synchronized(lock) {
        val now = System.currentTimeMillis()
        memoryCache.getOrPut(domain) {
            storage.loadCookies(domain).toMutableList()
        }.find { it.name == name && it.expiresAt >= now }?.value
    }

    /**
     * Returns the value of cookie [name] from ANY animevost or mirror domain.
     * Needed because login may happen via a mirror (e.g. v13.vost.pw) and the
     * cookie is saved under that domain, not under "animevost.org".
     */
    fun getCookieValueAnyAnimevost(name: String): String? = synchronized(lock) {
        val now = System.currentTimeMillis()
        val domains = memoryCache.keys.filter { d ->
            d.contains("animevost") || d.contains("vost.pw")
        }.ifEmpty {
            // Nothing in memory — try loading primary domain from storage
            listOf("animevost.org")
        }
        for (domain in domains) {
            val cookies = memoryCache.getOrPut(domain) {
                storage.loadCookies(domain).toMutableList()
            }
            val value = cookies.find { it.name == name && it.expiresAt >= now }?.value
            if (value != null) return value
        }
        return null
    }

    fun clear() {
        memoryCache.clear()
        storage.clearCookies()
    }
}
