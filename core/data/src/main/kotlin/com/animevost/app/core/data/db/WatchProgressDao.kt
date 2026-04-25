package com.animevost.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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

    /**
     * One row per anime — the most recently updated progress entry — ordered newest first.
     * Used to power the "Continue watching" rail.
     */
    @Query(
        """
        SELECT wp.* FROM watch_progress wp
        INNER JOIN (
            SELECT animeId, MAX(updatedAt) AS lastAt FROM watch_progress GROUP BY animeId
        ) latest ON latest.animeId = wp.animeId AND latest.lastAt = wp.updatedAt
        ORDER BY wp.updatedAt DESC
        """,
    )
    fun observeLatestPerAnime(): Flow<List<WatchProgressEntity>>

    @Query("DELETE FROM watch_progress WHERE animeId = :animeId")
    suspend fun clearForAnime(animeId: Int)
}
