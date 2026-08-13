package com.animevost.app.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val newsId: Int,
    val title: String,
    val titleOriginal: String,
    val posterUrl: String,
    val episodeInfo: String,
    val url: String,
    val addedAt: Long = System.currentTimeMillis(),
    val releaseStatus: String = "UNKNOWN",
)
