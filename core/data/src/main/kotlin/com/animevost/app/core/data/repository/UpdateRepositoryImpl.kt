package com.animevost.app.core.data.repository

import android.content.Context
import android.os.Build
import com.animevost.app.core.domain.model.UpdateInfo
import com.animevost.app.core.domain.repository.UpdateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class UpdateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : UpdateRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun checkForUpdate(currentVersionName: String): UpdateInfo? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/repos/Mirsmog/animevost-mobile/releases/latest")
                    .header("Accept", "application/vnd.github+json")
                    .build()

                val body = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    response.body?.string() ?: return@withContext null
                }

                val json = JSONObject(body)
                val tagName = json.getString("tag_name").trimStart('v')

                if (!isNewer(tagName, currentVersionName)) return@withContext null

                val assets = json.getJSONArray("assets")
                val preferredAbi = preferredAbi()
                var downloadUrl: String? = null

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    if (name.contains(preferredAbi, ignoreCase = true)) {
                        downloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }
                // Fall back to "universal" if the device ABI-specific asset is not found
                if (downloadUrl == null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.getString("name").contains("universal", ignoreCase = true)) {
                            downloadUrl = asset.getString("browser_download_url")
                            break
                        }
                    }
                }

                downloadUrl?.let { UpdateInfo(versionName = tagName, downloadUrl = it) }
            } catch (e: Exception) {
                Timber.w(e, "Update check failed")
                null
            }
        }

    override suspend fun downloadUpdate(url: String, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body ?: error("Empty response body for APK download")
            val contentLength = body.contentLength()
            val outFile = File(context.cacheDir, "animevost-update.apk")

            body.byteStream().use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesRead = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read
                        if (contentLength > 0) {
                            onProgress((bytesRead * 100 / contentLength).toInt())
                        }
                    }
                }
            }
            outFile
        }

    /** Returns true when [remote] version string is strictly greater than [current]. */
    private fun isNewer(remote: String, current: String): Boolean {
        val r = remote.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        val len = maxOf(r.size, c.size)
        for (i in 0 until len) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv > cv) return true
            if (rv < cv) return false
        }
        return false
    }

    private fun preferredAbi(): String {
        val supported = Build.SUPPORTED_ABIS.toSet()
        return when {
            "arm64-v8a" in supported -> "arm64-v8a"
            "armeabi-v7a" in supported -> "armeabi-v7a"
            "x86_64" in supported -> "x86_64"
            "x86" in supported -> "x86"
            else -> "universal"
        }
    }
}
