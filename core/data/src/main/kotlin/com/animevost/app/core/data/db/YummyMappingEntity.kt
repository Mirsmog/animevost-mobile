package com.animevost.app.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached mapping from AnimeVost anime id to a yummyanime.tv anime id, used as
 * the entry point of the Alloha skipTime lookup pipeline.
 */
@Entity(tableName = "yummy_mapping")
data class YummyMappingEntity(
    @PrimaryKey val animeId: Int,
    val yummyId: Int,
    val updatedAt: Long = System.currentTimeMillis(),
)
