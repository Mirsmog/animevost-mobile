package com.animevost.app.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserListDao {

    @Query("SELECT * FROM user_list WHERE status = :statusCode ORDER BY updatedAt DESC")
    fun getByStatus(statusCode: String): Flow<List<UserListEntity>>

    @Query("SELECT status FROM user_list WHERE animeUrl = :url")
    fun getStatusFlow(url: String): Flow<String?>

    @Upsert
    suspend fun upsert(entity: UserListEntity)

    @Query("DELETE FROM user_list WHERE animeUrl = :url")
    suspend fun deleteByUrl(url: String)

    @Query("SELECT * FROM user_list")
    suspend fun getAll(): List<UserListEntity>

    @Query("DELETE FROM user_list")
    suspend fun deleteAll()
}
