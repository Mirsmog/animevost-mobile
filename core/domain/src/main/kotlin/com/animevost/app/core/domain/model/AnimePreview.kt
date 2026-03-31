package com.animevost.app.core.domain.model

/**
 * Lightweight representation of an anime item shown in lists and search results.
 *
 * @property id Unique DLE news ID used as the primary key throughout the app.
 * @property url Relative or absolute URL to the detail page.
 */
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
