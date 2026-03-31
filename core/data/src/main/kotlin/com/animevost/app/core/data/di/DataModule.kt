package com.animevost.app.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.animevost.app.core.data.db.AppDatabase
import com.animevost.app.core.data.db.FavoriteDao
import com.animevost.app.core.data.db.HistoryDao
import com.animevost.app.core.data.db.MalMappingDao
import com.animevost.app.core.data.db.SkipSegmentDao
import com.animevost.app.core.data.repository.AnimeRepositoryImpl
import com.animevost.app.core.data.repository.AuthRepositoryImpl
import com.animevost.app.core.data.repository.CommentRepositoryImpl
import com.animevost.app.core.data.repository.FavoriteRepositoryImpl
import com.animevost.app.core.data.repository.HistoryRepositoryImpl
import com.animevost.app.core.data.repository.ScheduleRepositoryImpl
import com.animevost.app.core.data.repository.SharedPrefsCookieStorage
import com.animevost.app.core.data.repository.SkipRepositoryImpl
import com.animevost.app.core.data.repository.VideoRepositoryImpl
import com.animevost.app.core.domain.repository.AnimeRepository
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.domain.repository.CommentRepository
import com.animevost.app.core.domain.repository.FavoriteRepository
import com.animevost.app.core.domain.repository.HistoryRepository
import com.animevost.app.core.domain.repository.ScheduleRepository
import com.animevost.app.core.domain.repository.SkipRepository
import com.animevost.app.core.domain.repository.VideoRepository
import com.animevost.app.core.network.CookieStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataProvidesModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.build(context)

    @Provides
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideMalMappingDao(db: AppDatabase): MalMappingDao = db.malMappingDao()

    @Provides
    fun provideSkipSegmentDao(db: AppDatabase): SkipSegmentDao = db.skipSegmentDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("animevost_prefs")
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindsModule {

    @Binds
    @Singleton
    abstract fun bindAnimeRepository(impl: AnimeRepositoryImpl): AnimeRepository

    @Binds
    @Singleton
    abstract fun bindVideoRepository(impl: VideoRepositoryImpl): VideoRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCommentRepository(impl: CommentRepositoryImpl): CommentRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindSkipRepository(impl: SkipRepositoryImpl): SkipRepository

    @Binds
    @Singleton
    abstract fun bindCookieStorage(impl: SharedPrefsCookieStorage): CookieStorage
}
