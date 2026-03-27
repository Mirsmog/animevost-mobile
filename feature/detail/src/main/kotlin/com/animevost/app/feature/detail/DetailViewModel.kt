package com.animevost.app.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.data.download.EpisodeDownloader
import com.animevost.app.core.domain.model.AnimeDetail
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.usecase.DownloadEpisodeUseCase
import com.animevost.app.core.domain.usecase.GetAnimeDetailUseCase
import com.animevost.app.core.domain.usecase.ToggleFavoriteUseCase
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
    val anime: AnimeDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val userRating: Int = 0,
    val isDescriptionExpanded: Boolean = false,
)

sealed interface DetailEvent {
    data class LoadAnime(val url: String) : DetailEvent
    data class RateAnime(val rating: Int) : DetailEvent
    data object ToggleFavorite : DetailEvent
    data class PlayEpisode(val episode: Episode, val allEpisodes: List<Episode>, val index: Int) : DetailEvent
    data class DownloadEpisode(val episode: Episode) : DetailEvent
    data object ToggleDescription : DetailEvent
}

sealed interface DetailEffect {
    data class ShowSnackbar(val message: String) : DetailEffect
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getAnimeDetailUseCase: GetAnimeDetailUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val downloadEpisodeUseCase: DownloadEpisodeUseCase,
    private val episodeDownloader: EpisodeDownloader,
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
        }
    }

    private fun loadAnime(url: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val anime = getAnimeDetailUseCase(url)
                _uiState.update {
                    it.copy(
                        anime = anime,
                        isLoading = false,
                    )
                }
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
        _uiState.update { it.copy(userRating = rating) }
    }

    private fun toggleFavorite() {
        val anime = _uiState.value.anime ?: return
        viewModelScope.launch {
            try {
                val isFav = toggleFavoriteUseCase(anime.id)
                _uiState.update { it.copy(isFavorite = isFav) }
            } catch (_: Exception) { }
        }
    }

    private fun toggleDescription() {
        _uiState.update { it.copy(isDescriptionExpanded = !it.isDescriptionExpanded) }
    }
}
