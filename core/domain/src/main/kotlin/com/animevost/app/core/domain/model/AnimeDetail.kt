package com.animevost.app.core.domain.model

data class AnimeDetail(
    val id: Int,
    val url: String = "",
    val title: String,
    val titleOriginal: String,
    val posterUrl: String,
    val year: String,
    val genres: List<Genre>,
    val type: AnimeType,
    val episodeCount: String,
    val director: String,
    val rating: Double,
    val voteCount: Int,
    val viewCount: Int,
    val commentCount: Int,
    val description: String,
    val publishDate: String,
    val categories: List<String>,
    val relatedAnime: List<AnimePreview>,
    val relatedSeries: List<RelatedSeries>,
    val episodes: List<Episode>,
)
