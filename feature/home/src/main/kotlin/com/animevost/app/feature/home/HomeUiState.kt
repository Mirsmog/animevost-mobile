package com.animevost.app.feature.home

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.SortOption

data class HomeUiState(
    val animeList: List<AnimePreview> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val canLoadMore: Boolean = true,
    val sort: SortOption = SortOption.DATE,
    val sortAscending: Boolean = false,
)

sealed interface HomeEvent {
    data object LoadInitial : HomeEvent
    data object LoadMore : HomeEvent
    data object Refresh : HomeEvent
    data object ClearError : HomeEvent
    data class SelectSort(val sort: SortOption) : HomeEvent
}
