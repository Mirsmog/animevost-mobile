package com.animevost.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface YummyMappingDao {
    @Query("SELECT * FROM yummy_mapping WHERE animeId = :animeId")
    suspend fun get(animeId: Int): YummyMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: YummyMappingEntity)
}
