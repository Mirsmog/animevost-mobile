package com.animevost.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SkipTimeDao {
    @Query("SELECT * FROM skip_times WHERE animeId = :animeId AND episode = :episode")
    suspend fun get(animeId: Int, episode: Int): List<SkipTimeEntity>

    @Query(
        "SELECT * FROM skip_times " +
            "WHERE animeId = :animeId AND episode = :episode AND source = :source",
    )
    suspend fun getBySource(animeId: Int, episode: Int, source: String): List<SkipTimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SkipTimeEntity>)

}
