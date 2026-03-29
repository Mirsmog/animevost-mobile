package com.animevost.app.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        HistoryEntity::class,
        MalMappingEntity::class,
        SkipSegmentEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun malMappingDao(): MalMappingDao
    abstract fun skipSegmentDao(): SkipSegmentDao
}
