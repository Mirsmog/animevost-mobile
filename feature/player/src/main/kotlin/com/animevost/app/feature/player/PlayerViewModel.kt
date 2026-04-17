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
import com.animevost.app.core.domain.model.SegmentType
import com.animevost.app.core.domain.model.SkipSegment
import com.animevost.app.core.domain.model.VideoSource
import com.animevost.app.core.domain.model.WatchProgress
import com.animevost.app.core.domain.repository.SkipSegmentRepository
import com.animevost.app.core.domain.repository.WatchProgressRepository
import com.animevost.app.core.domain.usecase.AddToHistoryUseCase
import com.animevost.app.core.domain.usecase.GetAnimeDetailUseCase
import com.animevost.app.core.domain.usecase.GetVideoUrlUseCase
import com.animevost.app.core.domain.util.onError
import com.animevost.app.core.domain.util.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    val skipSegments: List<SkipSegment> = emptyList(),
    val editingSegmentType: SegmentType? = null,
    val pendingStartMs: Long? = null,
) {
    val hasPrevious: Boolean get() = currentEpisodeIndex > 0
    val hasNext: Boolean get() = currentEpisodeIndex < allEpisodes.lastIndex
    val currentVideoUrl: String?
        get() = videoSources.firstOrNull { it.quality == selectedQuality }?.url
            ?: videoSources.firstOrNull()?.url
}

data class SkipSnackbar(val message: String, val undoPositionMs: Long)

sealed interface PlayerEvent {
    data class SelectQuality(val quality: String) : PlayerEvent
    data class NextEpisode(val currentPositionMs: Long = 0L, val currentDurationMs: Long = 0L) : PlayerEvent
    data class PreviousEpisode(val currentPositionMs: Long = 0L, val currentDurationMs: Long = 0L) : PlayerEvent
    /** Called periodically from PlayerScreen to persist playback position. */
    data class UpdateProgress(val positionMs: Long, val durationMs: Long) : PlayerEvent
    /** Signal that resume position has been consumed (seek done). */
    data object ResumeConsumed : PlayerEvent
    data class BeginEditSegment(val type: SegmentType) : PlayerEvent
    data class SaveSkipStart(val type: SegmentType, val positionMs: Long) : PlayerEvent
    data class SaveSkipEnd(val type: SegmentType, val positionMs: Long) : PlayerEvent
    data class DeleteSkip(val type: SegmentType) : PlayerEvent
    data object DismissSkipSnackbar : PlayerEvent
    data class UndoSkip(val positionMs: Long) : PlayerEvent
    data object CancelEditSegment : PlayerEvent
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getVideoUrlUseCase: GetVideoUrlUseCase,
    private val getAnimeDetailUseCase: GetAnimeDetailUseCase,
    private val addToHistoryUseCase: AddToHistoryUseCase,
    private val dataStore: DataStore<Preferences>,
    private val watchProgressRepository: WatchProgressRepository,
    private val skipSegmentRepository: SkipSegmentRepository,
) : ViewModel() {

    private companion object {
        val KEY_QUALITY = stringPreferencesKey("preferred_quality")
    }

    private val videoId: String = savedStateHandle["videoId"] ?: ""
    private val episodeName: String = savedStateHandle["episodeName"] ?: ""
    private val animeUrl: String = savedStateHandle["animeUrl"] ?: ""

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _skipSnackbar = MutableStateFlow<SkipSnackbar?>(null)
    val skipSnackbar: StateFlow<SkipSnackbar?> = _skipSnackbar.asStateFlow()

    private var animePreview: AnimePreview? = null
    private var skipDetector: SkipDetector? = null

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
            is PlayerEvent.BeginEditSegment -> _uiState.update {
                it.copy(editingSegmentType = event.type, pendingStartMs = null)
            }
            is PlayerEvent.SaveSkipStart -> captureSkipStart(event.type, event.positionMs)
            is PlayerEvent.SaveSkipEnd -> captureSkipEnd(event.type, event.positionMs)
            is PlayerEvent.DeleteSkip -> deleteSkipSegment(event.type)
            is PlayerEvent.DismissSkipSnackbar -> _skipSnackbar.value = null
            is PlayerEvent.UndoSkip -> _skipSnackbar.value = null
            is PlayerEvent.CancelEditSegment -> _uiState.update {
                it.copy(editingSegmentType = null, pendingStartMs = null)
            }
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
            }
            if (result is com.animevost.app.core.domain.util.Result.Success) {
                val detail = result.data
                val savedProgress = watchProgressRepository.getProgress(detail.id, videoId)
                if (savedProgress != null && savedProgress.positionMs > 0L && !savedProgress.isCompleted) {
                    _uiState.update { it.copy(resumePositionMs = savedProgress.positionMs) }
                }
                loadSkipSegments(detail.id)
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
        skipDetector?.resetSkipped()
        loadVideo(episode, state.allEpisodes, newIndex)
    }

    // ── Skip segment support ─────────────────────────────────────

    private fun loadSkipSegments(animeId: Int) {
        viewModelScope.launch {
            val segments = skipSegmentRepository.getSegments(animeId)
            _uiState.update { it.copy(skipSegments = segments) }
        }
    }

    fun startSkipDetection(exoPlayer: androidx.media3.exoplayer.ExoPlayer) {
        val segments = _uiState.value.skipSegments
        if (segments.isEmpty()) return
        skipDetector?.stop()
        val detector = SkipDetector(exoPlayer)
        detector.onSegmentDetected = { type, seekToMs ->
            val prevPosition = exoPlayer.currentPosition
            exoPlayer.seekTo(seekToMs)
            val label = if (type == SegmentType.INTRO) "Интро" else "Аутро"
            _skipSnackbar.value = SkipSnackbar("$label пропущено", prevPosition)
            viewModelScope.launch {
                delay(3_000)
                if (_skipSnackbar.value?.undoPositionMs == prevPosition) {
                    _skipSnackbar.value = null
                }
            }
        }
        skipDetector = detector
        detector.start(segments, viewModelScope)
    }

    fun restartSkipDetection(exoPlayer: androidx.media3.exoplayer.ExoPlayer) {
        startSkipDetection(exoPlayer)
    }

    private fun captureSkipStart(type: SegmentType, positionMs: Long) {
        _uiState.update { it.copy(editingSegmentType = type, pendingStartMs = positionMs) }
    }

    private fun captureSkipEnd(type: SegmentType, positionMs: Long) {
        val startMs = _uiState.value.pendingStartMs ?: return
        val animeId = animePreview?.id ?: return
        val durationMs = positionMs - startMs
        if (durationMs <= 0) return
        viewModelScope.launch {
            val segment = SkipSegment(
                animeId = animeId,
                type = type,
                startMs = startMs,
                durationMs = durationMs,
            )
            skipSegmentRepository.saveSegment(segment)
            _uiState.update { it.copy(editingSegmentType = null, pendingStartMs = null) }
            loadSkipSegments(animeId)
        }
    }

    private fun deleteSkipSegment(type: SegmentType) {
        val animeId = animePreview?.id ?: return
        viewModelScope.launch {
            skipSegmentRepository.deleteSegment(animeId, type)
            loadSkipSegments(animeId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        skipDetector?.stop()
    }
}
