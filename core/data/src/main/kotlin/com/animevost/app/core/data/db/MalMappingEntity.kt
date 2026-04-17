package com.animevost.app.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mal_mapping")
data class MalMappingEntity(
    @PrimaryKey val animeId: Int,
    val malId: Int,
    val updatedAt: Long = System.currentTimeMillis(),
)
