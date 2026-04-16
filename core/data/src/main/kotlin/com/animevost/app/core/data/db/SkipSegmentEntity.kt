package com.animevost.app.core.data.db

import androidx.room.Entity

@Entity(tableName = "skip_segments", primaryKeys = ["animeId", "type"])
data class SkipSegmentEntity(
    val animeId: Int,
    val type: String,
    val hash0: Long,
    val hash3: Long,
    val hash6: Long,
    val durationMs: Long,
    val referenceTimeMs: Long,
)
