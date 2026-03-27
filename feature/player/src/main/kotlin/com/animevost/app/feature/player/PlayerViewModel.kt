package com.animevost.app.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.model.VideoSource
import com.animevost.app.core.domain.usecase.GetVideoUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val videoSources: List<VideoSource> = emptyList(),
    val selectedQuality: String = "SD (480p)",
    val currentEpisode: Episode? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val allEpisodes: List<Episode> = emptyList(),
    val currentEpisodeIndex: Int = 0,
) {
    val hasPrevious: Boolean get() = currentEpisodeIndex > 0
    val hasNext: Boolean get() = currentEpisodeIndex < allEpisodes.lastIndex
    val currentVideoUrl: String?
        get() = videoSources.firstOrNull { it.quality == selectedQuality }?.url
            ?: videoSources.firstOrNull()?.url
}

sealed interface PlayerEvent {
    data class LoadVideo(
        val episode: Episode,
        val allEpisodes: List<Episode>,
        val currentIndex: Int,
    ) : PlayerEvent
    data class SelectQuality(val quality: String) : PlayerEvent
    data object NextEpisode : PlayerEvent
    data object PreviousEpisode : PlayerEvent
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getVideoUrlUseCase: GetVideoUrlUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.LoadVideo -> loadVideo(event.episode, event.allEpisodes, event.currentIndex)
            is PlayerEvent.SelectQuality -> selectQuality(event.quality)
            is PlayerEvent.NextEpisode -> navigateEpisode(1)
            is PlayerEvent.PreviousEpisode -> navigateEpisode(-1)
        }
    }

    private fun loadVideo(episode: Episode, allEpisodes: List<Episode>, index: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    currentEpisode = episode,
                    allEpisodes = allEpisodes,
                    currentEpisodeIndex = index,
                )
            }
            try {
                val sources = getVideoUrlUseCase(episode.videoId)
                _uiState.update {
                    it.copy(
                        videoSources = sources,
                        isLoading = false,
                        selectedQuality = sources.firstOrNull()?.quality ?: "SD (480p)",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Не удалось загрузить видео",
                    )
                }
            }
        }
    }

    private fun selectQuality(quality: String) {
        _uiState.update { it.copy(selectedQuality = quality) }
    }

    private fun navigateEpisode(direction: Int) {
        val state = _uiState.value
        val newIndex = state.currentEpisodeIndex + direction
        if (newIndex < 0 || newIndex > state.allEpisodes.lastIndex) return
        val episode = state.allEpisodes[newIndex]
        loadVideo(episode, state.allEpisodes, newIndex)
    }
}
