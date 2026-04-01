package com.animevost.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WatchProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WatchProgressEntity)

    @Query("SELECT * FROM watch_progress WHERE animeId = :animeId AND episodeVideoId = :episodeVideoId LIMIT 1")
    suspend fun get(animeId: Int, episodeVideoId: String): WatchProgressEntity?

    @Query("SELECT * FROM watch_progress WHERE animeId = :animeId ORDER BY updatedAt DESC")
    suspend fun getAllForAnime(animeId: Int): List<WatchProgressEntity>

    @Query("SELECT * FROM watch_progress WHERE animeId = :animeId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLastForAnime(animeId: Int): WatchProgressEntity?

    @Query("DELETE FROM watch_progress WHERE animeId = :animeId")
    suspend fun clearForAnime(animeId: Int)
}
