package com.animevost.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MalMappingDao {

    @Query("SELECT * FROM mal_mapping WHERE animeId = :animeId LIMIT 1")
    suspend fun getMapping(animeId: Int): MalMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: MalMappingEntity)
}
