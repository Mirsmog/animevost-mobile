package com.animevost.app.core.network

import okhttp3.Cookie

interface CookieStorage {
    fun loadCookies(domain: String): List<Cookie>
    fun saveCookies(domain: String, cookies: List<Cookie>)
    fun clearCookies()
}
