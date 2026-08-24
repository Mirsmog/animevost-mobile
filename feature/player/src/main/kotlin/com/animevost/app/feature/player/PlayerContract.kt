package com.animevost.app.feature.player

import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.model.VideoSource

data class PlayerUiState(
    val videoSources: List<VideoSource> = emptyList(),
    val selectedQuality: String = "SD (480p)",
    val currentEpisode: Episode? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val allEpisodes: List<Episode> = emptyList(),
    val currentEpisodeIndex: Int = 0,
    val resumePositionMs: Long = 0L,
    val isCommentsPanelVisible: Boolean = false,
    val commentsEpisodeNumber: Int? = null,
    val episodeComments: List<Comment> = emptyList(),
    val isLoadingEpisodeComments: Boolean = false,
    val hasMoreEpisodeComments: Boolean = false,
    val episodeCommentsError: String? = null,
) {
    val hasPrevious: Boolean get() = currentEpisodeIndex > 0
    val hasNext: Boolean get() = currentEpisodeIndex < allEpisodes.lastIndex
    val currentVideoUrl: String?
        get() = videoSources.firstOrNull { it.quality == selectedQuality }?.url
            ?: videoSources.firstOrNull()?.url
}

sealed interface PlayerEvent {
    data class SelectQuality(val quality: String) : PlayerEvent
    data class NextEpisode(
        val currentPositionMs: Long = 0L,
        val currentDurationMs: Long = 0L,
    ) : PlayerEvent

    data class PreviousEpisode(
        val currentPositionMs: Long = 0L,
        val currentDurationMs: Long = 0L,
    ) : PlayerEvent

    data class UpdateProgress(val positionMs: Long, val durationMs: Long) : PlayerEvent
    data object ShowEpisodeComments : PlayerEvent
    data object HideEpisodeComments : PlayerEvent
    data object LoadMoreEpisodeComments : PlayerEvent
    data object RetryEpisodeComments : PlayerEvent
    data object ResumeConsumed : PlayerEvent
}
