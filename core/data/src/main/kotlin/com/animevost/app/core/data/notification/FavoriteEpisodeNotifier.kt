package com.animevost.app.core.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.animevost.app.core.data.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class FavoriteEpisodeNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("rss") private val client: OkHttpClient,
) {
    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun prepare() {
        createChannel()
        cancelLegacyNotifications()
    }

    suspend fun show(updates: List<FavoriteEpisodeUpdate>) {
        if (updates.isEmpty() || !canPostNotifications()) return

        prepare()

        updates.forEach { update ->
            manager.notify(
                notificationId(update.favorite.newsId),
                buildNotification(update),
            )
        }
        if (updates.size > 1) {
            manager.notify(SUMMARY_NOTIFICATION_ID, buildSummaryNotification(updates))
        } else {
            manager.cancel(SUMMARY_NOTIFICATION_ID)
        }
    }

    private suspend fun buildNotification(update: FavoriteEpisodeUpdate) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_anime)
            .setContentTitle(update.favorite.title)
            .setContentText(update.message())
            .setSubText("Новая серия")
            .setContentIntent(contentIntent(update))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setGroup(GROUP_KEY)
            .setNumber(update.currentEpisode)
            .apply {
                val poster = loadPoster(update.favorite.posterUrl)
                if (poster != null) {
                    setLargeIcon(poster)
                    setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(poster)
                            .setSummaryText(update.message()),
                    )
                } else {
                    setStyle(NotificationCompat.BigTextStyle().bigText(update.message()))
                }
            }
            .build()

    private fun buildSummaryNotification(updates: List<FavoriteEpisodeUpdate>) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_anime)
            .setContentTitle("Новые серии в избранном")
            .setContentText("Обновлений: ${updates.size}")
            .setStyle(
                NotificationCompat.InboxStyle()
                    .also { style ->
                        updates.forEach { update ->
                            style.addLine("${update.favorite.title}: ${update.message().lowercase()}")
                        }
                    }
                    .setSummaryText("AnimeVost"),
            )
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

    private fun contentIntent(update: FavoriteEpisodeUpdate): PendingIntent? {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(AnimeNotificationIntent.EXTRA_ANIME_URL, update.favorite.url)
            }
            ?: return null
        return PendingIntent.getActivity(
            context,
            update.favorite.newsId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private suspend fun loadPoster(rawUrl: String): Bitmap? {
        if (rawUrl.isBlank()) return null
        val url = runCatching { URI(RSS_BASE_URL).resolve(rawUrl).toString() }.getOrNull()
            ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    response.body?.byteStream()?.use(BitmapFactory::decodeStream)
                }
            }.getOrNull()
        }
    }

    private fun FavoriteEpisodeUpdate.message(): String =
        if (currentEpisode - previousEpisode == 1) {
            "Вышла $currentEpisode серия"
        } else {
            "Вышли серии ${previousEpisode + 1}-$currentEpisode"
        }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun createChannel() {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Новые серии избранного",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Уведомления о новых сериях аниме из избранного"
            },
        )
    }

    private fun cancelLegacyNotifications() {
        repeat(LEGACY_NOTIFICATION_COUNT) { index ->
            manager.cancel(LEGACY_NOTIFICATION_BASE_ID + index)
        }
    }

    private fun notificationId(animeId: Int): Int =
        FAVORITE_NOTIFICATION_BASE_ID + animeId

    private companion object {
        const val CHANNEL_ID = "anime_updates"
        const val GROUP_KEY = "favorite_episode_updates"
        const val RSS_BASE_URL = "https://v13.vost.pw/"
        const val SUMMARY_NOTIFICATION_ID = 9_999
        const val FAVORITE_NOTIFICATION_BASE_ID = 100_000
        const val LEGACY_NOTIFICATION_BASE_ID = 1_000
        const val LEGACY_NOTIFICATION_COUNT = 5
    }
}
