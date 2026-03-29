package com.animevost.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.model.AnimeType
import com.animevost.app.core.domain.model.CatalogFilter
import com.animevost.app.core.domain.model.SortOption
import com.animevost.app.core.domain.usecase.GetAnimeListUseCase
import com.animevost.app.core.domain.usecase.GetNavDataUseCase
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
class HomeViewModel @Inject constructor(
    private val getAnimeListUseCase: GetAnimeListUseCase,
    private val searchAnimeUseCase: SearchAnimeUseCase,
    private val getNavDataUseCase: GetNavDataUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        onEvent(HomeEvent.LoadInitial)
        loadNavData()
    }

    private fun loadNavData() {
        viewModelScope.launch {
            try {
                val navData = getNavDataUseCase()
                _uiState.update { it.copy(genres = navData.genres, years = navData.years) }
            } catch (_: Exception) {
                // Nav data is optional — ignore failures
            }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.LoadInitial        -> loadInitial()
            HomeEvent.LoadMore           -> loadMore()
            HomeEvent.Refresh            -> refresh()
            HomeEvent.ClearError         -> _uiState.update { it.copy(error = null) }
            is HomeEvent.SelectSort      -> selectSort(event.sort)
            is HomeEvent.SelectType      -> selectType(event.type)
            HomeEvent.ToggleSearch       -> toggleSearch()
            is HomeEvent.SearchQueryChanged -> onSearchQueryChanged(event.query)
            HomeEvent.SearchLoadMore     -> searchLoadMore()
        }
    }

    // ── Catalog list ─────────────────────────────────────────────────────────

    private fun buildFilter() = _uiState.value.run {
        CatalogFilter(sortBy = sort, sortAscending = sortAscending, type = selectedType)
    }

    private fun loadInitial() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val items = getAnimeListUseCase(page = 1, filter = buildFilter())
                _uiState.update {
                    it.copy(animeList = items, isLoading = false, currentPage = 1, canLoadMore = items.isNotEmpty())
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Ошибка загрузки") }
            }
        }
    }

    private fun loadMore() {
        val s = _uiState.value
        if (s.isLoading || s.isLoadingMore || !s.canLoadMore) return
        val nextPage = s.currentPage + 1
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            try {
                val items = getAnimeListUseCase(page = nextPage, filter = buildFilter())
                _uiState.update {
                    it.copy(
                        animeList = (it.animeList + items).distinctBy { a -> a.id },
                        isLoadingMore = false,
                        currentPage = nextPage,
                        canLoadMore = items.isNotEmpty(),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false, error = e.message ?: "Ошибка загрузки") }
            }
        }
    }

    private fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val items = getAnimeListUseCase(page = 1, filter = buildFilter())
                val s = _uiState.value
                _uiState.update {
                    HomeUiState(
                        animeList = items,
                        isLoading = false,
                        currentPage = 1,
                        canLoadMore = items.isNotEmpty(),
                        sort = s.sort,
                        sortAscending = s.sortAscending,
                        selectedType = s.selectedType,
                        genres = s.genres,
                        years = s.years,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Ошибка загрузки") }
            }
        }
    }

    private fun selectSort(sort: SortOption) {
        val prev = _uiState.value
        val ascending = if (prev.sort == sort) !prev.sortAscending else false
        _uiState.update { it.copy(sort = sort, sortAscending = ascending, isLoading = true, animeList = emptyList()) }
        viewModelScope.launch {
            try {
                val items = getAnimeListUseCase(page = 1, filter = buildFilter())
                _uiState.update { it.copy(animeList = items, isLoading = false, currentPage = 1, canLoadMore = items.isNotEmpty()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Ошибка загрузки") }
            }
        }
    }

    private fun selectType(type: AnimeType?) {
        val prev = _uiState.value
        val newType = if (prev.selectedType == type) null else type // tap again = deselect
        _uiState.update { it.copy(selectedType = newType, isLoading = true, animeList = emptyList(), currentPage = 1) }
        viewModelScope.launch {
            try {
                val items = getAnimeListUseCase(page = 1, filter = buildFilter())
                _uiState.update { it.copy(animeList = items, isLoading = false, currentPage = 1, canLoadMore = items.isNotEmpty()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Ошибка загрузки") }
            }
        }
    }

    // ── Search ───────────────────────────────────────────────────────────────

    private fun toggleSearch() {
        val active = !_uiState.value.isSearchActive
        _uiState.update {
            it.copy(
                isSearchActive = active,
                searchQuery = "",
                searchResults = emptyList(),
                hasSearched = false,
                searchError = null,
                isSearchLoading = false,
            )
        }
        if (!active) searchJob?.cancel()
    }

    private fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank() || query.trim().length < MIN_QUERY_LENGTH) {
            _uiState.update { it.copy(searchResults = emptyList(), hasSearched = false, searchError = null) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            _uiState.update { it.copy(isSearchLoading = true, searchError = null) }
            try {
                val items = searchAnimeUseCase(query, 1)
                _uiState.update {
                    it.copy(
                        searchResults = items,
                        isSearchLoading = false,
                        searchPage = 1,
                        canSearchLoadMore = items.isNotEmpty(),
                        hasSearched = true,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearchLoading = false, searchError = e.message ?: "Ошибка поиска") }
            }
        }
    }

    private fun searchLoadMore() {
        val s = _uiState.value
        if (s.isSearchLoading || s.isSearchLoadingMore || !s.canSearchLoadMore || s.searchQuery.isBlank()) return
        val nextPage = s.searchPage + 1
        _uiState.update { it.copy(isSearchLoadingMore = true) }
        viewModelScope.launch {
            try {
                val items = searchAnimeUseCase(s.searchQuery, nextPage)
                _uiState.update {
                    it.copy(
                        searchResults = (it.searchResults + items).distinctBy { a -> a.id },
                        isSearchLoadingMore = false,
                        searchPage = nextPage,
                        canSearchLoadMore = items.isNotEmpty(),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearchLoadingMore = false, searchError = e.message ?: "Ошибка загрузки") }
            }
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 350L
        private const val MIN_QUERY_LENGTH = 4
    }
}
