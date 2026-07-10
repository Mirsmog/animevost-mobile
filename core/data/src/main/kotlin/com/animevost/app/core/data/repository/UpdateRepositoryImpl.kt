package com.animevost.app.core.data.repository

import android.content.Context
import android.os.Build
import com.animevost.app.core.domain.model.UpdateInfo
import com.animevost.app.core.domain.repository.UpdateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.IOException
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
                    if (
                        name.endsWith(".apk", ignoreCase = true) &&
                        name.contains(preferredAbi, ignoreCase = true)
                    ) {
                        downloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }
                // Fall back to "universal" if the device ABI-specific asset is not found
                if (downloadUrl == null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.getString("name")
                        if (
                            name.endsWith(".apk", ignoreCase = true) &&
                            name.contains("universal", ignoreCase = true)
                        ) {
                            downloadUrl = asset.getString("browser_download_url")
                            break
                        }
                    }
                }

                downloadUrl?.let { UpdateInfo(versionName = tagName, downloadUrl = it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Update check failed")
                null
            }
        }

    override suspend fun downloadUpdate(url: String, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            val outFile = File(context.cacheDir, "animevost-update.apk")
            val tempFile = File(context.cacheDir, "animevost-update.apk.part")
            tempFile.delete()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("APK download failed: HTTP ${response.code}")
                    }
                    val body = response.body ?: throw IOException("Empty response body for APK download")
                    val contentLength = body.contentLength()
                    var totalBytesRead = 0L

                    body.byteStream().use { input ->
                        tempFile.outputStream().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                totalBytesRead += read
                                if (contentLength > 0) {
                                    onProgress((totalBytesRead * 100 / contentLength).toInt())
                                }
                            }
                        }
                    }

                    if (contentLength > 0 && totalBytesRead != contentLength) {
                        throw IOException(
                            "Incomplete APK download: expected $contentLength bytes, got $totalBytesRead",
                        )
                    }
                }

                if (tempFile.length() == 0L) {
                    throw IOException("Downloaded APK is empty")
                }
                outFile.delete()
                if (!tempFile.renameTo(outFile)) {
                    throw IOException("Could not finalize downloaded APK")
                }
                onProgress(100)
                outFile
            } catch (error: Exception) {
                tempFile.delete()
                throw error
            }
        }

    /** Returns true when [remote] version string is strictly greater than [current]. */
    private fun isNewer(remote: String, current: String): Boolean =
        isNewerVersion(remote, current)

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
