package com.animevost.app.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HtmlFetcher @Inject constructor(
    private val client: OkHttpClient,
) {

    suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw HtmlFetchException("HTTP ${response.code}: ${response.message}")
            }
            response.body?.string() ?: throw HtmlFetchException("Empty response body")
        }
    }

    suspend fun fetchPost(
        url: String,
        params: Map<String, String>,
    ): String = withContext(Dispatchers.IO) {
        val formBody = FormBody.Builder().apply {
            params.forEach { (key, value) -> add(key, value) }
        }.build()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .post(formBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw HtmlFetchException("HTTP ${response.code}: ${response.message}")
            }
            response.body?.string() ?: throw HtmlFetchException("Empty response body")
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }
}

class HtmlFetchException(message: String) : Exception(message)
