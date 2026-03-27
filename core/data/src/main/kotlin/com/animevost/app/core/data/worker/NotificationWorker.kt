package com.animevost.app.core.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.animevost.app.core.network.DleEndpoints
import com.animevost.app.core.network.HtmlFetcher
import com.animevost.app.core.network.parser.RssParser
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val htmlFetcher: HtmlFetcher,
    private val rssParser: RssParser,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val xml = htmlFetcher.fetch(DleEndpoints.BASE_URL + "rss.xml")
            val items = rssParser.parse(xml)
            if (items.isEmpty()) return Result.success()

            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastKnownTitle = prefs.getString(KEY_LAST_RSS_TITLE, null)

            val newItems = if (lastKnownTitle == null) {
                // First run — save reference point, don't spam notifications
                emptyList()
            } else {
                items.takeWhile { it.title != lastKnownTitle }
            }

            // Always update the reference to the latest item
            prefs.edit().putString(KEY_LAST_RSS_TITLE, items.first().title).apply()

            newItems.take(MAX_NOTIFICATIONS).forEachIndexed { index, item ->
                showNotification(item.title, item.description, index)
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(title: String, body: String, id: Int) {
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Обновления аниме",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Уведомления о новых сериях"
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(NOTIFICATION_BASE_ID + id, notification)
    }

    companion object {
        private const val CHANNEL_ID = "anime_updates"
        private const val PREFS_NAME = "notifications"
        private const val KEY_LAST_RSS_TITLE = "last_rss_title"
        private const val NOTIFICATION_BASE_ID = 1000
        private const val MAX_NOTIFICATIONS = 5
        private const val WORK_NAME = "anime_notifications"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<NotificationWorker>(
                30, TimeUnit.MINUTES,
                15, TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
