package com.animevost.app.feature.player

import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.model.VideoSource

const val START_FROM_BEGINNING = "startFromBeginning"

data class PlayerUiState(
    val animeTitle: String = "",
    val posterUrl: String = "",
    val videoSources: List<VideoSource> = emptyList(),
    val selectedQuality: String = "SD (480p)",
    val currentEpisode: Episode? = null,
    val isLoading: Boolean = false,
    val speedHintAvailable: Boolean = false,
    val error: String? = null,
    val allEpisodes: List<Episode> = emptyList(),
    val currentEpisodeIndex: Int = 0,
    val resumePosition: ResumePosition = ResumePosition.Loading,
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

sealed interface ResumePosition {
    data object Loading : ResumePosition
    data object Handled : ResumePosition
    data class Pending(val positionMs: Long) : ResumePosition
}

internal fun ResumePosition.consumeWhenReady(
    isReady: Boolean,
    canSeek: Boolean,
    seekTo: (Long) -> Unit,
): Boolean {
    if (this !is ResumePosition.Pending || !isReady) return false
    if (canSeek) seekTo(positionMs)
    return true
}

sealed interface PlayerEvent {
    data class SelectQuality(val quality: String) : PlayerEvent
    data object SpeedHintShown : PlayerEvent
    data object NextEpisode : PlayerEvent
    data object PreviousEpisode : PlayerEvent
    data class UpdateProgress(val videoId: String, val positionMs: Long, val durationMs: Long) : PlayerEvent
    data object ShowEpisodeComments : PlayerEvent
    data object HideEpisodeComments : PlayerEvent
    data object LoadMoreEpisodeComments : PlayerEvent
    data object RetryEpisodeComments : PlayerEvent
    data class ResumeConsumed(val videoId: String) : PlayerEvent
}
