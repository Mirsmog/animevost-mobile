package com.animevost.app.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.model.VideoSource
import com.animevost.app.core.domain.usecase.AddToHistoryUseCase
import com.animevost.app.core.domain.usecase.GetAnimeDetailUseCase
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
    data class SelectQuality(val quality: String) : PlayerEvent
    data object NextEpisode : PlayerEvent
    data object PreviousEpisode : PlayerEvent
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getVideoUrlUseCase: GetVideoUrlUseCase,
    private val getAnimeDetailUseCase: GetAnimeDetailUseCase,
    private val addToHistoryUseCase: AddToHistoryUseCase,
) : ViewModel() {

    private val videoId: String = savedStateHandle["videoId"] ?: ""
    private val episodeName: String = savedStateHandle["episodeName"] ?: ""
    private val animeUrl: String = savedStateHandle["animeUrl"] ?: ""

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var animePreview: AnimePreview? = null

    init {
        val episode = Episode(name = episodeName, videoId = videoId, thumbnailUrl = "")
        loadVideo(episode, emptyList(), 0)
        loadEpisodeList()
    }

    fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.SelectQuality -> selectQuality(event.quality)
            is PlayerEvent.NextEpisode -> navigateEpisode(1)
            is PlayerEvent.PreviousEpisode -> navigateEpisode(-1)
        }
    }

    private fun loadEpisodeList() {
        if (animeUrl.isBlank()) return
        viewModelScope.launch {
            try {
                val detail = getAnimeDetailUseCase(animeUrl)
                val episodes = detail.episodes
                val currentIdx = episodes.indexOfFirst { it.videoId == videoId }
                animePreview = AnimePreview(
                    id = detail.id,
                    title = detail.title,
                    titleOriginal = detail.titleOriginal,
                    posterUrl = detail.posterUrl,
                    episodeInfo = detail.episodeCount,
                    url = animeUrl,
                )
                _uiState.update {
                    it.copy(
                        allEpisodes = episodes,
                        currentEpisodeIndex = if (currentIdx >= 0) currentIdx else 0,
                    )
                }
                // Record current episode in history after detail loaded
                val currentEp = _uiState.value.currentEpisode
                val preview = animePreview
                if (currentEp != null && preview != null) {
                    addToHistoryUseCase(preview, currentEp)
                }
            } catch (_: Exception) {
                // prev/next navigation just won't work
            }
        }
    }

    private fun loadVideo(episode: Episode, allEpisodes: List<Episode>, index: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    currentEpisode = episode,
                    allEpisodes = if (allEpisodes.isNotEmpty()) allEpisodes else it.allEpisodes,
                    currentEpisodeIndex = if (allEpisodes.isNotEmpty()) index else it.currentEpisodeIndex,
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
                // Record in history when video loads successfully
                val preview = animePreview
                if (preview != null) {
                    addToHistoryUseCase(preview, episode)
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
