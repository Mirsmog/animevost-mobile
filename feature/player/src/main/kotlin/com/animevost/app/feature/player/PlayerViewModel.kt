package com.animevost.app.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.BetaFeature
import com.animevost.app.core.domain.model.CommentScope
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.model.SkipInterval
import com.animevost.app.core.domain.usecase.AddToHistoryUseCase
import com.animevost.app.core.domain.usecase.GetAnimeDetailUseCase
import com.animevost.app.core.domain.usecase.GetCommentsUseCase
import com.animevost.app.core.domain.usecase.GetVideoUrlUseCase
import com.animevost.app.core.domain.model.WatchProgress
import com.animevost.app.core.domain.repository.FeatureFlagsRepository
import com.animevost.app.core.domain.repository.LocalSkipDetector
import com.animevost.app.core.domain.repository.LocalSkipEvent
import com.animevost.app.core.domain.repository.LocalSkipRequest
import com.animevost.app.core.domain.repository.SkipTimesRepository
import com.animevost.app.core.domain.repository.UserPreferencesRepository
import com.animevost.app.core.domain.repository.WatchProgressRepository
import com.animevost.app.core.domain.util.onError
import com.animevost.app.core.domain.util.onSuccess
import com.animevost.app.core.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getVideoUrlUseCase: GetVideoUrlUseCase,
    private val getAnimeDetailUseCase: GetAnimeDetailUseCase,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val addToHistoryUseCase: AddToHistoryUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val watchProgressRepository: WatchProgressRepository,
    private val skipTimesRepository: SkipTimesRepository,
    private val localSkipDetector: LocalSkipDetector,
    private val featureFlagsRepository: FeatureFlagsRepository,
) : ViewModel() {

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
    private var localSkipIntervals: List<SkipInterval> = emptyList()
    private var allohaSkipIntervals: List<SkipInterval> = emptyList()
    private var activeSkipAnimeId: Int? = null
    private var activeSkipEpisodeNumber: Int? = null
    private var title: String = ""
    private var titleOriginal: String = ""
    private var titleAlternative: String = ""
    private var animeYear: Int? = null
    private var skipTimesJob: Job? = null
    private var videoLoadJob: Job? = null
    private var resumeLoadJob: Job? = null
    private var episodeCommentsJob: Job? = null
    private var episodeCommentsScannedPage = 0
    private var episodeCommentsTotalPages = 1

    init {
        val episode = Episode(name = episodeName, videoId = videoId, thumbnailUrl = "")
        loadVideo(episode, emptyList(), 0)
        loadEpisodeList()
        viewModelScope.launch {
            localSkipDetector.events.collect { event ->
                if (
                    event.animeId != activeSkipAnimeId ||
                    event.episodeNumber != activeSkipEpisodeNumber
                ) {
                    return@collect
                }
                when (event) {
                    is LocalSkipEvent.IntervalFound -> {
                        if (allohaSkipIntervals.isNotEmpty()) return@collect
                        localSkipIntervals = localSkipIntervals
                            .filterNot { it.type == event.interval.type } + event.interval
                        publishSkipIntervals()
                    }
                }
            }
        }
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
            PlayerEvent.ShowEpisodeComments -> showEpisodeComments()
            PlayerEvent.HideEpisodeComments -> hideEpisodeComments()
            PlayerEvent.LoadMoreEpisodeComments -> loadEpisodeComments()
            PlayerEvent.RetryEpisodeComments -> loadEpisodeComments()
            is PlayerEvent.ResumeConsumed -> _uiState.update { it.copy(resumePositionMs = 0L) }
        }
    }

    private fun saveCurrentProgress(positionMs: Long, durationMs: Long) {
        val episode = _uiState.value.currentEpisode ?: return
        val animeId = animePreview?.id ?: return
        val episodeIndex = _uiState.value.currentEpisodeIndex
        if (positionMs <= 0L) return
        viewModelScope.launch {
            watchProgressRepository.saveProgress(
                WatchProgress(
                    animeId = animeId,
                    episodeVideoId = episode.videoId,
                    episodeName = episode.name,
                    episodeIndex = episodeIndex,
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
                title = detail.title
                titleOriginal = detail.titleOriginal
                titleAlternative = detail.titleAlternative
                animeYear = Regex("\\d{4}").find(detail.year)?.value?.toIntOrNull()
                animePreview = AnimePreview(
                    id = detail.id,
                    title = detail.title,
                    titleOriginal = detail.titleOriginal,
                    posterUrl = detail.posterUrl,
                    episodeInfo = detail.episodeCount,
                    url = animeUrl,
                )
                val resolvedCurrentEpisode = episodes.getOrNull(currentIdx)
                _uiState.update {
                    it.copy(
                        animeTitle = detail.title,
                        posterUrl = detail.posterUrl,
                        allEpisodes = episodes,
                        currentEpisodeIndex = if (currentIdx >= 0) currentIdx else 0,
                        currentEpisode = resolvedCurrentEpisode ?: it.currentEpisode,
                    )
                }
                val currentEp = _uiState.value.currentEpisode
                val preview = animePreview
                if (currentEp != null && preview != null) {
                    addToHistoryUseCase(preview, currentEp)
                }
                _uiState.value.currentEpisode?.let { currentEpisode ->
                    loadSkipTimesForEpisode(detail.id, currentEpisode.name)
                    loadResumePosition(detail.id, currentEpisode.videoId)
                }
                if (_uiState.value.isCommentsPanelVisible) {
                    resetEpisodeComments(currentEpisodeNumber())
                    loadEpisodeComments()
                }
            }
        }
    }

    private fun loadVideo(episode: Episode, allEpisodes: List<Episode>, index: Int) {
        videoLoadJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                currentEpisode = episode,
                videoSources = emptyList(),
                allEpisodes = if (allEpisodes.isNotEmpty()) allEpisodes else it.allEpisodes,
                currentEpisodeIndex = if (allEpisodes.isNotEmpty()) index else it.currentEpisodeIndex,
                resumePositionMs = 0L,
            )
        }
        videoLoadJob = viewModelScope.launch {
            getVideoUrlUseCase(episode.videoId)
                .onSuccess { sources ->
                    if (_uiState.value.currentEpisode?.videoId != episode.videoId) return@onSuccess
                    val preferred = userPreferencesRepository.preferredVideoQuality().first()
                    if (_uiState.value.currentEpisode?.videoId != episode.videoId) return@onSuccess
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
                    if (_uiState.value.currentEpisode?.videoId != episode.videoId) return@onError
                    _uiState.update {
                        it.copy(isLoading = false, error = msg ?: "Не удалось загрузить видео")
                    }
                }
        }
    }

    private fun selectQuality(quality: String) {
        _uiState.update { it.copy(selectedQuality = quality) }
        viewModelScope.launch {
            userPreferencesRepository.setPreferredVideoQuality(quality)
        }
    }

    private fun navigateEpisode(direction: Int) {
        val state = _uiState.value
        val newIndex = state.currentEpisodeIndex + direction
        if (newIndex < 0 || newIndex > state.allEpisodes.lastIndex) return
        val episode = state.allEpisodes[newIndex]
        resetSkipState()
        loadVideo(episode, state.allEpisodes, newIndex)
        resetEpisodeComments(currentEpisodeNumber(episode, newIndex))
        if (_uiState.value.isCommentsPanelVisible) loadEpisodeComments()
        animePreview?.let { preview ->
            loadSkipTimesForEpisode(preview.id, episode.name)
            loadResumePosition(preview.id, episode.videoId)
        }
    }

    private fun showEpisodeComments() {
        val episodeNumber = currentEpisodeNumber()
        if (_uiState.value.commentsEpisodeNumber != episodeNumber) {
            resetEpisodeComments(episodeNumber)
        }
        _uiState.update { it.copy(isCommentsPanelVisible = true) }
        if (animePreview == null) {
            _uiState.update { it.copy(isLoadingEpisodeComments = true) }
            return
        }
        if (episodeCommentsScannedPage == 0 && !_uiState.value.isLoadingEpisodeComments) {
            loadEpisodeComments()
        }
    }

    private fun hideEpisodeComments() {
        episodeCommentsJob?.cancel()
        episodeCommentsJob = null
        _uiState.update {
            it.copy(
                isCommentsPanelVisible = false,
                isLoadingEpisodeComments = false,
            )
        }
    }

    private fun resetEpisodeComments(episodeNumber: Int) {
        episodeCommentsJob?.cancel()
        episodeCommentsJob = null
        episodeCommentsScannedPage = 0
        episodeCommentsTotalPages = 1
        _uiState.update {
            it.copy(
                commentsEpisodeNumber = episodeNumber,
                episodeComments = emptyList(),
                isLoadingEpisodeComments = false,
                hasMoreEpisodeComments = false,
                episodeCommentsError = null,
            )
        }
    }

    private fun loadEpisodeComments() {
        val preview = animePreview ?: return
        val state = _uiState.value
        val episode = state.currentEpisode ?: return
        if (state.isLoadingEpisodeComments) return
        val episodeNumber = state.commentsEpisodeNumber ?: currentEpisodeNumber()
        val episodeVideoId = episode.videoId

        episodeCommentsJob?.cancel()
        episodeCommentsJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingEpisodeComments = true,
                    episodeCommentsError = null,
                )
            }
            var comments = _uiState.value.episodeComments
            val initialSize = comments.size
            var scannedInBatch = 0
            var scannedPage = episodeCommentsScannedPage
            var totalPages = episodeCommentsTotalPages
            var errorMessage: String? = null

            while (
                scannedPage < totalPages &&
                scannedInBatch < EPISODE_COMMENT_SCAN_PAGE_LIMIT &&
                comments.size == initialSize
            ) {
                val nextPage = scannedPage + 1
                when (val result = getCommentsUseCase(preview.id, nextPage, animeUrl)) {
                    is Result.Success -> {
                        val page = result.data
                        totalPages = page.totalPages.coerceAtLeast(1)
                        comments = (comments + page.comments.filter {
                            it.scope == CommentScope.Episode(episodeNumber)
                        }).distinctBy { it.id }
                        scannedPage = nextPage
                        scannedInBatch++
                    }
                    is Result.Error -> {
                        errorMessage = result.message ?: "Не удалось загрузить комментарии"
                        break
                    }
                }
            }

            val currentState = _uiState.value
            if (
                currentState.currentEpisode?.videoId != episodeVideoId ||
                currentState.commentsEpisodeNumber != episodeNumber
            ) {
                return@launch
            }
            episodeCommentsScannedPage = scannedPage
            episodeCommentsTotalPages = totalPages
            _uiState.update {
                it.copy(
                    episodeComments = comments,
                    isLoadingEpisodeComments = false,
                    hasMoreEpisodeComments = scannedPage < totalPages,
                    episodeCommentsError = errorMessage,
                )
            }
        }
    }

    private fun currentEpisodeNumber(
        episode: Episode? = _uiState.value.currentEpisode,
        episodeIndex: Int = _uiState.value.currentEpisodeIndex,
    ): Int = episode?.number
        ?: Regex("\\d+").find(episode?.name.orEmpty())?.value?.toIntOrNull()
        ?: episodeIndex + 1

    private fun loadResumePosition(animeId: Int, episodeVideoId: String) {
        resumeLoadJob?.cancel()
        resumeLoadJob = viewModelScope.launch {
            val savedProgress = watchProgressRepository.getProgress(animeId, episodeVideoId)
            if (_uiState.value.currentEpisode?.videoId != episodeVideoId) return@launch
            val position = savedProgress
                ?.takeIf { it.positionMs > 0L && !it.isCompleted }
                ?.positionMs
                ?: 0L
            _uiState.update { it.copy(resumePositionMs = position) }
        }
    }

    fun checkSkipPosition(positionMs: Long, durationMs: Long) {
        localSkipDetector.updatePlaybackPosition(positionMs, durationMs)
        val active = currentSkipIntervals.firstOrNull {
            positionMs in it.startMs..it.endMs
        }
        if (_activeSkip.value != active) _activeSkip.value = active
    }

    private fun loadSkipTimesForEpisode(animeId: Int, epName: String) {
        skipTimesJob?.cancel()
        val episodeNum = _uiState.value.currentEpisode
            ?.takeIf { it.name == epName }
            ?.number
            ?: Regex("\\d+").find(epName)?.value?.toIntOrNull()
            ?: 1
        activeSkipAnimeId = animeId
        activeSkipEpisodeNumber = episodeNum
        localSkipIntervals = emptyList()
        allohaSkipIntervals = emptyList()
        publishSkipIntervals()
        skipTimesJob = viewModelScope.launch {
            val isEnabled = featureFlagsRepository.isEnabled(BetaFeature.SKIP_INTRO_OUTRO).first()
            if (!isEnabled) {
                localSkipDetector.stop()
                resetSkipState()
                return@launch
            }

            localSkipDetector.stop()
            val allohaIntervals = try {
                skipTimesRepository.getSkipTimes(
                    animeId = animeId,
                    episodeNumber = episodeNum,
                    titleOriginal = titleOriginal,
                    titleAlternative = titleAlternative,
                    year = animeYear,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(
                    error,
                    "Alloha primary skip lookup failed: animeId=%d ep=%d",
                    animeId,
                    episodeNum,
                )
                emptyList()
            }
            if (
                animeId != activeSkipAnimeId ||
                episodeNum != activeSkipEpisodeNumber
            ) {
                return@launch
            }
            if (allohaIntervals.isNotEmpty()) {
                allohaSkipIntervals = allohaIntervals
                publishSkipIntervals()
                Timber.d(
                    "Alloha primary skip selected: animeId=%d ep=%d intervals=%d",
                    animeId,
                    episodeNum,
                    allohaIntervals.size,
                )
                return@launch
            }

            Timber.d(
                "Alloha returned no skip intervals, starting local fallback: animeId=%d ep=%d",
                animeId,
                episodeNum,
            )
            val preparation = try {
                localSkipDetector.prepare(
                    LocalSkipRequest(
                        animeId = animeId,
                        episodeNumber = episodeNum,
                        title = title,
                        titleOriginal = titleOriginal,
                        titleAlternative = titleAlternative,
                        year = animeYear,
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
            if (
                animeId != activeSkipAnimeId ||
                episodeNum != activeSkipEpisodeNumber
            ) {
                return@launch
            }
            localSkipIntervals = preparation?.cachedIntervals.orEmpty()
            publishSkipIntervals()
        }
    }

    private fun publishSkipIntervals() {
        currentSkipIntervals = (
            if (allohaSkipIntervals.isNotEmpty()) allohaSkipIntervals else localSkipIntervals
            ).sortedBy { it.startMs }
        _skipIntervals.value = currentSkipIntervals
    }

    private fun resetSkipState() {
        localSkipDetector.stop()
        activeSkipAnimeId = null
        activeSkipEpisodeNumber = null
        currentSkipIntervals = emptyList()
        localSkipIntervals = emptyList()
        allohaSkipIntervals = emptyList()
        _skipIntervals.value = emptyList()
        _activeSkip.value = null
    }

    override fun onCleared() {
        localSkipDetector.stop()
        super.onCleared()
    }

    private companion object {
        const val EPISODE_COMMENT_SCAN_PAGE_LIMIT = 4
    }
}
