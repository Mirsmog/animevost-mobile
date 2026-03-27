package com.animevost.app.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val animeId: Int,
    val title: String,
    val titleOriginal: String,
    val posterUrl: String,
    val episodeInfo: String,
    val url: String,
    val episodeName: String,
    val episodeVideoId: String,
    val watchedAt: Long = System.currentTimeMillis(),
)
