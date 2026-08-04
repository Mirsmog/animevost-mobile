package com.animevost.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeStatus
import com.animevost.app.core.domain.model.User
import com.animevost.app.core.domain.model.BetaFeature
import com.animevost.app.core.domain.repository.FeatureFlagsRepository
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.domain.repository.FavoriteRepository
import com.animevost.app.core.domain.repository.UserListRepository
import com.animevost.app.core.domain.usecase.LogoutUseCase
import com.animevost.app.core.domain.usecase.GetWatchHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isLoggedIn: Boolean = false,
    val favorites: List<AnimePreview> = emptyList(),
    val history: List<AnimePreview> = emptyList(),
    val watchLists: Map<AnimeStatus, List<AnimePreview>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val watchStatusEnabled: Boolean = false,
)

sealed interface ProfileEvent {
    data object Logout : ProfileEvent
    data object Refresh : ProfileEvent
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val favoriteRepository: FavoriteRepository,
    private val getWatchHistoryUseCase: GetWatchHistoryUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val userListRepository: UserListRepository,
    private val featureFlagsRepository: FeatureFlagsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // Re-load profile (user info + history) on every auth state change
        viewModelScope.launch {
            authRepository.isLoggedInFlow.collectLatest { refreshProfile() }
        }
        // Reactive favorites preview, with remote refresh cancelled immediately on logout.
        viewModelScope.launch {
            authRepository.isLoggedInFlow
                .flatMapLatest { isLoggedIn ->
                    if (isLoggedIn) {
                        favoriteRepository.getAllFavorites()
                            .onStart { favoriteRepository.loadRemoteFavorites() }
                    } else {
                        flowOf(emptyList())
                    }
                }
                .collect { favorites ->
                    _uiState.update { it.copy(favorites = favorites.take(8)) }
                }
        }
        // Reactive watch lists per status
        viewModelScope.launch {
            val flows = AnimeStatus.entries.map { status -> userListRepository.getByStatus(status) }
            combine(flows) { arrays ->
                AnimeStatus.entries.zip(arrays.toList()).associate { (status, list) -> status to list }
            }.collect { watchLists ->
                _uiState.update { it.copy(watchLists = watchLists) }
            }
        }
        // Watch status beta flag
        viewModelScope.launch {
            featureFlagsRepository.isEnabled(BetaFeature.WATCH_STATUS)
                .collect { enabled -> _uiState.update { it.copy(watchStatusEnabled = enabled) } }
        }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.Logout -> logout()
            is ProfileEvent.Refresh -> loadProfile()
        }
    }

    fun hydratePreview(anime: AnimePreview) {
        if (anime.id <= 0 || anime.title.isNotBlank()) return
        viewModelScope.launch {
            try {
                userListRepository.hydratePreview(anime.id)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // A visible card can retry when it is composed again.
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch { refreshProfile() }
    }

    private suspend fun refreshProfile() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val isLoggedIn = authRepository.isLoggedIn()
            val user = if (isLoggedIn) authRepository.getCurrentUser() else null
            val history = try {
                getWatchHistoryUseCase()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                emptyList()
            }
            _uiState.update {
                it.copy(
                    user = user,
                    isLoggedIn = isLoggedIn,
                    history = history,
                    isLoading = false,
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка загрузки профиля",
                )
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            try {
                logoutUseCase()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Server-side logout is best-effort; always clear local state below
            }
            _uiState.update { ProfileUiState() }
        }
    }
}
