package com.animevost.app.feature.profile

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeStatus
import com.animevost.app.core.domain.model.SortOption

data class LibraryItem(
    val anime: AnimePreview,
    val sources: Set<CollectionSource>,
    val lastActivityAt: Long,
    val listStatus: AnimeStatus? = null,
    val isFavorite: Boolean = false,
)

enum class CollectionSource {
    FAVORITES,
    HISTORY,
    LISTS,
}

data class ContinueWatchingItem(
    val anime: AnimePreview,
    val episodeVideoId: String,
    val episodeName: String,
    val episodeIndex: Int,
    val progressFraction: Float,
    val updatedAt: Long,
)

sealed interface LibraryFilter {
    data object All : LibraryFilter
    data object Favorites : LibraryFilter
    data class Status(val status: AnimeStatus) : LibraryFilter
}

data class LibraryUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val items: List<LibraryItem> = emptyList(),
    val totalCount: Int = 0,
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val statusCounts: Map<AnimeStatus, Int> = emptyMap(),
    val favoritesCount: Int = 0,
    val sort: SortOption = SortOption.DATE,
    val sortAscending: Boolean = false,
    val selectedFilter: LibraryFilter = LibraryFilter.All,
)

sealed interface LibraryEvent {
    data class QueryChanged(val query: String) : LibraryEvent
    data class FilterSelected(val filter: LibraryFilter) : LibraryEvent
    data class SortSelected(val sort: SortOption) : LibraryEvent
    data class SortDirectionChanged(val ascending: Boolean) : LibraryEvent
}
