package com.animevost.app.feature.catalog

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeType
import com.animevost.app.core.domain.model.CatalogFilter
import com.animevost.app.core.domain.model.Genre
import com.animevost.app.core.domain.model.SortOption
import com.animevost.app.core.domain.usecase.GetAnimeListUseCase
import com.animevost.app.core.domain.util.BasePaginatedViewModel
import com.animevost.app.core.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class FilteredListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAnimeListUseCase: GetAnimeListUseCase,
) : BasePaginatedViewModel<AnimePreview>() {

    private val filterType: String = requireNotNull(savedStateHandle["filterType"]) {
        "Missing required navigation argument: filterType"
    }
    private val filterValue: String = requireNotNull(savedStateHandle["filterValue"]) {
        "Missing required navigation argument: filterValue"
    }
    private val filterLabel: String = requireNotNull(savedStateHandle["filterLabel"]) {
        "Missing required navigation argument: filterLabel"
    }

    private var currentSort: SortOption = SortOption.DATE
    private var currentSortAscending: Boolean = false
    private var catalogFilter: CatalogFilter = buildCatalogFilter()

    private val _uiState = MutableStateFlow(FilteredListUiState(filterLabel = filterLabel))
    val uiState: StateFlow<FilteredListUiState> = _uiState.asStateFlow()

    init {
        combine(items, isLoading, isLoadingMore, error, hasMore) { animeList, loading, loadingMore, err, more ->
            _uiState.update {
                it.copy(
                    animeList = animeList,
                    isLoading = loading,
                    isLoadingMore = loadingMore,
                    error = err,
                    canLoadMore = more,
                )
            }
        }.launchIn(viewModelScope)
        loadInitial()
    }

    fun onEvent(event: FilteredListEvent) {
        when (event) {
            FilteredListEvent.LoadMore -> loadMore()
            FilteredListEvent.Refresh -> refresh()
            FilteredListEvent.ClearError -> _error.value = null
            is FilteredListEvent.SelectSort -> selectSort(event.sort)
        }
    }

    override suspend fun fetchPage(page: Int): List<AnimePreview> {
        val result = getAnimeListUseCase(page = page, filter = catalogFilter)
        return when (result) {
            is Result.Success -> result.data
            is Result.Error -> throw result.exception ?: Exception(result.message)
        }
    }

    override fun mergeItems(
        existing: List<AnimePreview>,
        new: List<AnimePreview>,
    ): List<AnimePreview> = (existing + new).distinctBy { it.id }

    private fun selectSort(sort: SortOption) {
        currentSortAscending = if (currentSort == sort) !currentSortAscending else false
        currentSort = sort
        catalogFilter = catalogFilter.copy(sortBy = sort, sortAscending = currentSortAscending)
        _uiState.update { it.copy(sort = sort, sortAscending = currentSortAscending) }
        _items.value = emptyList()
        loadInitial()
    }

    private fun buildCatalogFilter(): CatalogFilter = when (filterType) {
        "genre" -> CatalogFilter(genre = Genre(0, filterLabel, filterValue))
        "year"  -> CatalogFilter(year = filterValue)
        "type"  -> CatalogFilter(
            type = AnimeType.entries.find { it.name == filterValue } ?: AnimeType.UNKNOWN,
        )
        "path"  -> CatalogFilter(path = filterValue)
        else -> CatalogFilter()
    }
}
