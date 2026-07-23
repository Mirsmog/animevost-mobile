package com.animevost.app.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.animevost.app.core.data.db.AppDatabase
import com.animevost.app.core.data.db.FavoriteDao
import com.animevost.app.core.data.db.HistoryDao
import com.animevost.app.core.data.db.SkipTimeDao
import com.animevost.app.core.data.db.ThemeFingerprintDao
import com.animevost.app.core.data.db.ThemeLookupDao
import com.animevost.app.core.data.db.UserListDao
import com.animevost.app.core.data.db.WatchProgressDao
import com.animevost.app.core.data.db.YummyMappingDao
import com.animevost.app.core.data.download.EpisodeDownloader
import com.animevost.app.core.data.repository.FeatureFlagsRepositoryImpl
import com.animevost.app.core.data.repository.AnimeRepositoryImpl
import com.animevost.app.core.data.repository.UpdateRepositoryImpl
import com.animevost.app.core.data.repository.AuthRepositoryImpl
import com.animevost.app.core.data.repository.CommentRepositoryImpl
import com.animevost.app.core.data.repository.FavoriteRepositoryImpl
import com.animevost.app.core.data.repository.HistoryRepositoryImpl
import com.animevost.app.core.data.repository.NotificationPreferencesRepositoryImpl
import com.animevost.app.core.data.repository.ScheduleRepositoryImpl
import com.animevost.app.core.data.repository.SkipTimesRepositoryImpl
import com.animevost.app.core.data.repository.UserListRepositoryImpl
import com.animevost.app.core.data.repository.UserPreferencesRepositoryImpl
import com.animevost.app.core.data.repository.WatchProgressRepositoryImpl
import com.animevost.app.core.data.repository.VideoRepositoryImpl
import com.animevost.app.core.data.skip.LocalSkipDetectorImpl
import com.animevost.app.core.domain.repository.FeatureFlagsRepository
import com.animevost.app.core.domain.repository.EpisodeDownloadManager
import com.animevost.app.core.domain.repository.AnimeRepository
import com.animevost.app.core.domain.repository.UpdateRepository
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.domain.repository.CommentRepository
import com.animevost.app.core.domain.repository.FavoriteRepository
import com.animevost.app.core.domain.repository.HistoryRepository
import com.animevost.app.core.domain.repository.LocalSkipDetector
import com.animevost.app.core.domain.repository.NotificationPreferencesRepository
import com.animevost.app.core.domain.repository.ScheduleRepository
import com.animevost.app.core.domain.repository.SkipTimesRepository
import com.animevost.app.core.domain.repository.UserListRepository
import com.animevost.app.core.domain.repository.UserPreferencesRepository
import com.animevost.app.core.domain.repository.WatchProgressRepository
import com.animevost.app.core.domain.repository.VideoRepository
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
    fun provideWatchProgressDao(db: AppDatabase): WatchProgressDao = db.watchProgressDao()

    @Provides
    fun provideUserListDao(db: AppDatabase): UserListDao = db.userListDao()

    @Provides
    fun provideYummyMappingDao(db: AppDatabase): YummyMappingDao = db.yummyMappingDao()

    @Provides
    fun provideSkipTimeDao(db: AppDatabase): SkipTimeDao = db.skipTimeDao()

    @Provides
    fun provideThemeFingerprintDao(db: AppDatabase): ThemeFingerprintDao = db.themeFingerprintDao()

    @Provides
    fun provideThemeLookupDao(db: AppDatabase): ThemeLookupDao = db.themeLookupDao()

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
    abstract fun bindEpisodeDownloadManager(impl: EpisodeDownloader): EpisodeDownloadManager

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
    abstract fun bindNotificationPreferencesRepository(
        impl: NotificationPreferencesRepositoryImpl,
    ): NotificationPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindWatchProgressRepository(impl: WatchProgressRepositoryImpl): WatchProgressRepository

    @Binds
    @Singleton
    abstract fun bindUpdateRepository(impl: UpdateRepositoryImpl): UpdateRepository

    @Binds
    @Singleton
    abstract fun bindUserListRepository(impl: UserListRepositoryImpl): UserListRepository

    @Binds
    @Singleton
    abstract fun bindSkipTimesRepository(impl: SkipTimesRepositoryImpl): SkipTimesRepository

    @Binds
    @Singleton
    abstract fun bindLocalSkipDetector(impl: LocalSkipDetectorImpl): LocalSkipDetector

    @Binds
    @Singleton
    abstract fun bindFeatureFlagsRepository(impl: FeatureFlagsRepositoryImpl): FeatureFlagsRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository
}
