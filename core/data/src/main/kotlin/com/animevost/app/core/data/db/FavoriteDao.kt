package com.animevost.app.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<FavoriteEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE newsId = :newsId)")
    suspend fun isFavorite(newsId: Int): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE newsId = :newsId)")
    fun isFavoriteFlow(newsId: Int): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM favorites")
    fun countFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE newsId = :newsId")
    suspend fun deleteByNewsId(newsId: Int)

    @Query("SELECT newsId FROM favorites")
    suspend fun getAllIds(): List<Int>
}
