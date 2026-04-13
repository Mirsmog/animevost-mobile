package com.animevost.app.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HtmlFetcher @Inject constructor(
    private val client: OkHttpClient,
) {

    suspend fun fetch(url: String): String = withRetry {
        withContext(Dispatchers.IO) {
            Timber.d("Fetching: $url")
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
    }

    suspend fun fetchPost(
        url: String,
        params: Map<String, String>,
    ): String = withRetry {
        withContext(Dispatchers.IO) {
            Timber.d("Fetching (POST): $url")
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
    }

    /** POST with multipart/form-data encoding (required for DLE profile updates). */
    suspend fun fetchMultipart(
        url: String,
        parts: Map<String, String>,
    ): String = withRetry {
        withContext(Dispatchers.IO) {
            Timber.d("Fetching (multipart POST): $url")
            val body = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
                parts.forEach { (key, value) -> addFormDataPart(key, value) }
            }.build()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw HtmlFetchException("HTTP ${response.code}: ${response.message}")
                }
                response.body?.string() ?: throw HtmlFetchException("Empty response body")
            }
        }
    }

    private suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        delayMs: Long = 100L,
        block: suspend () -> T,
    ): T {
        var lastException: Exception? = null
        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: IOException) {
                lastException = e
                Timber.w(e, "Attempt ${attempt + 1} failed, retrying in ${delayMs * (attempt + 1)}ms")
                delay(delayMs * (attempt + 1))
            }
        }
        throw lastException!!
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }
}

class HtmlFetchException(message: String) : Exception(message)
