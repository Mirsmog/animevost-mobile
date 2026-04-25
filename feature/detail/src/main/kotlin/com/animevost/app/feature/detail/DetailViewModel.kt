package com.animevost.app.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.animevost.app.core.data.download.EpisodeDownloader
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeStatus
import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.model.VideoSource
import com.animevost.app.core.domain.usecase.AddCommentUseCase
import com.animevost.app.core.domain.usecase.DownloadEpisodeUseCase
import com.animevost.app.core.domain.usecase.GetAnimeDetailUseCase
import com.animevost.app.core.domain.usecase.GetCommentsUseCase
import com.animevost.app.core.domain.usecase.RateAnimeUseCase
import com.animevost.app.core.domain.usecase.ToggleFavoriteUseCase
import com.animevost.app.core.domain.model.BetaFeature
import com.animevost.app.core.domain.repository.FeatureFlagsRepository
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.domain.repository.FavoriteRepository
import com.animevost.app.core.domain.model.WatchProgress
import com.animevost.app.core.domain.repository.WatchProgressRepository
import com.animevost.app.core.domain.repository.UserListRepository
import com.animevost.app.core.domain.util.onSuccess
import com.animevost.app.core.domain.util.onError
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val anime: com.animevost.app.core.domain.model.AnimeDetail? = null,
    val animeUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userRating: Int = 0,
    val isDescriptionExpanded: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val commentTextValue: TextFieldValue = TextFieldValue(),
    val isAddingComment: Boolean = false,
    val commentsPage: Int = 1,
    val hasMoreComments: Boolean = false,
    // Download quality sheet
    val downloadEpisodePending: Episode? = null,
    val downloadSources: List<VideoSource> = emptyList(),
    val isLoadingDownloadSources: Boolean = false,
    // Watch progress
    val watchedEpisodeIds: Set<String> = emptySet(),
    val continueEpisode: Episode? = null,
    val continuePositionMs: Long = 0L,
    // Watch status (user list)
    val watchStatus: AnimeStatus? = null,
    val watchStatusEnabled: Boolean = false,
    // Episode pagination
    val episodeRangeStart: Int = 0,
)

sealed interface DetailEvent {
    data class LoadAnime(val url: String) : DetailEvent
    data class RateAnime(val rating: Int) : DetailEvent
    data object ToggleFavorite : DetailEvent
    data class PlayEpisode(val episode: Episode, val allEpisodes: List<Episode>, val index: Int) : DetailEvent
    data class DownloadEpisode(val episode: Episode) : DetailEvent
    data object ToggleDescription : DetailEvent
    data object LoadComments : DetailEvent
    data object LoadMoreComments : DetailEvent
    data class UpdateCommentTextValue(val value: TextFieldValue) : DetailEvent
    data class ReplyToComment(val comment: Comment) : DetailEvent
    data object SubmitComment : DetailEvent
    // Download quality sheet
    data class ShowDownloadSheet(val episode: Episode) : DetailEvent
    data object HideDownloadSheet : DetailEvent
    data class DownloadWithQuality(val source: VideoSource) : DetailEvent
    // Watch status
    data class SetWatchStatus(val status: AnimeStatus?) : DetailEvent
    // Episode pagination
    data class SelectEpisodeRange(val start: Int) : DetailEvent
}

sealed interface DetailEffect {
    data class ShowSnackbar(val message: String) : DetailEffect
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getAnimeDetailUseCase: GetAnimeDetailUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val favoriteRepository: FavoriteRepository,
    private val downloadEpisodeUseCase: DownloadEpisodeUseCase,
    private val episodeDownloader: EpisodeDownloader,
    private val rateAnimeUseCase: RateAnimeUseCase,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val authRepository: AuthRepository,
    private val watchProgressRepository: WatchProgressRepository,
    private val userListRepository: UserListRepository,
    private val featureFlagsRepository: FeatureFlagsRepository,
) : ViewModel() {

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
            is DetailEvent.PlayEpisode -> { /* handled by navigation in UI */ }
            is DetailEvent.DownloadEpisode -> downloadEpisode(event.episode)
            is DetailEvent.ToggleDescription -> toggleDescription()
            is DetailEvent.LoadComments -> loadComments()
            is DetailEvent.LoadMoreComments -> loadMoreComments()
            is DetailEvent.UpdateCommentTextValue -> _uiState.update { it.copy(commentTextValue = event.value) }
            is DetailEvent.ReplyToComment -> replyToComment(event.comment)
            is DetailEvent.SubmitComment -> submitComment()
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
            _uiState.update { it.copy(isLoading = true, error = null, animeUrl = url) }
            val result = getAnimeDetailUseCase(url)
            result.onSuccess { anime ->
                val isFav = favoriteRepository.isFavorite(anime.id)
                _uiState.update { it.copy(anime = anime, isFavorite = isFav, isLoading = false) }
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

    private fun downloadEpisode(episode: Episode) {
        val animeTitle = _uiState.value.anime?.title ?: "Anime"
        viewModelScope.launch {
            downloadEpisodeUseCase(episode.videoId)
                .onSuccess { sources ->
                    val source = sources.firstOrNull()
                    if (source != null) {
                        val downloadUrl = source.downloadUrl.ifBlank { source.url }
                        episodeDownloader.download(
                            url = downloadUrl,
                            fileName = "$animeTitle - ${episode.name}",
                            title = "$animeTitle — ${episode.name}",
                        )
                        _effect.emit(DetailEffect.ShowSnackbar("Загрузка начата: ${episode.name}"))
                    } else {
                        _effect.emit(DetailEffect.ShowSnackbar("Нет доступных источников"))
                    }
                }
                .onError { _, msg ->
                    _effect.emit(
                        DetailEffect.ShowSnackbar(
                            "Ошибка загрузки: ${msg ?: "неизвестная ошибка"}",
                        ),
                    )
                }
        }
    }

    private fun showDownloadSheet(episode: Episode) {
        _uiState.update { it.copy(isLoadingDownloadSources = true, downloadEpisodePending = episode) }
        viewModelScope.launch {
            downloadEpisodeUseCase(episode.videoId)
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
        episodeDownloader.download(
            url = downloadUrl,
            fileName = "$animeTitle - ${episode.name}",
            title = "$animeTitle — ${episode.name}",
        )
        hideDownloadSheet()
        viewModelScope.launch {
            _effect.emit(DetailEffect.ShowSnackbar("Загрузка начата: ${episode.name}"))
        }
    }

    private fun rateAnime(rating: Int) {
        val anime = _uiState.value.anime ?: return
        if (!_uiState.value.isLoggedIn) {
            viewModelScope.launch { _effect.emit(DetailEffect.ShowSnackbar("Войдите в аккаунт чтобы оценить")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(userRating = rating) }
            rateAnimeUseCase(anime.id, rating)
                .onSuccess { newRating ->
                    _uiState.update { it.copy(anime = it.anime?.copy(rating = newRating)) }
                }
        }
    }

    private fun toggleFavorite() {
        val anime = _uiState.value.anime ?: return
        val preview = AnimePreview(
            id = anime.id,
            title = anime.title,
            titleOriginal = anime.titleOriginal,
            posterUrl = anime.posterUrl,
            episodeInfo = anime.episodeCount,
            url = _uiState.value.animeUrl,
        )
        viewModelScope.launch {
            try {
                val isFav = toggleFavoriteUseCase(anime.id, preview)
                _uiState.update { it.copy(isFavorite = isFav) }
            } catch (_: Exception) { }
        }
    }

    private fun toggleDescription() {
        _uiState.update { it.copy(isDescriptionExpanded = !it.isDescriptionExpanded) }
    }

    private fun loadComments() {
        val anime = _uiState.value.anime ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingComments = true) }
            getCommentsUseCase(anime.id, 1, anime.url)
                .onSuccess { comments ->
                    _uiState.update {
                        it.copy(
                            comments = comments,
                            isLoadingComments = false,
                            commentsPage = 1,
                            hasMoreComments = anime.totalCommentPages > 1,
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
        if (nextPage > anime.totalCommentPages) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingComments = true) }
            getCommentsUseCase(anime.id, nextPage, anime.url)
                .onSuccess { moreComments ->
                    _uiState.update {
                        it.copy(
                            comments = it.comments + moreComments,
                            isLoadingComments = false,
                            commentsPage = nextPage,
                            hasMoreComments = nextPage < anime.totalCommentPages,
                        )
                    }
                }
                .onError { _, _ ->
                    _uiState.update { it.copy(isLoadingComments = false) }
                }
        }
    }

    private fun replyToComment(comment: Comment) {
        val quote = "[quote=${comment.author}]${comment.text.take(200)}[/quote]\n"
        val newText = quote + _uiState.value.commentTextValue.text
        _uiState.update { it.copy(
            commentTextValue = TextFieldValue(newText, TextRange(newText.length)),
        )}
    }

    private fun submitComment() {
        val anime = _uiState.value.anime ?: return
        if (!_uiState.value.isLoggedIn) {
            viewModelScope.launch { _effect.emit(DetailEffect.ShowSnackbar("Войдите в аккаунт чтобы комментировать")) }
            return
        }
        val text = _uiState.value.commentTextValue.text.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingComment = true) }
            addCommentUseCase(anime.id, text)
                .onSuccess { comment ->
                    _uiState.update {
                        it.copy(
                            comments = listOf(comment) + it.comments,
                            commentTextValue = TextFieldValue(),
                            isAddingComment = false,
                        )
                    }
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
            } catch (e: Exception) {
                _effect.emit(DetailEffect.ShowSnackbar("Ошибка сохранения статуса"))
            }
        }
    }
}
