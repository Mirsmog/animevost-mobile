package com.animevost.app.core.network.di

import com.animevost.app.core.network.AnimeVostApi
import com.animevost.app.core.network.BaseUrlInterceptor
import com.animevost.app.core.network.CookieStorage
import com.animevost.app.core.network.DleEndpoints
import com.animevost.app.core.network.HtmlFetcher
import com.animevost.app.core.network.SessionCookieJar
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
import java.util.concurrent.ConcurrentHashMap
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

    // ---------------------------------------------------------- Alloha / yummyanime

    @Provides
    @Singleton
    @Named("alloha")
    fun provideAllohaOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient {
        // Dedicated client: must NOT use BaseUrlInterceptor (which rewrites the
        // host to the AnimeVost mirror) and must persist cookies across the
        // yummyanime → iframe → POST chain.
        val cookieJar = object : CookieJar {
            private val store = ConcurrentHashMap<String, List<Cookie>>()
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                store[url.host] = cookies
            }
            override fun loadForRequest(url: HttpUrl): List<Cookie> =
                store[url.host].orEmpty()
        }
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
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
