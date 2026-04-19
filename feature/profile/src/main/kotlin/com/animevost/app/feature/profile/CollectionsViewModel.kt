package com.animevost.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeStatus
import com.animevost.app.core.domain.model.FavoriteEntry
import com.animevost.app.core.domain.model.HistoryEntry
import com.animevost.app.core.domain.model.SortOption
import com.animevost.app.core.domain.model.UserListEntry
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.domain.repository.FavoriteRepository
import com.animevost.app.core.domain.repository.HistoryRepository
import com.animevost.app.core.domain.repository.UserListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URI
import javax.inject.Inject

data class CollectionItem(
    val anime: AnimePreview,
    val sources: Set<CollectionSource>,
    val lastActivityAt: Long,
    val listStatus: AnimeStatus? = null,
)

enum class CollectionSource {
    FAVORITES,
    HISTORY,
    LISTS,
}

enum class CollectionsTab(val title: String) {
    ALL("Все"),
    HISTORY("История"),
    FAVORITES("Избранное"),
    LISTS("Списки"),
}

data class CollectionsUiState(
    val items: List<CollectionItem> = emptyList(),
    val availableStatuses: List<AnimeStatus> = emptyList(),
    val tabCounts: Map<CollectionsTab, Int> = emptyMap(),
    val statusCounts: Map<AnimeStatus, Int> = emptyMap(),
    val query: String = "",
    val selectedTab: CollectionsTab = CollectionsTab.ALL,
    val selectedStatus: AnimeStatus? = null,
    val sort: SortOption = SortOption.DATE,
    val sortAscending: Boolean = false,
    val isLoading: Boolean = true,
)

sealed interface CollectionsEvent {
    data class QueryChanged(val query: String) : CollectionsEvent
    data class TabSelected(val tab: CollectionsTab) : CollectionsEvent
    data class StatusSelected(val status: AnimeStatus?) : CollectionsEvent
    data class SortSelected(val sort: SortOption) : CollectionsEvent
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val historyRepository: HistoryRepository,
    private val userListRepository: UserListRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val controls = MutableStateFlow(CollectionsUiState())

    val uiState: StateFlow<CollectionsUiState> = authRepository.isLoggedInFlow
        .flatMapLatest { isLoggedIn ->
            if (isLoggedIn) {
                viewModelScope.launch { favoriteRepository.loadRemoteFavorites() }
            }
            combine(
                favoriteRepository.getAllFavoriteEntries(),
                historyRepository.getHistoryFlow(),
                userListRepository.getAllEntries(),
                controls,
            ) { favorites, history, userListEntries, controlState ->
                val mergedItems = buildMergedItems(
                    favorites = favorites,
                    history = history,
                    userListEntries = userListEntries,
                )
                val queryItems = mergedItems.filter { it.matchesQuery(controlState.query) }
                val listItems = queryItems.filter { matchesTab(it, CollectionsTab.LISTS) }
                val statusCounts = AnimeStatus.entries
                    .associateWith { status -> listItems.count { it.listStatus == status } }
                    .filterValues { it > 0 }
                val availableStatuses = AnimeStatus.entries.filter { status -> status in statusCounts }
                val selectedStatus = controlState.selectedStatus?.takeIf { it in availableStatuses }
                val tabCounts = CollectionsTab.entries.associateWith { tab -> queryItems.count { matchesTab(it, tab) } }

                controlState.copy(
                    items = filterAndSortItems(
                        items = queryItems,
                        tab = controlState.selectedTab,
                        status = selectedStatus,
                        sort = controlState.sort,
                        ascending = controlState.sortAscending,
                    ),
                    availableStatuses = availableStatuses,
                    tabCounts = tabCounts,
                    statusCounts = statusCounts,
                    selectedStatus = selectedStatus,
                    isLoading = false,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionsUiState())

    fun onEvent(event: CollectionsEvent) {
        when (event) {
            is CollectionsEvent.QueryChanged -> controls.update {
                it.copy(query = event.query)
            }

            is CollectionsEvent.TabSelected -> controls.update {
                it.copy(
                    selectedTab = event.tab,
                    selectedStatus = if (event.tab == CollectionsTab.LISTS) it.selectedStatus else null,
                )
            }

            is CollectionsEvent.StatusSelected -> controls.update {
                it.copy(selectedStatus = event.status)
            }

            is CollectionsEvent.SortSelected -> controls.update {
                val nextAscending = if (it.sort == event.sort) !it.sortAscending else event.sort == SortOption.TITLE
                it.copy(sort = event.sort, sortAscending = nextAscending)
            }
        }
    }

    private fun buildMergedItems(
        favorites: List<FavoriteEntry>,
        history: List<HistoryEntry>,
        userListEntries: List<UserListEntry>,
    ): List<CollectionItem> {
        val historyMap = history.associateBy { keyFor(it.anime.url, it.anime.id) }
        val userListMap = userListEntries.associateBy { keyFor(it.anime.url, it.anime.id) }
        val items = linkedMapOf<String, CollectionItem>()

        favorites.forEach { favorite ->
            val key = keyFor(favorite.anime.url, favorite.anime.id)
            val historyEntry = historyMap[key]
            val listEntry = userListMap[key]
            items[key] = CollectionItem(
                anime = mergeAnime(favorite.anime, historyEntry?.anime, listEntry?.anime),
                sources = buildSet {
                    add(CollectionSource.FAVORITES)
                    if (historyEntry != null) add(CollectionSource.HISTORY)
                    if (listEntry != null) add(CollectionSource.LISTS)
                },
                lastActivityAt = maxOf(
                    favorite.addedAt,
                    historyEntry?.watchedAt ?: 0L,
                    listEntry?.updatedAt ?: 0L,
                    0L,
                ),
                listStatus = listEntry?.status,
            )
        }

        history.forEach { entry ->
            val key = keyFor(entry.anime.url, entry.anime.id)
            val existing = items[key]
            if (existing != null) {
                items[key] = existing.copy(
                    anime = mergeAnime(existing.anime, entry.anime),
                    sources = existing.sources + CollectionSource.HISTORY,
                    lastActivityAt = maxOf(existing.lastActivityAt, entry.watchedAt),
                )
            } else {
                val listEntry = userListMap[key]
                items[key] = CollectionItem(
                    anime = mergeAnime(entry.anime, listEntry?.anime),
                    sources = buildSet {
                        add(CollectionSource.HISTORY)
                        if (listEntry != null) add(CollectionSource.LISTS)
                    },
                    lastActivityAt = maxOf(entry.watchedAt, listEntry?.updatedAt ?: 0L),
                    listStatus = listEntry?.status,
                )
            }
        }

        userListEntries.forEach { entry ->
            val key = keyFor(entry.anime.url, entry.anime.id)
            val existing = items[key]
            if (existing != null) {
                items[key] = existing.copy(
                    anime = mergeAnime(existing.anime, entry.anime),
                    sources = existing.sources + CollectionSource.LISTS,
                    lastActivityAt = maxOf(existing.lastActivityAt, entry.updatedAt),
                    listStatus = entry.status,
                )
            } else {
                items[key] = CollectionItem(
                    anime = entry.anime,
                    sources = setOf(CollectionSource.LISTS),
                    lastActivityAt = entry.updatedAt,
                    listStatus = entry.status,
                )
            }
        }

        return items.values.toList()
    }

    private fun filterAndSortItems(
        items: List<CollectionItem>,
        tab: CollectionsTab,
        status: AnimeStatus?,
        sort: SortOption,
        ascending: Boolean,
    ): List<CollectionItem> {
        return items.asSequence()
            .filter { item -> matchesTab(item, tab) }
            .filter { item -> status == null || item.listStatus == status }
            .sortedWith(collectionComparator(sort, ascending))
            .toList()
    }

    private fun matchesTab(item: CollectionItem, tab: CollectionsTab): Boolean = when (tab) {
        CollectionsTab.ALL -> true
        CollectionsTab.HISTORY -> CollectionSource.HISTORY in item.sources
        CollectionsTab.FAVORITES -> CollectionSource.FAVORITES in item.sources
        CollectionsTab.LISTS -> CollectionSource.LISTS in item.sources
    }

    private fun CollectionItem.matchesQuery(query: String): Boolean {
        val normalized = query.trim()
        if (normalized.isBlank()) return true
        return anime.title.contains(normalized, ignoreCase = true) ||
            anime.titleOriginal.contains(normalized, ignoreCase = true)
    }

    private fun collectionComparator(sort: SortOption, ascending: Boolean): Comparator<CollectionItem> {
        val comparator = when (sort) {
            SortOption.TITLE -> compareBy<CollectionItem> { it.anime.title.lowercase() }
                .thenBy { it.anime.titleOriginal.lowercase() }
            else -> compareBy<CollectionItem> { it.lastActivityAt }
                .thenBy { it.anime.title.lowercase() }
        }
        return if (ascending) comparator else comparator.reversed()
    }

    private fun mergeAnime(primary: AnimePreview, vararg fallbacks: AnimePreview?): AnimePreview {
        var merged = primary
        fallbacks.filterNotNull().forEach { fallback ->
            merged = merged.copy(
                title = merged.title.ifBlank { fallback.title },
                titleOriginal = merged.titleOriginal.ifBlank { fallback.titleOriginal },
                posterUrl = merged.posterUrl.ifBlank { fallback.posterUrl },
                episodeInfo = merged.episodeInfo.ifBlank { fallback.episodeInfo },
                url = merged.url.ifBlank { fallback.url },
            )
        }
        return merged
    }

    private fun keyFor(url: String, id: Int): String {
        val normalizedUrl = try {
            URI(url).path.trimStart('/').ifBlank { url.trimStart('/') }
        } catch (_: Exception) {
            url.trimStart('/')
        }
        return normalizedUrl.ifBlank { id.toString() }
    }
}
