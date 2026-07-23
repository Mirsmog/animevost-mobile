package com.animevost.app.core.data.db

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "theme_fingerprints",
    primaryKeys = ["animeId", "referenceId"],
    indices = [Index(value = ["animeId", "algorithmVersion"])],
)
data class ThemeFingerprintEntity(
    val animeId: Int,
    val referenceId: Int,
    val type: String,
    val episodes: String,
    val durationMs: Long,
    val algorithmVersion: Int,
    val landmarks: ByteArray,
    val updatedAt: Long = System.currentTimeMillis(),
)
