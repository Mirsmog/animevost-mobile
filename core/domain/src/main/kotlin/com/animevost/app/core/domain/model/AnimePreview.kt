package com.animevost.app.core.domain.model

data class AnimePreview(
    val id: Int,
    val title: String,
    val titleOriginal: String,
    val posterUrl: String,
    val episodeInfo: String,
    val url: String,
)
