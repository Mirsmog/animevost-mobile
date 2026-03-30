package com.animevost.app.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.data.download.EpisodeDownloader
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.usecase.AddCommentUseCase
import com.animevost.app.core.domain.usecase.DownloadEpisodeUseCase
import com.animevost.app.core.domain.usecase.GetAnimeDetailUseCase
import com.animevost.app.core.domain.usecase.GetCommentsUseCase
import com.animevost.app.core.domain.usecase.RateAnimeUseCase
import com.animevost.app.core.domain.usecase.ToggleFavoriteUseCase
import com.animevost.app.core.domain.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val anime: com.animevost.app.core.domain.model.AnimeDetail? = null,
    val animeUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val userRating: Int = 0,
    val isDescriptionExpanded: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val commentText: String = "",
    val isAddingComment: Boolean = false,
    val commentsPage: Int = 1,
    val hasMoreComments: Boolean = false,
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
    data class UpdateCommentText(val text: String) : DetailEvent
    data object SubmitComment : DetailEvent
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<DetailEffect>()
    val effect: SharedFlow<DetailEffect> = _effect.asSharedFlow()

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
            is DetailEvent.UpdateCommentText -> updateCommentText(event.text)
            is DetailEvent.SubmitComment -> submitComment()
        }
    }

    private fun loadAnime(url: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, animeUrl = url) }
            try {
                val anime = getAnimeDetailUseCase(url)
                val isFav = favoriteRepository.isFavorite(anime.id)
                _uiState.update {
                    it.copy(
                        anime = anime,
                        isFavorite = isFav,
                        isLoading = false,
                    )
                }
                loadComments()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Не удалось загрузить данные",
                    )
                }
            }
        }
    }

    private fun downloadEpisode(episode: Episode) {
        val animeTitle = _uiState.value.anime?.title ?: "Anime"
        viewModelScope.launch {
            try {
                val sources = downloadEpisodeUseCase(episode.videoId)
                val source = sources.firstOrNull()
                if (source != null) {
                    val downloadUrl = source.downloadUrl.ifBlank { source.url }
                    val fileName = "$animeTitle - ${episode.name}"
                    episodeDownloader.download(
                        url = downloadUrl,
                        fileName = fileName,
                        title = "$animeTitle — ${episode.name}",
                    )
                    _effect.emit(DetailEffect.ShowSnackbar("Загрузка начата: ${episode.name}"))
                } else {
                    _effect.emit(DetailEffect.ShowSnackbar("Нет доступных источников"))
                }
            } catch (e: Exception) {
                _effect.emit(
                    DetailEffect.ShowSnackbar(
                        "Ошибка загрузки: ${e.message ?: "неизвестная ошибка"}",
                    ),
                )
            }
        }
    }

    private fun rateAnime(rating: Int) {
        val anime = _uiState.value.anime ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(userRating = rating) }
            try {
                val newRating = rateAnimeUseCase(anime.id, rating)
                _uiState.update { it.copy(anime = it.anime?.copy(rating = newRating)) }
            } catch (_: Exception) { }
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
            try {
                val comments = getCommentsUseCase(anime.id, 1, anime.url)
                _uiState.update {
                    it.copy(
                        comments = comments,
                        isLoadingComments = false,
                        commentsPage = 1,
                        hasMoreComments = comments.size >= 10,
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingComments = false) }
            }
        }
    }

    private fun loadMoreComments() {
        val anime = _uiState.value.anime ?: return
        val state = _uiState.value
        val nextPage = state.commentsPage + 1
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingComments = true) }
            try {
                val moreComments = getCommentsUseCase(anime.id, nextPage, anime.url)
                _uiState.update {
                    it.copy(
                        comments = it.comments + moreComments,
                        isLoadingComments = false,
                        commentsPage = nextPage,
                        hasMoreComments = moreComments.size >= 10,
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingComments = false) }
            }
        }
    }

    private fun updateCommentText(text: String) {
        _uiState.update { it.copy(commentText = text) }
    }

    private fun submitComment() {
        val anime = _uiState.value.anime ?: return
        val text = _uiState.value.commentText.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingComment = true) }
            try {
                val comment = addCommentUseCase(anime.id, text)
                _uiState.update {
                    it.copy(
                        comments = listOf(comment) + it.comments,
                        commentText = "",
                        isAddingComment = false,
                    )
                }
                _effect.emit(DetailEffect.ShowSnackbar("Комментарий добавлен"))
            } catch (e: Exception) {
                _uiState.update { it.copy(isAddingComment = false) }
                _effect.emit(
                    DetailEffect.ShowSnackbar(
                        "Ошибка: ${e.message ?: "не удалось добавить комментарий"}",
                    ),
                )
            }
        }
    }
}
