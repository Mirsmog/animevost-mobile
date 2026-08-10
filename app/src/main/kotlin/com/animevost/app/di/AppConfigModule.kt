package com.animevost.app.di

import com.animevost.app.BuildConfig
import com.animevost.app.core.domain.model.AppBuildInfo
import com.animevost.app.core.domain.model.AppDistribution
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {
    @Provides
    @Singleton
    fun provideAppBuildInfo(): AppBuildInfo = AppBuildInfo(
        distribution = if (BuildConfig.IS_DEVELOPMENT) {
            AppDistribution.DEV
        } else {
            AppDistribution.PROD
        },
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        inAppUpdatesEnabled = BuildConfig.ENABLE_IN_APP_UPDATES,
        backgroundNotificationsEnabled = BuildConfig.ENABLE_BACKGROUND_NOTIFICATIONS,
    )
}
