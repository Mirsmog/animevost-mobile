package com.animevost.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SkipSegmentDao {

    @Query("SELECT * FROM skip_segments WHERE animeId = :animeId")
    suspend fun getByAnimeId(animeId: Int): List<SkipSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SkipSegmentEntity)

    @Query("DELETE FROM skip_segments WHERE animeId = :animeId AND type = :type")
    suspend fun delete(animeId: Int, type: String)
}
