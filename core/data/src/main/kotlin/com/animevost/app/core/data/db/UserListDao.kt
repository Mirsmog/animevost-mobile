package com.animevost.app.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserListDao {

    @Query("SELECT * FROM user_list ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<UserListEntity>>

    @Query("SELECT * FROM user_list WHERE status = :statusCode ORDER BY updatedAt DESC")
    fun getByStatus(statusCode: String): Flow<List<UserListEntity>>

    @Query("SELECT status FROM user_list WHERE animeUrl = :url")
    fun getStatusFlow(url: String): Flow<String?>

    @Query("SELECT status FROM user_list WHERE animeId = :newsId LIMIT 1")
    fun getStatusByNewsIdFlow(newsId: Int): Flow<String?>

    @Query("SELECT * FROM user_list WHERE animeId = :newsId LIMIT 1")
    suspend fun getByNewsId(newsId: Int): UserListEntity?

    @Upsert
    suspend fun upsert(entity: UserListEntity)

    @Query("DELETE FROM user_list WHERE animeUrl = :url")
    suspend fun deleteByUrl(url: String)

    @Query("DELETE FROM user_list WHERE animeId = :newsId")
    suspend fun deleteByNewsId(newsId: Int)

    @Query("SELECT * FROM user_list")
    suspend fun getAll(): List<UserListEntity>

    @Query("DELETE FROM user_list")
    suspend fun deleteAll()

    /** Updates title/posterUrl/animeId only if the entry exists and title is currently blank. */
    @Query("UPDATE user_list SET title=:title, posterUrl=:posterUrl, animeId=:animeId WHERE animeUrl=:url AND title=''")
    suspend fun enrichIfBlank(url: String, title: String, posterUrl: String, animeId: Int)

    @Query("UPDATE user_list SET title=:title, posterUrl=:posterUrl WHERE animeId=:newsId AND title=''")
    suspend fun enrichByNewsIdIfBlank(newsId: Int, title: String, posterUrl: String)

    @Transaction
    suspend fun upsertByNewsId(entity: UserListEntity) {
        if (entity.animeId > 0) deleteByNewsId(entity.animeId)
        upsert(entity)
    }
}
