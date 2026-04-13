package com.animevost.app.core.network.di

import com.animevost.app.core.network.AnimeVostApi
import com.animevost.app.core.network.BaseUrlInterceptor
import com.animevost.app.core.network.CookieStorage
import com.animevost.app.core.network.DleEndpoints
import com.animevost.app.core.network.HtmlFetcher
import com.animevost.app.core.network.SessionCookieJar
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideSessionCookieJar(storage: CookieStorage): SessionCookieJar {
        return SessionCookieJar(storage)
    }

    @Provides
    @Singleton
    @Named("resolver")
    fun provideResolverOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        cookieJar: SessionCookieJar,
        baseUrlInterceptor: BaseUrlInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(baseUrlInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(DleEndpoints.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAnimeVostApi(retrofit: Retrofit): AnimeVostApi {
        return retrofit.create(AnimeVostApi::class.java)
    }

    @Provides
    @Singleton
    fun provideHtmlFetcher(client: OkHttpClient): HtmlFetcher {
        return HtmlFetcher(client)
    }
}
