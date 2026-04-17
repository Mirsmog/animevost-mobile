package com.animevost.app.feature.player

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.model.SkipInterval
import com.animevost.app.core.domain.model.VideoSource
import com.animevost.app.core.domain.usecase.AddToHistoryUseCase
import com.animevost.app.core.domain.usecase.GetAnimeDetailUseCase
import com.animevost.app.core.domain.usecase.GetVideoUrlUseCase
import com.animevost.app.core.domain.model.WatchProgress
import com.animevost.app.core.domain.repository.SkipTimesRepository
import com.animevost.app.core.domain.repository.WatchProgressRepository
import com.animevost.app.core.domain.util.onError
import com.animevost.app.core.domain.util.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    /** Position to seek to when the video finishes loading (cleared after use). */
    val resumePositionMs: Long = 0L,
) {
    val hasPrevious: Boolean get() = currentEpisodeIndex > 0
    val hasNext: Boolean get() = currentEpisodeIndex < allEpisodes.lastIndex
    val currentVideoUrl: String?
        get() = videoSources.firstOrNull { it.quality == selectedQuality }?.url
            ?: videoSources.firstOrNull()?.url
}

sealed interface PlayerEvent {
    data class SelectQuality(val quality: String) : PlayerEvent
    data class NextEpisode(val currentPositionMs: Long = 0L, val currentDurationMs: Long = 0L) : PlayerEvent
    data class PreviousEpisode(val currentPositionMs: Long = 0L, val currentDurationMs: Long = 0L) : PlayerEvent
    /** Called periodically from PlayerScreen to persist playback position. */
    data class UpdateProgress(val positionMs: Long, val durationMs: Long) : PlayerEvent
    /** Signal that resume position has been consumed (seek done). */
    data object ResumeConsumed : PlayerEvent
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getVideoUrlUseCase: GetVideoUrlUseCase,
    private val getAnimeDetailUseCase: GetAnimeDetailUseCase,
    private val addToHistoryUseCase: AddToHistoryUseCase,
    private val dataStore: DataStore<Preferences>,
    private val watchProgressRepository: WatchProgressRepository,
    private val skipTimesRepository: SkipTimesRepository,
) : ViewModel() {

    private companion object {
        val KEY_QUALITY = stringPreferencesKey("preferred_quality")
    }

    private val videoId: String = savedStateHandle["videoId"] ?: ""
    private val episodeName: String = savedStateHandle["episodeName"] ?: ""
    private val animeUrl: String = savedStateHandle["animeUrl"] ?: ""

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _activeSkip = MutableStateFlow<SkipInterval?>(null)
    val activeSkip: StateFlow<SkipInterval?> = _activeSkip.asStateFlow()

    private val _skipIntervals = MutableStateFlow<List<SkipInterval>>(emptyList())
    val skipIntervals: StateFlow<List<SkipInterval>> = _skipIntervals.asStateFlow()

    private var animePreview: AnimePreview? = null
    private var currentSkipIntervals: List<SkipInterval> = emptyList()
    private var titleOriginal: String = ""
    private var titleAlternative: String = ""

    init {
        val episode = Episode(name = episodeName, videoId = videoId, thumbnailUrl = "")
        loadVideo(episode, emptyList(), 0)
        loadEpisodeList()
    }

    fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.SelectQuality -> selectQuality(event.quality)
            is PlayerEvent.NextEpisode -> {
                saveCurrentProgress(event.currentPositionMs, event.currentDurationMs)
                navigateEpisode(1)
            }
            is PlayerEvent.PreviousEpisode -> {
                saveCurrentProgress(event.currentPositionMs, event.currentDurationMs)
                navigateEpisode(-1)
            }
            is PlayerEvent.UpdateProgress -> saveCurrentProgress(event.positionMs, event.durationMs)
            is PlayerEvent.ResumeConsumed -> _uiState.update { it.copy(resumePositionMs = 0L) }
        }
    }

    private fun saveCurrentProgress(positionMs: Long, durationMs: Long) {
        val episode = _uiState.value.currentEpisode ?: return
        val animeId = animePreview?.id ?: return
        if (positionMs <= 0L) return
        viewModelScope.launch {
            watchProgressRepository.saveProgress(
                WatchProgress(
                    animeId = animeId,
                    episodeVideoId = episode.videoId,
                    episodeName = episode.name,
                    episodeIndex = _uiState.value.currentEpisodeIndex,
                    positionMs = positionMs,
                    durationMs = durationMs,
                ),
            )
        }
    }

    private fun loadEpisodeList() {
        if (animeUrl.isBlank()) return
        viewModelScope.launch {
            val result = getAnimeDetailUseCase(animeUrl)
            result.onSuccess { detail ->
                val episodes = detail.episodes
                val currentIdx = episodes.indexOfFirst { it.videoId == videoId }
                titleOriginal = detail.titleOriginal
                titleAlternative = detail.titleAlternative
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
                val currentEp = _uiState.value.currentEpisode
                val preview = animePreview
                if (currentEp != null && preview != null) {
                    addToHistoryUseCase(preview, currentEp)
                }
                loadSkipTimesForEpisode(detail.id, episodeName)
            }
            if (result is com.animevost.app.core.domain.util.Result.Success) {
                val detail = result.data
                val savedProgress = watchProgressRepository.getProgress(detail.id, videoId)
                if (savedProgress != null && savedProgress.positionMs > 0L && !savedProgress.isCompleted) {
                    _uiState.update { it.copy(resumePositionMs = savedProgress.positionMs) }
                }
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
            getVideoUrlUseCase(episode.videoId)
                .onSuccess { sources ->
                    val preferred = dataStore.data.map { it[KEY_QUALITY] }.first()
                    val quality = if (preferred != null && sources.any { it.quality == preferred }) {
                        preferred
                    } else {
                        sources.firstOrNull()?.quality ?: "SD (480p)"
                    }
                    _uiState.update {
                        it.copy(videoSources = sources, isLoading = false, selectedQuality = quality)
                    }
                    val preview = animePreview
                    if (preview != null) addToHistoryUseCase(preview, episode)
                }
                .onError { _, msg ->
                    _uiState.update {
                        it.copy(isLoading = false, error = msg ?: "Не удалось загрузить видео")
                    }
                }
        }
    }

    private fun selectQuality(quality: String) {
        _uiState.update { it.copy(selectedQuality = quality) }
        viewModelScope.launch {
            dataStore.edit { it[KEY_QUALITY] = quality }
        }
    }

    private fun navigateEpisode(direction: Int) {
        val state = _uiState.value
        val newIndex = state.currentEpisodeIndex + direction
        if (newIndex < 0 || newIndex > state.allEpisodes.lastIndex) return
        val episode = state.allEpisodes[newIndex]
        _activeSkip.value = null
        currentSkipIntervals = emptyList()
        _skipIntervals.value = emptyList()
        loadVideo(episode, state.allEpisodes, newIndex)
        animePreview?.let { preview ->
            loadSkipTimesForEpisode(preview.id, episode.name)
        }
    }

    fun checkSkipPosition(positionMs: Long) {
        val active = currentSkipIntervals.firstOrNull { positionMs in it.startMs..it.endMs }
        if (_activeSkip.value != active) _activeSkip.value = active
    }

    private fun loadSkipTimesForEpisode(animeId: Int, epName: String) {
        val episodeNum = Regex("\\d+").find(epName)?.value?.toIntOrNull() ?: 1
        viewModelScope.launch {
            currentSkipIntervals = try {
                skipTimesRepository.getSkipTimes(
                    animeId = animeId,
                    episodeNumber = episodeNum,
                    titleOriginal = titleOriginal,
                    titleAlternative = titleAlternative,
                )
            } catch (_: Exception) {
                emptyList()
            }
            _skipIntervals.value = currentSkipIntervals
        }
    }
}
