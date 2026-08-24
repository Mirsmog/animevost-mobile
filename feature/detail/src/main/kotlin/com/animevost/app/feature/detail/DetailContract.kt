package com.animevost.app.feature.detail

import androidx.compose.ui.text.input.TextFieldValue
import com.animevost.app.core.domain.model.AnimeDetail
import com.animevost.app.core.domain.model.AnimeStatus
import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.domain.model.CommentScope
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.model.VideoSource

data class DetailUiState(
    val anime: AnimeDetail? = null,
    val animeUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val areFavoriteNotificationsEnabled: Boolean = true,
    val isFavoriteNotificationMuted: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userRating: Int = 0,
    val isRatingSubmitting: Boolean = false,
    val isDescriptionExpanded: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val commentScope: CommentScope = CommentScope.Anime,
    val isLoadingComments: Boolean = false,
    val commentTextValue: TextFieldValue = TextFieldValue(),
    val isAddingComment: Boolean = false,
    val commentsPage: Int = 1,
    val commentsTotalPages: Int = 1,
    val hasMoreComments: Boolean = false,
    val commentsFocusRequest: Int = 0,
    val replyTarget: Comment? = null,
    val replyMarkup: String = "",
    val isPreparingReply: Boolean = false,
    val reportTarget: Comment? = null,
    val reportTextValue: TextFieldValue = TextFieldValue(),
    val isReportingComment: Boolean = false,
    val deleteTarget: Comment? = null,
    val deletingCommentId: Int? = null,
    val downloadEpisodePending: Episode? = null,
    val downloadSources: List<VideoSource> = emptyList(),
    val isLoadingDownloadSources: Boolean = false,
    val watchedEpisodeIds: Set<String> = emptySet(),
    val continueEpisode: Episode? = null,
    val continuePositionMs: Long = 0L,
    val watchStatus: AnimeStatus? = null,
    val watchStatusEnabled: Boolean = false,
    val episodeRangeStart: Int = 0,
)

sealed interface DetailEvent {
    data class LoadAnime(val url: String) : DetailEvent
    data class RateAnime(val rating: Int) : DetailEvent
    data object ToggleFavorite : DetailEvent
    data object ToggleFavoriteNotification : DetailEvent
    data object ToggleDescription : DetailEvent
    data object LoadMoreComments : DetailEvent
    data class SelectCommentScope(
        val scope: CommentScope,
        val focusComments: Boolean = false,
    ) : DetailEvent
    data class UpdateCommentTextValue(val value: TextFieldValue) : DetailEvent
    data class ReplyToComment(val comment: Comment) : DetailEvent
    data object CancelReply : DetailEvent
    data object SubmitComment : DetailEvent
    data class ReportComment(val comment: Comment) : DetailEvent
    data class UpdateReportTextValue(val value: TextFieldValue) : DetailEvent
    data object SubmitReport : DetailEvent
    data object DismissReport : DetailEvent
    data class RequestDeleteComment(val comment: Comment) : DetailEvent
    data object ConfirmDeleteComment : DetailEvent
    data object DismissDeleteComment : DetailEvent
    data class ShowDownloadSheet(val episode: Episode) : DetailEvent
    data object HideDownloadSheet : DetailEvent
    data class DownloadWithQuality(val source: VideoSource) : DetailEvent
    data class SetWatchStatus(val status: AnimeStatus?) : DetailEvent
    data class SelectEpisodeRange(val start: Int) : DetailEvent
}

sealed interface DetailEffect {
    data class ShowSnackbar(val message: String) : DetailEffect
}
