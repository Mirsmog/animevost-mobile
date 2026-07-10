package com.animevost.app.core.data.di

import com.animevost.app.core.data.sdk.EncryptedAnimeVostCookieStore
import com.animevost.sdk.AnimeVostClient
import com.animevost.sdk.config.AnimeVostConfig
import com.animevost.sdk.http.AnimeVostCookieStore
import com.animevost.sdk.http.OkHttpAnimeVostHttpClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnimeVostSdkBindsModule {

    @Binds
    @Singleton
    abstract fun bindAnimeVostCookieStore(
        impl: EncryptedAnimeVostCookieStore,
    ): AnimeVostCookieStore
}

@Module
@InstallIn(SingletonComponent::class)
object AnimeVostSdkProvidesModule {

    @Provides
    @Singleton
    fun provideAnimeVostClient(cookieStore: AnimeVostCookieStore): AnimeVostClient =
        AnimeVostClient(
            config = AnimeVostConfig(baseUrl = "https://v13.vost.pw/"),
            httpClient = OkHttpAnimeVostHttpClient(cookieStore = cookieStore),
        )
}
