package com.animevost.app.feature.search

import com.animevost.app.core.domain.model.AnimePreview

data class SearchUiState(
    val query: String = "",
    val results: List<AnimePreview> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val canLoadMore: Boolean = true,
    val hasSearched: Boolean = false,
)

sealed interface SearchEvent {
    data class QueryChanged(val query: String) : SearchEvent
    data object LoadMore : SearchEvent
    data object ClearError : SearchEvent
}
