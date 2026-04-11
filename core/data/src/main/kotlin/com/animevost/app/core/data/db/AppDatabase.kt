package com.animevost.app.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        HistoryEntity::class,
        MalMappingEntity::class,
        SkipSegmentEntity::class,
        WatchProgressEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun malMappingDao(): MalMappingDao
    abstract fun skipSegmentDao(): SkipSegmentDao
    abstract fun watchProgressDao(): WatchProgressDao

    companion object {
        /**
         * Migrates from version 1 (favorites + history) to version 2,
         * which introduced the mal_mapping and skip_segments tables.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `mal_mapping` (
                        `animeId` INTEGER NOT NULL,
                        `malId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`animeId`)
                    )""".trimIndent(),
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `skip_segments` (
                        `animeId` INTEGER NOT NULL,
                        `episodeName` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `startMs` INTEGER NOT NULL,
                        `endMs` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        PRIMARY KEY(`animeId`, `episodeName`, `type`)
                    )""".trimIndent(),
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `watch_progress` (
                        `animeId` INTEGER NOT NULL,
                        `episodeVideoId` TEXT NOT NULL,
                        `episodeName` TEXT NOT NULL,
                        `episodeIndex` INTEGER NOT NULL,
                        `positionMs` INTEGER NOT NULL,
                        `durationMs` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`animeId`, `episodeVideoId`)
                    )""".trimIndent(),
                )
            }
        }

        /** Clears stale MAL ID mappings so the improved resolver re-fetches them. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM mal_mapping")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "animevost.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
