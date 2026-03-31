package com.animevost.app.feature.catalog

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeType
import com.animevost.app.core.domain.model.CatalogFilter
import com.animevost.app.core.domain.model.Genre
import com.animevost.app.core.domain.usecase.GetAnimeListUseCase
import com.animevost.app.core.domain.util.BasePaginatedViewModel
import com.animevost.app.core.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    private val catalogFilter: CatalogFilter = buildCatalogFilter()

    val uiState: StateFlow<FilteredListUiState> = combine(
        items, isLoading, isLoadingMore, error, hasMore,
    ) { animeList, loading, loadingMore, err, more ->
        FilteredListUiState(
            animeList = animeList,
            isLoading = loading,
            isLoadingMore = loadingMore,
            error = err,
            canLoadMore = more,
            filterLabel = filterLabel,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = FilteredListUiState(filterLabel = filterLabel),
    )

    init {
        loadInitial()
    }

    fun onEvent(event: FilteredListEvent) {
        when (event) {
            FilteredListEvent.LoadMore -> loadMore()
            FilteredListEvent.Refresh -> refresh()
            FilteredListEvent.ClearError -> _error.value = null
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

    private fun buildCatalogFilter(): CatalogFilter = when (filterType) {
        "genre" -> CatalogFilter(genre = Genre(0, filterLabel, filterValue))
        "year"  -> CatalogFilter(year = filterValue)
        "type"  -> CatalogFilter(
            type = AnimeType.entries.find { it.name == filterValue } ?: AnimeType.UNKNOWN,
        )
        else -> CatalogFilter()
    }
}
