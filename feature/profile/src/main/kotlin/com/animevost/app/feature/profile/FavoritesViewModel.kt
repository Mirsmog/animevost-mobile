package com.animevost.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            favoriteRepository.getAllFavorites().collect { favorites ->
                _uiState.update { it.copy(favorites = favorites, isLoading = false) }
            }
        }
        // Fetch fresh data from remote on every screen open (no-op when not logged in)
        viewModelScope.launch {
            favoriteRepository.loadRemoteFavorites()
        }
    }

    fun removeFavorite(newsId: Int) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(newsId)
        }
    }
}
