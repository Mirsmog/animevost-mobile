package com.animevost.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SkipSegmentDao {

    @Query("SELECT * FROM skip_segments WHERE animeId = :animeId AND episodeName = :episodeName")
    suspend fun getSegments(animeId: Int, episodeName: String): List<SkipSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegment(segment: SkipSegmentEntity)
}
