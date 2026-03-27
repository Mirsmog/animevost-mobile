package com.animevost.app.feature.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.model.AnimeType
import com.animevost.app.core.domain.model.CatalogFilter
import com.animevost.app.core.domain.model.Genre
import com.animevost.app.core.domain.usecase.GetAnimeListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilteredListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAnimeListUseCase: GetAnimeListUseCase,
) : ViewModel() {

    private val filterType: String = savedStateHandle["filterType"] ?: ""
    private val filterValue: String = savedStateHandle["filterValue"] ?: ""
    private val filterLabel: String = savedStateHandle["filterLabel"] ?: ""

    private val _uiState = MutableStateFlow(FilteredListUiState(filterLabel = filterLabel))
    val uiState: StateFlow<FilteredListUiState> = _uiState.asStateFlow()

    private val catalogFilter: CatalogFilter = buildFilter()

    init {
        loadInitial()
    }

    fun onEvent(event: FilteredListEvent) {
        when (event) {
            FilteredListEvent.LoadMore -> loadMore()
            FilteredListEvent.Refresh -> refresh()
            FilteredListEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun buildFilter(): CatalogFilter {
        return when (filterType) {
            "genre" -> CatalogFilter(genre = Genre(0, filterLabel, filterValue))
            "year" -> CatalogFilter(year = filterValue)
            "type" -> CatalogFilter(
                type = AnimeType.entries.find { it.name == filterValue } ?: AnimeType.UNKNOWN,
            )
            else -> CatalogFilter()
        }
    }

    private fun loadInitial() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val items = getAnimeListUseCase(page = 1, filter = catalogFilter)
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
                val items = getAnimeListUseCase(page = nextPage, filter = catalogFilter)
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
                val items = getAnimeListUseCase(page = 1, filter = catalogFilter)
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
}
