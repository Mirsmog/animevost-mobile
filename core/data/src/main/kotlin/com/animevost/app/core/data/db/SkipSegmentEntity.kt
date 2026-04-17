package com.animevost.app.core.data.db

import androidx.room.Entity

@Entity(tableName = "skip_segments", primaryKeys = ["animeId", "type"])
data class SkipSegmentEntity(
    val animeId: Int,
    val type: String,
    val startMs: Long,
    val durationMs: Long,
    val windowMs: Long = 90_000L,
)
