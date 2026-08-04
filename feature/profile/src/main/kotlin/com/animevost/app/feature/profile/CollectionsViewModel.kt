package com.animevost.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeStatus
import com.animevost.app.core.domain.model.FavoriteEntry
import com.animevost.app.core.domain.model.HistoryEntry
import com.animevost.app.core.domain.model.SortOption
import com.animevost.app.core.domain.model.UserListEntry
import com.animevost.app.core.domain.model.WatchProgress
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.domain.repository.FavoriteRepository
import com.animevost.app.core.domain.repository.HistoryRepository
import com.animevost.app.core.domain.repository.UserListRepository
import com.animevost.app.core.domain.repository.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URI
import javax.inject.Inject

private data class ControlState(
    val query: String = "",
    val selectedFilter: LibraryFilter = LibraryFilter.All,
    val sort: SortOption = SortOption.DATE,
    val sortAscending: Boolean = false,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val historyRepository: HistoryRepository,
    private val userListRepository: UserListRepository,
    private val watchProgressRepository: WatchProgressRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val controls = MutableStateFlow(ControlState())

    val uiState: StateFlow<LibraryUiState> = authRepository.isLoggedInFlow
        .flatMapLatest { isLoggedIn ->
            if (isLoggedIn) {
                viewModelScope.launch { favoriteRepository.loadRemoteFavorites() }
            }
            combine(
                favoriteRepository.getAllFavoriteEntries(),
                historyRepository.getHistoryFlow(),
                userListRepository.getAllEntries(),
                watchProgressRepository.observeLatestPerAnime(),
                controls,
            ) { favorites, history, userListEntries, watchProgress, controlState ->
                val merged = buildMergedItems(favorites, history, userListEntries)
                val statusCounts = AnimeStatus.entries.associateWith { st ->
                    merged.count { it.listStatus == st }
                }.filterValues { it > 0 }
                val favoritesCount = merged.count { it.isFavorite }

                val rail = buildContinueWatching(watchProgress, merged)

                val filtered = merged
                    .asSequence()
                    .filter { it.matchesQuery(controlState.query) }
                    .filter { it.matchesFilter(controlState.selectedFilter) }
                    .sortedWith(comparatorFor(controlState.sort, controlState.sortAscending))
                    .toList()

                LibraryUiState(
                    isLoading = false,
                    query = controlState.query,
                    items = filtered,
                    totalCount = merged.size,
                    continueWatching = rail,
                    statusCounts = statusCounts,
                    favoritesCount = favoritesCount,
                    sort = controlState.sort,
                    sortAscending = controlState.sortAscending,
                    selectedFilter = controlState.selectedFilter,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.QueryChanged -> controls.update { it.copy(query = event.query) }
            is LibraryEvent.FilterSelected -> controls.update { it.copy(selectedFilter = event.filter) }
            is LibraryEvent.SortSelected -> controls.update {
                val defaultAscending = event.sort == SortOption.TITLE
                it.copy(sort = event.sort, sortAscending = defaultAscending)
            }
            is LibraryEvent.SortDirectionChanged -> controls.update {
                it.copy(sortAscending = event.ascending)
            }
        }
    }

    fun hydratePreview(anime: AnimePreview) {
        if (anime.id <= 0 || anime.title.isNotBlank()) return
        viewModelScope.launch {
            try {
                userListRepository.hydratePreview(anime.id)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // A visible card can retry when it is composed again.
            }
        }
    }

    private fun buildContinueWatching(
        progress: List<WatchProgress>,
        merged: List<LibraryItem>,
    ): List<ContinueWatchingItem> {
        if (progress.isEmpty()) return emptyList()
        val byId = merged.associateBy { it.anime.id }
        return progress.asSequence()
            .filter { !it.isCompleted }
            .mapNotNull { wp ->
                val anime = byId[wp.animeId]?.anime ?: return@mapNotNull null
                val fraction = if (wp.durationMs > 0) {
                    (wp.positionMs.toFloat() / wp.durationMs).coerceIn(0f, 1f)
                } else 0f
                ContinueWatchingItem(
                    anime = anime,
                    episodeVideoId = wp.episodeVideoId,
                    episodeName = wp.episodeName,
                    episodeIndex = wp.episodeIndex,
                    progressFraction = fraction,
                    updatedAt = wp.updatedAt,
                )
            }
            .sortedByDescending { it.updatedAt }
            .take(20)
            .toList()
    }

    private fun buildMergedItems(
        favorites: List<FavoriteEntry>,
        history: List<HistoryEntry>,
        userListEntries: List<UserListEntry>,
    ): List<LibraryItem> {
        val historyMap = history.associateBy { keyFor(it.anime.url, it.anime.id) }
        val userListMap = userListEntries.associateBy { keyFor(it.anime.url, it.anime.id) }
        val items = linkedMapOf<String, LibraryItem>()

        favorites.forEach { favorite ->
            val key = keyFor(favorite.anime.url, favorite.anime.id)
            val historyEntry = historyMap[key]
            val listEntry = userListMap[key]
            items[key] = LibraryItem(
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
                isFavorite = true,
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
                items[key] = LibraryItem(
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
                items[key] = LibraryItem(
                    anime = entry.anime,
                    sources = setOf(CollectionSource.LISTS),
                    lastActivityAt = entry.updatedAt,
                    listStatus = entry.status,
                )
            }
        }

        return items.values.toList()
    }

    private fun LibraryItem.matchesFilter(filter: LibraryFilter): Boolean = when (filter) {
        LibraryFilter.All -> true
        LibraryFilter.Favorites -> isFavorite
        is LibraryFilter.Status -> listStatus == filter.status
    }

    private fun LibraryItem.matchesQuery(query: String): Boolean {
        val normalized = query.trim()
        if (normalized.isBlank()) return true
        return anime.title.contains(normalized, ignoreCase = true) ||
            anime.titleOriginal.contains(normalized, ignoreCase = true)
    }

    private fun comparatorFor(sort: SortOption, ascending: Boolean): Comparator<LibraryItem> {
        val comparator = when (sort) {
            SortOption.TITLE -> compareBy<LibraryItem> { it.anime.title.lowercase() }
                .thenBy { it.anime.titleOriginal.lowercase() }
            else -> compareBy<LibraryItem> { it.lastActivityAt }
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
        if (id > 0) return "id:$id"
        val normalizedUrl = try {
            URI(url).path.trimStart('/').ifBlank { url.trimStart('/') }
        } catch (_: Exception) {
            url.trimStart('/')
        }
        return normalizedUrl.ifBlank { id.toString() }
    }
}
