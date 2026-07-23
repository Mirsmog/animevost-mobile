package com.animevost.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ThemeFingerprintDao {
    @Query(
        "SELECT * FROM theme_fingerprints " +
            "WHERE animeId = :animeId AND algorithmVersion = :algorithmVersion",
    )
    suspend fun get(animeId: Int, algorithmVersion: Int): List<ThemeFingerprintEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ThemeFingerprintEntity)

    @Query(
        "DELETE FROM theme_fingerprints " +
            "WHERE animeId = :animeId AND algorithmVersion != :algorithmVersion",
    )
    suspend fun deleteOutdated(animeId: Int, algorithmVersion: Int)
}
