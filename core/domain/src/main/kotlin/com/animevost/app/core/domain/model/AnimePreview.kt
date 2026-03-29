package com.animevost.app.core.domain.model

data class AnimePreview(
    val id: Int,
    val title: String,
    val titleOriginal: String,
    val posterUrl: String,
    val episodeInfo: String,
    val url: String,
    val type: String = "",
    val rating: Double = 0.0,
    val viewCount: Int = 0,
    val commentCount: Int = 0,
)
