package com.animevost.app.core.domain.model

/**
 * Full details for a single anime title, fetched from the detail page.
 *
 * @property id Unique DLE news ID — matches [AnimePreview.id].
 * @property url Canonical URL of the detail page.
 * @property episodes Ordered list of available episodes with their playback IDs.
 */
data class AnimeDetail(
    val id: Int,
    val url: String = "",
    val title: String,
    val titleOriginal: String,
    val titleAlternative: String = "",
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
    val totalCommentPages: Int,
    val description: String,
    val publishDate: String,
    val categories: List<String>,
    val relatedAnime: List<AnimePreview>,
    val relatedSeries: List<RelatedSeries>,
    val episodes: List<Episode>,
    val releaseStatus: AnimeReleaseStatus = AnimeReleaseStatus.UNKNOWN,
    val episodeInfo: String = "",
    val upcomingEpisode: UpcomingEpisode? = null,
)

data class UpcomingEpisode(
    val number: Int,
    val scheduledAtEpochSeconds: Long,
)
