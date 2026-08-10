package com.animevost.app

import android.app.Application
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.animevost.app.BuildConfig
import com.animevost.app.core.data.notification.FavoriteEpisodeNotifier
import com.animevost.app.core.data.worker.NotificationWorker
import com.animevost.app.core.domain.model.AppBuildInfo
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var favoriteEpisodeNotifier: FavoriteEpisodeNotifier

    @Inject
    lateinit var appBuildInfo: AppBuildInfo

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        if (appBuildInfo.backgroundNotificationsEnabled) {
            favoriteEpisodeNotifier.prepare()
            NotificationWorker.schedule(this)
        } else {
            NotificationWorker.cancel(this)
        }
    }
}
