package com.animevost.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.model.CatalogFilter
import com.animevost.app.core.domain.usecase.GetAnimeListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAnimeListUseCase: GetAnimeListUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        onEvent(HomeEvent.LoadInitial)
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.LoadInitial -> loadInitial()
            HomeEvent.LoadMore -> loadMore()
            HomeEvent.Refresh -> refresh()
            HomeEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadInitial() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val items = getAnimeListUseCase(page = 1, filter = CatalogFilter())
                _uiState.update {
                    it.copy(
                        animeList = items,
                        isLoading = false,
                        currentPage = 1,
                        canLoadMore = items.isNotEmpty(),
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Ошибка загрузки")
                }
            }
        }
    }

    private fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore) return
        val nextPage = state.currentPage + 1
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            try {
                val items = getAnimeListUseCase(page = nextPage, filter = CatalogFilter())
                _uiState.update {
                    it.copy(
                        animeList = it.animeList + items,
                        isLoadingMore = false,
                        currentPage = nextPage,
                        canLoadMore = items.isNotEmpty(),
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoadingMore = false, error = e.message ?: "Ошибка загрузки")
                }
            }
        }
    }

    private fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val items = getAnimeListUseCase(page = 1, filter = CatalogFilter())
                _uiState.update {
                    HomeUiState(
                        animeList = items,
                        isLoading = false,
                        currentPage = 1,
                        canLoadMore = items.isNotEmpty(),
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Ошибка загрузки")
                }
            }
        }
    }
}
