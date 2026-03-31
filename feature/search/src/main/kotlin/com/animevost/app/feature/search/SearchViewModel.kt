package com.animevost.app.feature.search

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.usecase.SearchAnimeUseCase
import com.animevost.app.core.domain.util.BasePaginatedViewModel
import com.animevost.app.core.domain.util.Result
import com.animevost.app.core.domain.util.asSearchQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchAnimeUseCase: SearchAnimeUseCase,
) : BasePaginatedViewModel<AnimePreview>() {

    private val _queryFlow = MutableStateFlow("")
    private var currentQuery = ""

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    override suspend fun fetchPage(page: Int): List<AnimePreview> {
        val result = searchAnimeUseCase(currentQuery, page)
        return when (result) {
            is Result.Success -> result.data
            is Result.Error -> throw result.exception ?: Exception(result.message)
        }
    }

    override fun mergeItems(
        existing: List<AnimePreview>,
        new: List<AnimePreview>,
    ): List<AnimePreview> = (existing + new).distinctBy { it.id }

    init {
        // Sync base-class pagination flows into SearchUiState.
        combine(items, isLoading, isLoadingMore, error, hasMore) { results, loading, loadingMore, err, more ->
            _uiState.update {
                it.copy(
                    results = results,
                    isLoading = loading,
                    isLoadingMore = loadingMore,
                    error = err,
                    canLoadMore = more,
                )
            }
        }.launchIn(viewModelScope)

        // Immediate handling: keep query in sync and clear stale results for short input.
        viewModelScope.launch {
            _queryFlow.collect { query ->
                _uiState.update { it.copy(query = query) }
                if (query.isBlank() || query.trim().length < MIN_QUERY_LENGTH) {
                    cancelLoad()
                    _items.value = emptyList()
                    _error.value = null
                    _uiState.update { it.copy(hasSearched = false) }
                }
            }
        }

        // Debounced: trigger a fresh load only for valid, stable queries.
        viewModelScope.launch {
            _queryFlow.asSearchQuery(DEBOUNCE_MS).collect { query ->
                // Guard against a stale debounce firing after the user cleared the input.
                if (query.isBlank() || query != _queryFlow.value) return@collect
                currentQuery = query
                _uiState.update { it.copy(hasSearched = true) }
                loadInitial()
            }
        }
    }

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> _queryFlow.value = event.query
            SearchEvent.LoadMore        -> loadMore()
            SearchEvent.ClearError      -> _error.value = null
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 300L
        private const val MIN_QUERY_LENGTH = 4
    }
}
