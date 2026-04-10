package com.animevost.app.feature.home

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeType
import com.animevost.app.core.domain.model.Genre
import com.animevost.app.core.domain.model.SortOption

data class HomeUiState(
    // ── Anime list ────────────────────────────────────────────
    val animeList: List<AnimePreview> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val canLoadMore: Boolean = true,
    val sort: SortOption = SortOption.DATE,
    val sortAscending: Boolean = false,
    val selectedType: AnimeType? = null,

    // ── Search ────────────────────────────────────────────────
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<AnimePreview> = emptyList(),
    val isSearchLoading: Boolean = false,
    val isSearchLoadingMore: Boolean = false,
    val searchPage: Int = 1,
    val canSearchLoadMore: Boolean = true,
    val hasSearched: Boolean = false,
    val searchError: String? = null,

    // ── Nav data (genres / years for search browsing) ─────────────────────────
    val genres: List<Genre> = emptyList(),
    val years: List<String> = emptyList(),
    val isLoadingRandom: Boolean = false,
    val randomAnimeUrl: String? = null,
)

sealed interface HomeEvent {
    data object LoadInitial : HomeEvent
    data object LoadMore : HomeEvent
    data object Refresh : HomeEvent
    data object ClearError : HomeEvent
    data class SelectSort(val sort: SortOption) : HomeEvent
    data class SelectType(val type: AnimeType?) : HomeEvent
    data object ToggleSearch : HomeEvent
    data class SearchQueryChanged(val query: String) : HomeEvent
    data object SearchLoadMore : HomeEvent
    data object RandomAnime : HomeEvent
    data object ConsumedRandomAnime : HomeEvent
}
