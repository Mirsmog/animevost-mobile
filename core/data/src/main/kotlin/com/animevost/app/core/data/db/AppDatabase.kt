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
        WatchProgressEntity::class,
        UserListEntity::class,
        YummyMappingEntity::class,
        MalMappingEntity::class,
        SkipTimeEntity::class,
        ThemeFingerprintEntity::class,
        ThemeLookupEntity::class,
    ],
    version = 14,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun userListDao(): UserListDao
    abstract fun yummyMappingDao(): YummyMappingDao
    abstract fun malMappingDao(): MalMappingDao
    abstract fun skipTimeDao(): SkipTimeDao
    abstract fun themeFingerprintDao(): ThemeFingerprintDao
    abstract fun themeLookupDao(): ThemeLookupDao

    companion object {
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM mal_mapping")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `skip_segments`")
                db.execSQL("DROP TABLE IF EXISTS `mal_mapping`")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `user_list` (
                        `animeUrl` TEXT NOT NULL,
                        `animeId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `posterUrl` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`animeUrl`)
                    )""".trimIndent(),
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `mal_mapping` (
                        `animeId` INTEGER NOT NULL,
                        `malId` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`animeId`)
                    )""".trimIndent(),
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `skip_times` (
                        `animeId` INTEGER NOT NULL,
                        `episode` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `startMs` INTEGER NOT NULL,
                        `endMs` INTEGER NOT NULL,
                        PRIMARY KEY(`animeId`, `episode`, `type`)
                    )""".trimIndent(),
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `mal_mapping`")
                // Existing skip_times rows came from AniSkip MAL ids; drop them
                // because the new Alloha pipeline uses different timing data.
                db.execSQL("DELETE FROM `skip_times`")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `yummy_mapping` (
                        `animeId` INTEGER NOT NULL,
                        `yummyId` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`animeId`)
                    )""".trimIndent(),
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `skip_times` " +
                        "ADD COLUMN `source` TEXT NOT NULL DEFAULT 'ALLOHA'",
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `theme_fingerprints` (
                        `animeId` INTEGER NOT NULL,
                        `referenceId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `episodes` TEXT NOT NULL,
                        `durationMs` INTEGER NOT NULL,
                        `algorithmVersion` INTEGER NOT NULL,
                        `landmarks` BLOB NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`animeId`, `referenceId`)
                    )""".trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_theme_fingerprints_animeId_algorithmVersion` " +
                        "ON `theme_fingerprints` (`animeId`, `algorithmVersion`)",
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `theme_lookups` (
                        `animeId` INTEGER NOT NULL,
                        `episode` INTEGER NOT NULL,
                        `queryKey` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`animeId`, `episode`)
                    )""".trimIndent(),
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `skip_placements` (
                        `animeId` INTEGER NOT NULL,
                        `referenceId` INTEGER NOT NULL,
                        `episode` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `startMs` INTEGER NOT NULL,
                        `endMs` INTEGER NOT NULL,
                        `episodeDurationMs` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`animeId`, `referenceId`, `episode`)
                    )""".trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_skip_placements_animeId_referenceId` " +
                        "ON `skip_placements` (`animeId`, `referenceId`)",
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM `skip_times` WHERE `source` = 'LOCAL'")
                db.execSQL("DELETE FROM `skip_placements`")
                db.execSQL("DELETE FROM `theme_fingerprints`")
                db.execSQL("DELETE FROM `theme_lookups`")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM `skip_times` WHERE `source` = 'LOCAL'")
                db.execSQL("DELETE FROM `theme_fingerprints`")
                db.execSQL("DELETE FROM `theme_lookups`")
                db.execSQL("DROP TABLE IF EXISTS `skip_placements`")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `mal_mapping` (
                        `animeId` INTEGER NOT NULL,
                        `malId` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`animeId`)
                    )""".trimIndent(),
                )
                db.execSQL("DELETE FROM `skip_times` WHERE `source` = 'ALLOHA'")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `favorites` " +
                        "ADD COLUMN `releaseStatus` TEXT NOT NULL DEFAULT 'UNKNOWN'",
                )
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "animevost.db")
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                )
                .build()
    }
}
