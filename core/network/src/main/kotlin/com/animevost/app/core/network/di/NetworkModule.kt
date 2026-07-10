package com.animevost.app.core.network.di

import com.animevost.app.core.network.BuildConfig
import com.animevost.app.core.network.alloha.AllohaApi
import com.animevost.app.core.network.alloha.AllohaIframeFetcher
import com.animevost.app.core.network.alloha.AllohaSkipClient
import com.animevost.app.core.network.alloha.YummyAnimeApi
import com.animevost.app.core.network.alloha.YummyAnimeSearchClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
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
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    @Named("rss")
    fun provideRssOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @Named("alloha")
    fun provideAllohaOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient {
        val cookieJar = object : CookieJar {
            private val lock = Any()
            private val store = mutableListOf<Cookie>()

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val now = System.currentTimeMillis()
                synchronized(lock) {
                    cookies.forEach { cookie ->
                        store.removeAll {
                            it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path
                        }
                        if (cookie.expiresAt > now) store += cookie
                    }
                    store.removeAll { it.expiresAt <= now }
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(lock) {
                store.removeAll { it.expiresAt <= System.currentTimeMillis() }
                store.filter { it.matches(url) }
            }
        }
        val browserHeadersInterceptor = okhttp3.Interceptor { chain ->
            val req = chain.request()
            val builder = req.newBuilder()
            if (req.header("User-Agent").isNullOrBlank()) {
                builder.header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
                )
            }
            if (req.header("Referer") == null && req.url.host == "yummyanime.tv") {
                builder.header("Referer", "https://yummyanime.tv/")
            }
            chain.proceed(builder.build())
        }
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(browserHeadersInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("yummyanime")
    fun provideYummyAnimeRetrofit(
        @Named("alloha") client: OkHttpClient,
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://yummyanime.tv/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideYummyAnimeApi(@Named("yummyanime") retrofit: Retrofit): YummyAnimeApi =
        retrofit.create(YummyAnimeApi::class.java)

    @Provides
    @Singleton
    @Named("alloha-bnsi")
    fun provideAllohaRetrofit(
        @Named("alloha") client: OkHttpClient,
    ): Retrofit = Retrofit.Builder()
        // Base URL is overridden per-request via @Url; this just needs to be valid.
        .baseUrl("https://absciss.thealloha.club/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideAllohaApi(@Named("alloha-bnsi") retrofit: Retrofit): AllohaApi =
        retrofit.create(AllohaApi::class.java)

    @Provides
    @Singleton
    fun provideAllohaIframeFetcher(
        @Named("alloha") client: OkHttpClient,
    ): AllohaIframeFetcher = AllohaIframeFetcher(client)

    @Provides
    @Singleton
    fun provideYummyAnimeSearchClient(
        @Named("alloha") client: OkHttpClient,
    ): YummyAnimeSearchClient = YummyAnimeSearchClient(client)

    @Provides
    @Singleton
    fun provideAllohaSkipClient(
        yummyApi: YummyAnimeApi,
        allohaApi: AllohaApi,
        iframeFetcher: AllohaIframeFetcher,
    ): AllohaSkipClient = AllohaSkipClient(yummyApi, allohaApi, iframeFetcher)
}
