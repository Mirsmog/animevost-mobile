package com.animevost.app.core.data.db

import androidx.room.Entity

@Entity(tableName = "skip_times", primaryKeys = ["animeId", "episode", "type"])
data class SkipTimeEntity(
    val animeId: Int,
    val episode: Int,
    val type: String,
    val startMs: Long,
    val endMs: Long,
)
