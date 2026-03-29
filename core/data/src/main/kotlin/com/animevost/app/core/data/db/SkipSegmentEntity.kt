package com.animevost.app.core.data.db

import androidx.room.Entity

@Entity(
    tableName = "skip_segments",
    primaryKeys = ["animeId", "episodeName", "type"],
)
data class SkipSegmentEntity(
    val animeId: Int,
    val episodeName: String,
    val type: String,
    val startMs: Long,
    val endMs: Long,
    val source: String,
)
