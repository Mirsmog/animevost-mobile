package com.animevost.app.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local cache of an anime's watch status.
 * [animeUrl] is the relative URL path (e.g. "tip/tv/3774-slug.html") — stable across mirrors.
 * Title and posterUrl may be empty when the entry was synced from remote without local preview data.
 */
@Entity(tableName = "user_list")
data class UserListEntity(
    @PrimaryKey
    val animeUrl: String,
    val animeId: Int,
    val title: String,
    val posterUrl: String,
    val status: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
