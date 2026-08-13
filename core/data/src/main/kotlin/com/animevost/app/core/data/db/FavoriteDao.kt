package com.animevost.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<FavoriteEntity>

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    suspend fun getAllList(): List<FavoriteEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE newsId = :newsId)")
    suspend fun isFavorite(newsId: Int): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE newsId = :newsId)")
    fun isFavoriteFlow(newsId: Int): Flow<Boolean>

    @Query("SELECT * FROM favorites WHERE newsId = :newsId LIMIT 1")
    suspend fun getByNewsId(newsId: Int): FavoriteEntity?

    @Query("SELECT COUNT(*) FROM favorites")
    fun countFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE newsId = :newsId")
    suspend fun deleteByNewsId(newsId: Int)

    @Query(
        "UPDATE favorites SET releaseStatus = :releaseStatus " +
            "WHERE newsId = :newsId AND releaseStatus != :releaseStatus",
    )
    suspend fun updateReleaseStatus(newsId: Int, releaseStatus: String)

    @Query("SELECT newsId FROM favorites")
    suspend fun getAllIds(): List<Int>

    @Query("DELETE FROM favorites")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entities: List<FavoriteEntity>) {
        deleteAll()
        if (entities.isNotEmpty()) insertAll(entities)
    }
}
