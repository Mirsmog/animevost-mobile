package com.animevost.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.domain.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<AnimePreview> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Re-subscribe to the correct flow (remote vs local) whenever auth state changes
            authRepository.isLoggedInFlow
                .flatMapLatest { isLoggedIn ->
                    if (isLoggedIn) {
                        // Load fresh data from remote (fire-and-forget)
                        launch { favoriteRepository.loadRemoteFavorites() }
                    }
                    favoriteRepository.getAllFavorites()
                }
                .collect { favorites ->
                    _uiState.update { it.copy(favorites = favorites, isLoading = false) }
                }
        }
    }

    fun removeFavorite(newsId: Int) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(newsId)
        }
    }
}
