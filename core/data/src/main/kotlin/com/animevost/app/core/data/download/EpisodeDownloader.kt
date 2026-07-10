package com.animevost.app.core.data.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.animevost.app.core.domain.repository.EpisodeDownloadManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
) : EpisodeDownloadManager {
    override fun enqueue(url: String, fileName: String, title: String): Long {
        val uri = Uri.parse(url)
        require(uri.scheme == "https" || uri.scheme == "http") { "Unsupported download URL" }
        require(!uri.host.isNullOrBlank()) { "Download URL has no host" }
        val sanitized = fileName
            .replace(Regex("[^a-zA-Zа-яА-ЯёЁ0-9._\\- ]"), "_")
            .trim()
            .ifBlank { "episode" }
        val request = DownloadManager.Request(uri)
            .setTitle(title)
            .setDescription("Загрузка: $sanitized")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_MOVIES,
                "AnimeVost/$sanitized.mp4",
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }
}
