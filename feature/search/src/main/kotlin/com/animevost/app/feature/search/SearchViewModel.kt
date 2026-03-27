package com.animevost.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.usecase.SearchAnimeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchAnimeUseCase: SearchAnimeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> onQueryChanged(event.query)
            SearchEvent.LoadMore -> loadMore()
            SearchEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    results = emptyList(),
                    hasSearched = false,
                    error = null,
                    currentPage = 1,
                    canLoadMore = true,
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            performSearch(query, page = 1)
        }
    }

    private suspend fun performSearch(query: String, page: Int) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val items = searchAnimeUseCase(query, page)
            _uiState.update {
                it.copy(
                    results = items,
                    isLoading = false,
                    currentPage = 1,
                    canLoadMore = items.isNotEmpty(),
                    hasSearched = true,
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isLoading = false, error = e.message ?: "Ошибка поиска")
            }
        }
    }

    private fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore || state.query.isBlank()) return
        val nextPage = state.currentPage + 1
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            try {
                val items = searchAnimeUseCase(state.query, nextPage)
                _uiState.update {
                    it.copy(
                        results = it.results + items,
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

    companion object {
        private const val DEBOUNCE_MS = 500L
    }
}
