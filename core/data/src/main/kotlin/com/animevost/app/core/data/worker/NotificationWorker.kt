package com.animevost.app.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.animevost.app.core.data.db.FavoriteDao
import com.animevost.app.core.data.notification.FavoriteEpisodeNotifier
import com.animevost.app.core.data.notification.FavoriteEpisodeStateStore
import com.animevost.app.core.data.notification.FavoriteEpisodeUpdateDetector
import com.animevost.app.core.domain.repository.NotificationPreferencesRepository
import com.animevost.app.core.network.parser.RssParser
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Named

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    @Named("rss") private val client: OkHttpClient,
    private val rssParser: RssParser,
    private val favoriteDao: FavoriteDao,
    private val detector: FavoriteEpisodeUpdateDetector,
    private val stateStore: FavoriteEpisodeStateStore,
    private val preferencesRepository: NotificationPreferencesRepository,
    private val notifier: FavoriteEpisodeNotifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val favorites = favoriteDao.getAllList()
            val state = stateStore.read()
            if (favorites.isEmpty()) {
                stateStore.replace(emptyMap())
                return Result.success()
            }

            val feedItems = rssParser.parse(fetchRss())
            if (feedItems.isEmpty()) return Result.success()

            val detection = detector.detect(
                favorites = favorites,
                feedItems = feedItems,
                knownEpisodes = state.episodesByAnimeId,
            )
            stateStore.replace(detection.episodesByAnimeId)

            if (!state.initialized) return Result.success()
            if (!preferencesRepository.favoriteNotificationsEnabled.first()) return Result.success()

            val mutedIds = preferencesRepository.mutedFavoriteIds.first()
            val updates = detection.updates
                .filterNot { it.favorite.newsId in mutedIds }
                .take(MAX_NOTIFICATIONS_PER_RUN)
            notifier.show(updates)
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun fetchRss(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(RSS_URL).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("RSS request failed: HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
    }

    companion object {
        private const val RSS_URL = "https://v13.vost.pw/rss.xml"
        private const val MAX_NOTIFICATIONS_PER_RUN = 10
        private const val WORK_NAME = "anime_notifications"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(
                30,
                TimeUnit.MINUTES,
                15,
                TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
