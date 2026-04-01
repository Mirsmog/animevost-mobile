package com.animevost.app.core.data.db

import androidx.room.Entity

@Entity(tableName = "watch_progress", primaryKeys = ["animeId", "episodeVideoId"])
data class WatchProgressEntity(
    val animeId: Int,
    val episodeVideoId: String,
    val episodeName: String,
    val episodeIndex: Int,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
)
