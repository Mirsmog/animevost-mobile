package com.animevost.app.core.data.db

import androidx.room.Entity

@Entity(
    tableName = "theme_lookups",
    primaryKeys = ["animeId", "episode"],
)
data class ThemeLookupEntity(
    val animeId: Int,
    val episode: Int,
    val queryKey: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
