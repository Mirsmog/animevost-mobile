package com.animevost.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ThemeLookupDao {
    @Query("SELECT * FROM theme_lookups WHERE animeId = :animeId AND episode = :episode")
    suspend fun get(animeId: Int, episode: Int): ThemeLookupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ThemeLookupEntity)
}
