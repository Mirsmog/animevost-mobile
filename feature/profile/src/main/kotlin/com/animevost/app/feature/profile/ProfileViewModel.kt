package com.animevost.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeStatus
import com.animevost.app.core.domain.model.User
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.domain.repository.FavoriteRepository
import com.animevost.app.core.domain.repository.UserListRepository
import com.animevost.app.core.domain.usecase.GetWatchHistoryUseCase
import com.animevost.app.core.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isLoggedIn: Boolean = false,
    val favorites: List<AnimePreview> = emptyList(),
    val history: List<AnimePreview> = emptyList(),
    val watchLists: Map<AnimeStatus, List<AnimePreview>> = emptyMap(),
    val selectedTab: ProfileTab = ProfileTab.FAVORITES,
    val isLoading: Boolean = false,
    val error: String? = null,
)

enum class ProfileTab(val title: String) {
    FAVORITES("Избранное"),
    HISTORY("История"),
    LISTS("Списки"),
}

sealed interface ProfileEvent {
    data object LoadProfile : ProfileEvent
    data class SelectTab(val tab: ProfileTab) : ProfileEvent
    data object Logout : ProfileEvent
    data object Refresh : ProfileEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val favoriteRepository: FavoriteRepository,
    private val getWatchHistoryUseCase: GetWatchHistoryUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val userListRepository: UserListRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // Re-load profile (user info + history) on every auth state change
        viewModelScope.launch {
            authRepository.isLoggedInFlow.collect { loadProfile() }
        }
        // Reactive favorites preview — re-subscribes on login/logout
        viewModelScope.launch {
            authRepository.isLoggedInFlow
                .flatMapLatest { isLoggedIn ->
                    if (isLoggedIn) {
                        launch { favoriteRepository.loadRemoteFavorites() }
                        favoriteRepository.getAllFavorites()
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
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.LoadProfile -> loadProfile()
            is ProfileEvent.SelectTab -> _uiState.update { it.copy(selectedTab = event.tab) }
            is ProfileEvent.Logout -> logout()
            is ProfileEvent.Refresh -> loadProfile()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val isLoggedIn = authRepository.isLoggedIn()
                val user = if (isLoggedIn) authRepository.getCurrentUser() else null
                val history = try { getWatchHistoryUseCase() } catch (_: Exception) { emptyList() }
                _uiState.update {
                    it.copy(
                        user = user,
                        isLoggedIn = isLoggedIn,
                        history = history,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка загрузки профиля",
                    )
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            try {
                logoutUseCase()
            } catch (_: Exception) {
                // Server-side logout is best-effort; always clear local state below
            }
            _uiState.update { ProfileUiState() }
        }
    }
}
