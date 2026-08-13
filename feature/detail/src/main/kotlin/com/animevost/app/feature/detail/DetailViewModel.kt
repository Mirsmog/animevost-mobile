package com.animevost.app.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeDetail
import com.animevost.app.core.domain.model.AnimeStatus
import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.model.VideoSource
import com.animevost.app.core.domain.usecase.AddCommentUseCase
import com.animevost.app.core.domain.usecase.GetEpisodeDownloadSourcesUseCase
import com.animevost.app.core.domain.usecase.GetAnimeDetailUseCase
import com.animevost.app.core.domain.usecase.GetCommentsUseCase
import com.animevost.app.core.domain.usecase.RateAnimeUseCase
import com.animevost.app.core.domain.usecase.ToggleFavoriteUseCase
import com.animevost.app.core.domain.model.BetaFeature
import com.animevost.app.core.domain.repository.FeatureFlagsRepository
import com.animevost.app.core.domain.repository.EpisodeDownloadManager
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.domain.repository.FavoriteRepository
import com.animevost.app.core.domain.repository.NotificationPreferencesRepository
import com.animevost.app.core.domain.repository.UserPreferencesRepository
import com.animevost.app.core.domain.model.WatchProgress
import com.animevost.app.core.domain.repository.WatchProgressRepository
import com.animevost.app.core.domain.repository.UserListRepository
import com.animevost.app.core.domain.usecase.DeleteCommentUseCase
import com.animevost.app.core.domain.usecase.GetCommentReplyTemplateUseCase
import com.animevost.app.core.domain.usecase.ReportCommentUseCase
import com.animevost.app.core.domain.util.onSuccess
import com.animevost.app.core.domain.util.onError
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlin.math.max
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getAnimeDetailUseCase: GetAnimeDetailUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val favoriteRepository: FavoriteRepository,
    private val notificationPreferencesRepository: NotificationPreferencesRepository,
    private val getEpisodeDownloadSourcesUseCase: GetEpisodeDownloadSourcesUseCase,
    private val episodeDownloadManager: EpisodeDownloadManager,
    private val rateAnimeUseCase: RateAnimeUseCase,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val getCommentReplyTemplateUseCase: GetCommentReplyTemplateUseCase,
    private val reportCommentUseCase: ReportCommentUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase,
    private val authRepository: AuthRepository,
    private val watchProgressRepository: WatchProgressRepository,
    private val userListRepository: UserListRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val featureFlagsRepository: FeatureFlagsRepository,
) : ViewModel() {

    private var favoriteNotificationsEnabled = true
    private var mutedFavoriteIds = emptySet<Int>()

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<DetailEffect>()
    val effect: SharedFlow<DetailEffect> = _effect.asSharedFlow()

    init {
        authRepository.isLoggedInFlow
            .onEach { loggedIn -> _uiState.update { it.copy(isLoggedIn = loggedIn) } }
            .launchIn(viewModelScope)
        featureFlagsRepository.isEnabled(BetaFeature.WATCH_STATUS)
            .onEach { enabled -> _uiState.update { it.copy(watchStatusEnabled = enabled) } }
            .launchIn(viewModelScope)
        combine(
            notificationPreferencesRepository.favoriteNotificationsEnabled,
            notificationPreferencesRepository.mutedFavoriteIds,
        ) { enabled, mutedIds -> enabled to mutedIds }
            .onEach { (enabled, mutedIds) ->
                favoriteNotificationsEnabled = enabled
                mutedFavoriteIds = mutedIds
                _uiState.update { state ->
                    state.copy(
                        areFavoriteNotificationsEnabled = enabled,
                        isFavoriteNotificationMuted = state.anime?.id in mutedIds,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private var watchStatusJob: kotlinx.coroutines.Job? = null

    private fun observeWatchStatus(url: String) {
        watchStatusJob?.cancel()
        watchStatusJob = userListRepository.getStatusFlow(url)
            .onEach { status -> _uiState.update { it.copy(watchStatus = status) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: DetailEvent) {
        when (event) {
            is DetailEvent.LoadAnime -> loadAnime(event.url)
            is DetailEvent.RateAnime -> rateAnime(event.rating)
            is DetailEvent.ToggleFavorite -> toggleFavorite()
            is DetailEvent.ToggleFavoriteNotification -> toggleFavoriteNotification()
            is DetailEvent.ToggleDescription -> toggleDescription()
            is DetailEvent.LoadMoreComments -> loadMoreComments()
            is DetailEvent.UpdateCommentTextValue -> _uiState.update { it.copy(commentTextValue = event.value) }
            is DetailEvent.ReplyToComment -> replyToComment(event.comment)
            DetailEvent.CancelReply -> cancelReply()
            is DetailEvent.SubmitComment -> submitComment()
            is DetailEvent.ReportComment -> openReport(event.comment)
            is DetailEvent.UpdateReportTextValue -> _uiState.update { it.copy(reportTextValue = event.value) }
            DetailEvent.SubmitReport -> submitReport()
            DetailEvent.DismissReport -> dismissReport()
            is DetailEvent.RequestDeleteComment -> _uiState.update { it.copy(deleteTarget = event.comment) }
            DetailEvent.ConfirmDeleteComment -> deleteComment()
            DetailEvent.DismissDeleteComment -> _uiState.update { it.copy(deleteTarget = null) }
            is DetailEvent.ShowDownloadSheet -> showDownloadSheet(event.episode)
            is DetailEvent.HideDownloadSheet -> hideDownloadSheet()
            is DetailEvent.DownloadWithQuality -> downloadWithQuality(event.source)
            is DetailEvent.SetWatchStatus -> setWatchStatus(event.status)
            is DetailEvent.SelectEpisodeRange -> _uiState.update { it.copy(episodeRangeStart = event.start) }
        }
    }

    private fun loadAnime(url: String) {
        if (_uiState.value.isLoading) return
        observeWatchStatus(url)
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    animeUrl = url,
                    userRating = 0,
                    comments = emptyList(),
                    commentsPage = 1,
                    commentsTotalPages = 1,
                    hasMoreComments = false,
                    commentTextValue = TextFieldValue(),
                    replyTarget = null,
                    replyMarkup = "",
                    reportTarget = null,
                    deleteTarget = null,
                )
            }
            val result = getAnimeDetailUseCase(url)
            result.onSuccess { anime ->
                val isFav = favoriteRepository.isFavorite(anime.id)
                if (isFav) {
                    favoriteRepository.updateMetadata(anime.toFavoritePreview(url))
                }
                _uiState.update {
                    it.copy(
                        anime = anime,
                        isFavorite = isFav,
                        isLoading = false,
                        areFavoriteNotificationsEnabled = favoriteNotificationsEnabled,
                        isFavoriteNotificationMuted = anime.id in mutedFavoriteIds,
                    )
                }
                loadComments()
                viewModelScope.launch {
                    userListRepository.enrichPreview(
                        animeUrl = url,
                        preview = AnimePreview(
                            id = anime.id,
                            title = anime.title,
                            titleOriginal = anime.titleOriginal,
                            posterUrl = anime.posterUrl,
                            episodeInfo = "",
                            url = url,
                        ),
                    )
                }
            }
            result.onError { _, msg ->
                _uiState.update {
                    it.copy(isLoading = false, error = msg ?: "Не удалось загрузить данные")
                }
            }
            // Load watch progress after anime is set (suspend call outside inline lambda)
            val anime = _uiState.value.anime ?: return@launch
            val savedRating = userPreferencesRepository.getUserRating(anime.id)
            _uiState.update { it.copy(userRating = savedRating) }
            val allProgress = watchProgressRepository.getAllProgress(anime.id)
            val watchedIds = allProgress
                .filter { it.isCompleted }
                .map { it.episodeVideoId }
                .toSet()
            val lastProgress = allProgress.maxByOrNull { it.updatedAt }
            val continueEp = if (lastProgress != null && !lastProgress.isCompleted) {
                anime.episodes.find { it.videoId == lastProgress.episodeVideoId }
            } else null
            _uiState.update {
                it.copy(
                    watchedEpisodeIds = watchedIds,
                    continueEpisode = continueEp,
                    continuePositionMs = if (continueEp != null) lastProgress?.positionMs ?: 0L else 0L,
                )
            }
        }
    }

    private fun showDownloadSheet(episode: Episode) {
        _uiState.update { it.copy(isLoadingDownloadSources = true, downloadEpisodePending = episode) }
        viewModelScope.launch {
            getEpisodeDownloadSourcesUseCase(episode.videoId)
                .onSuccess { sources ->
                    _uiState.update { it.copy(downloadSources = sources, isLoadingDownloadSources = false) }
                }
                .onError { _, msg ->
                    _uiState.update { it.copy(isLoadingDownloadSources = false, downloadEpisodePending = null) }
                    _effect.emit(DetailEffect.ShowSnackbar("Ошибка: ${msg ?: "не удалось загрузить источники"}"))
                }
        }
    }

    private fun hideDownloadSheet() {
        _uiState.update {
            it.copy(downloadEpisodePending = null, downloadSources = emptyList(), isLoadingDownloadSources = false)
        }
    }

    private fun downloadWithQuality(source: VideoSource) {
        val episode = _uiState.value.downloadEpisodePending ?: return
        val animeTitle = _uiState.value.anime?.title ?: "Anime"
        val downloadUrl = source.downloadUrl.ifBlank { source.url }
        try {
            episodeDownloadManager.enqueue(
                url = downloadUrl,
                fileName = "$animeTitle - ${episode.name}",
                title = "$animeTitle - ${episode.name}",
            )
            hideDownloadSheet()
            viewModelScope.launch {
                _effect.emit(DetailEffect.ShowSnackbar("Загрузка начата: ${episode.name}"))
            }
        } catch (_: Exception) {
            hideDownloadSheet()
            viewModelScope.launch {
                _effect.emit(DetailEffect.ShowSnackbar("Не удалось начать загрузку"))
            }
        }
    }

    private fun rateAnime(rating: Int) {
        val anime = _uiState.value.anime ?: return
        if (!_uiState.value.isLoggedIn) {
            viewModelScope.launch { _effect.emit(DetailEffect.ShowSnackbar("Войдите в аккаунт чтобы оценить")) }
            return
        }
        if (_uiState.value.isRatingSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRatingSubmitting = true) }
            rateAnimeUseCase(anime.id, rating)
                .onSuccess { newRating ->
                    userPreferencesRepository.setUserRating(anime.id, rating)
                    _uiState.update {
                        it.copy(
                            userRating = rating,
                            isRatingSubmitting = false,
                            anime = it.anime?.copy(rating = newRating),
                        )
                    }
                    _effect.emit(DetailEffect.ShowSnackbar("Оценка сохранена"))
                }
                .onError { _, msg ->
                    _uiState.update { it.copy(isRatingSubmitting = false) }
                    _effect.emit(DetailEffect.ShowSnackbar(msg ?: "Не удалось сохранить оценку"))
                }
        }
    }

    private fun toggleFavorite() {
        val anime = _uiState.value.anime ?: return
        val preview = anime.toFavoritePreview(_uiState.value.animeUrl)
        viewModelScope.launch {
            try {
                val isFav = toggleFavoriteUseCase(anime.id, preview)
                _uiState.update { it.copy(isFavorite = isFav) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _effect.emit(DetailEffect.ShowSnackbar("Не удалось изменить избранное"))
            }
        }
    }

    private fun toggleFavoriteNotification() {
        val state = _uiState.value
        val anime = state.anime ?: return
        if (!state.isFavorite) return
        if (!anime.releaseStatus.supportsEpisodeNotifications) return
        if (!state.areFavoriteNotificationsEnabled) {
            viewModelScope.launch {
                _effect.emit(
                    DetailEffect.ShowSnackbar("Включите уведомления об избранном в настройках"),
                )
            }
            return
        }
        viewModelScope.launch {
            notificationPreferencesRepository.setFavoriteMuted(
                animeId = anime.id,
                muted = !state.isFavoriteNotificationMuted,
            )
            _effect.emit(
                DetailEffect.ShowSnackbar(
                    if (state.isFavoriteNotificationMuted) {
                        "Уведомления для этого аниме включены"
                    } else {
                        "Уведомления для этого аниме отключены"
                    },
                ),
            )
        }
    }

    private fun AnimeDetail.toFavoritePreview(url: String): AnimePreview = AnimePreview(
        id = id,
        title = title,
        titleOriginal = titleOriginal,
        posterUrl = posterUrl,
        episodeInfo = episodeInfo.ifBlank { episodeCount },
        url = url,
        releaseStatus = releaseStatus,
    )

    private fun toggleDescription() {
        _uiState.update { it.copy(isDescriptionExpanded = !it.isDescriptionExpanded) }
    }

    private fun loadComments() {
        val anime = _uiState.value.anime ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingComments = true) }
            getCommentsUseCase(anime.id, 1, anime.url)
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            comments = page.comments,
                            isLoadingComments = false,
                            commentsPage = page.currentPage,
                            commentsTotalPages = page.totalPages,
                            hasMoreComments = page.currentPage < page.totalPages,
                        )
                    }
                }
                .onError { _, _ ->
                    _uiState.update { it.copy(isLoadingComments = false) }
                }
        }
    }

    private fun loadMoreComments() {
        val anime = _uiState.value.anime ?: return
        val state = _uiState.value
        val nextPage = state.commentsPage + 1
        if (state.isLoadingComments || !state.hasMoreComments) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingComments = true) }
            getCommentsUseCase(anime.id, nextPage, anime.url)
                .onSuccess { page ->
                    val totalPages = max(state.commentsTotalPages, page.totalPages)
                    _uiState.update {
                        it.copy(
                            comments = it.comments + page.comments,
                            isLoadingComments = false,
                            commentsPage = page.currentPage,
                            commentsTotalPages = totalPages,
                            hasMoreComments = page.currentPage < totalPages,
                        )
                    }
                }
                .onError { _, _ ->
                    _uiState.update { it.copy(isLoadingComments = false) }
                }
        }
    }

    private fun replyToComment(comment: Comment) {
        if (!_uiState.value.isLoggedIn) {
            viewModelScope.launch { _effect.emit(DetailEffect.ShowSnackbar("Войдите в аккаунт чтобы ответить")) }
            return
        }
        _uiState.update {
            it.copy(
                replyTarget = comment,
                replyMarkup = "",
                isPreparingReply = true,
                commentTextValue = TextFieldValue(),
            )
        }
        viewModelScope.launch {
            getCommentReplyTemplateUseCase(comment.id)
                .onSuccess { markup ->
                    val newText = markup.ensureTrailingLineBreak()
                    _uiState.update {
                        it.copy(
                            replyMarkup = newText,
                            commentTextValue = TextFieldValue(),
                            isPreparingReply = false,
                        )
                    }
                }
                .onError { _, _ ->
                    val fallback = "[quote=${comment.author}]${comment.text.stripHtml().take(200)}[/quote]\n"
                    _uiState.update {
                        it.copy(
                            replyMarkup = fallback,
                            commentTextValue = TextFieldValue(),
                            isPreparingReply = false,
                        )
                    }
                }
        }
    }

    private fun cancelReply() {
        _uiState.update {
            it.copy(
                replyTarget = null,
                replyMarkup = "",
                isPreparingReply = false,
                commentTextValue = TextFieldValue(),
            )
        }
    }

    private fun submitComment() {
        val anime = _uiState.value.anime ?: return
        if (!_uiState.value.isLoggedIn) {
            viewModelScope.launch { _effect.emit(DetailEffect.ShowSnackbar("Войдите в аккаунт чтобы комментировать")) }
            return
        }
        val state = _uiState.value
        val visibleText = state.commentTextValue.text.trim()
        if (visibleText.isBlank()) return
        val text = buildString {
            append(state.replyMarkup)
            if (state.replyMarkup.isNotBlank() && !state.replyMarkup.endsWith('\n')) {
                append('\n')
            }
            append(visibleText)
        }.trim()
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingComment = true) }
            addCommentUseCase(anime.id, text)
                .onSuccess {
                    _uiState.update {
                        val updatedAnime = it.anime?.let { anime ->
                            anime.copy(commentCount = anime.commentCount + 1)
                        }
                        it.copy(
                            commentTextValue = TextFieldValue(),
                            isAddingComment = false,
                            replyTarget = null,
                            replyMarkup = "",
                            anime = updatedAnime,
                        )
                    }
                    loadComments()
                    _effect.emit(DetailEffect.ShowSnackbar("Комментарий добавлен"))
                }
                .onError { _, msg ->
                    _uiState.update { it.copy(isAddingComment = false) }
                    _effect.emit(
                        DetailEffect.ShowSnackbar(
                            "Ошибка: ${msg ?: "не удалось добавить комментарий"}",
                        ),
                    )
                }
        }
    }

    private fun openReport(comment: Comment) {
        if (!_uiState.value.isLoggedIn) {
            viewModelScope.launch { _effect.emit(DetailEffect.ShowSnackbar("Войдите в аккаунт чтобы пожаловаться")) }
            return
        }
        _uiState.update {
            it.copy(
                reportTarget = comment,
                reportTextValue = TextFieldValue(),
            )
        }
    }

    private fun dismissReport() {
        _uiState.update {
            it.copy(
                reportTarget = null,
                reportTextValue = TextFieldValue(),
                isReportingComment = false,
            )
        }
    }

    private fun submitReport() {
        val target = _uiState.value.reportTarget ?: return
        val text = _uiState.value.reportTextValue.text.trim()
        if (text.isBlank() || _uiState.value.isReportingComment) return
        viewModelScope.launch {
            _uiState.update { it.copy(isReportingComment = true) }
            reportCommentUseCase(target.id, text)
                .onSuccess {
                    dismissReport()
                    _effect.emit(DetailEffect.ShowSnackbar("Жалоба отправлена"))
                }
                .onError { _, msg ->
                    _uiState.update { it.copy(isReportingComment = false) }
                    _effect.emit(DetailEffect.ShowSnackbar(msg ?: "Не удалось отправить жалобу"))
                }
        }
    }

    private fun deleteComment() {
        val target = _uiState.value.deleteTarget ?: return
        if (_uiState.value.deletingCommentId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(deletingCommentId = target.id) }
            deleteCommentUseCase(target.id)
                .onSuccess {
                    _uiState.update {
                        val updatedAnime = it.anime?.let { anime ->
                            anime.copy(commentCount = (anime.commentCount - 1).coerceAtLeast(0))
                        }
                        it.copy(
                            comments = it.comments.filterNot { comment -> comment.id == target.id },
                            deleteTarget = null,
                            deletingCommentId = null,
                            anime = updatedAnime,
                        )
                    }
                    _effect.emit(DetailEffect.ShowSnackbar("Комментарий удален"))
                }
                .onError { _, msg ->
                    _uiState.update { it.copy(deletingCommentId = null, deleteTarget = null) }
                    _effect.emit(DetailEffect.ShowSnackbar(msg ?: "Не удалось удалить комментарий"))
                }
        }
    }

    private fun setWatchStatus(status: AnimeStatus?) {
        val url = _uiState.value.animeUrl
        if (url.isBlank()) return
        val anime = _uiState.value.anime
        val preview = anime?.let {
            AnimePreview(
                id = it.id,
                title = it.title,
                titleOriginal = it.titleOriginal,
                posterUrl = it.posterUrl,
                episodeInfo = it.episodeCount,
                url = url,
            )
        }
        viewModelScope.launch {
            try {
                userListRepository.setStatus(url, status, preview)
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                _effect.emit(DetailEffect.ShowSnackbar("Ошибка сохранения статуса"))
            }
        }
    }
}

private fun String.ensureTrailingLineBreak(): String =
    if (endsWith('\n')) this else "$this\n"

private fun String.stripHtml(): String =
    replace(Regex("""<[^>]+>"""), " ")
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
        .replace(Regex("""\s+"""), " ")
        .trim()
