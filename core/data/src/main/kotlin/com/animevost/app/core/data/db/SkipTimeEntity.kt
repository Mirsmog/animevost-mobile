package com.animevost.app.core.data.db

import androidx.room.Entity
import androidx.room.ColumnInfo

@Entity(tableName = "skip_times", primaryKeys = ["animeId", "episode", "type"])
data class SkipTimeEntity(
    val animeId: Int,
    val episode: Int,
    val type: String,
    val startMs: Long,
    val endMs: Long,
    @ColumnInfo(defaultValue = "'ALLOHA'")
    val source: String = SOURCE_ALLOHA,
) {
    companion object {
        const val SOURCE_ALLOHA = "ALLOHA"
        const val SOURCE_ALLOHA_ANISKIP = "ALLOHA_ANISKIP_V1"
        const val SOURCE_LOCAL = "LOCAL"
    }
}
