package com.animevost.app.feature.home

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeType
import com.animevost.app.core.domain.model.CatalogFilter
import com.animevost.app.core.domain.model.SortOption
import com.animevost.app.core.domain.usecase.GetAnimeListUseCase
import com.animevost.app.core.domain.usecase.GetNavDataUseCase
import com.animevost.app.core.domain.usecase.SearchAnimeUseCase
import com.animevost.app.core.domain.repository.AnimeRepository
import com.animevost.app.core.domain.util.BasePaginatedViewModel
import com.animevost.app.core.domain.util.Result
import com.animevost.app.core.domain.util.onError
import com.animevost.app.core.domain.util.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
class HomeViewModel @Inject constructor(
    private val getAnimeListUseCase: GetAnimeListUseCase,
    private val searchAnimeUseCase: SearchAnimeUseCase,
    private val getNavDataUseCase: GetNavDataUseCase,
    private val animeRepository: AnimeRepository,
) : BasePaginatedViewModel<AnimePreview>() {

    // Filter state backing fields — read by fetchPage, updated by selectSort/selectType.
    private var currentSort: SortOption = SortOption.DATE
    private var currentSortAscending: Boolean = false
    private var currentTypeFilter: AnimeType? = null

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var searchLoadMoreJob: Job? = null

    init {
        // Keep HomeUiState's pagination fields in sync with base class flows.
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
        loadNavData()
    }

    override suspend fun fetchPage(page: Int): List<AnimePreview> {
        val result = getAnimeListUseCase(
            page = page,
            filter = CatalogFilter(
                sortBy = currentSort,
                sortAscending = currentSortAscending,
                type = currentTypeFilter,
            ),
        )
        return when (result) {
            is Result.Success -> result.data
            is Result.Error -> throw result.exception ?: Exception(result.message)
        }
    }

    override fun mergeItems(
        existing: List<AnimePreview>,
        new: List<AnimePreview>,
    ): List<AnimePreview> = (existing + new).distinctBy { it.id }

    // ── Public event dispatch ─────────────────────────────────────────────────

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.LoadMore              -> loadMore()
            HomeEvent.Refresh               -> refresh()
            is HomeEvent.SelectSort         -> selectSort(event.sort)
            is HomeEvent.SelectType         -> selectType(event.type)
            HomeEvent.ToggleSearch          -> toggleSearch()
            is HomeEvent.SearchQueryChanged -> onSearchQueryChanged(event.query)
            HomeEvent.SearchLoadMore        -> searchLoadMore()
            HomeEvent.RandomAnime           -> loadRandomAnime()
            HomeEvent.ConsumedRandomAnime   -> _uiState.update { it.copy(randomAnimeUrl = null) }
        }
    }

    // ── Nav data ──────────────────────────────────────────────────────────────

    private fun loadNavData() {
        viewModelScope.launch {
            getNavDataUseCase()
                .onSuccess { navData ->
                    _uiState.update { it.copy(genres = navData.genres, years = navData.years) }
                }
            // Nav data is optional — ignore failures
        }
    }

    // ── Filter / sort ─────────────────────────────────────────────────────────

    private fun selectSort(sort: SortOption) {
        currentSortAscending = if (currentSort == sort) !currentSortAscending else false
        currentSort = sort
        _uiState.update { it.copy(sort = sort, sortAscending = currentSortAscending) }
        _items.value = emptyList() // clear list for immediate visual feedback
        loadInitial()
    }

    private fun selectType(type: AnimeType?) {
        currentTypeFilter = if (currentTypeFilter == type) null else type
        _uiState.update { it.copy(selectedType = currentTypeFilter) }
        _items.value = emptyList() // clear list for immediate visual feedback
        loadInitial()
    }

    // ── Search ────────────────────────────────────────────────────────────────

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
                isSearchLoadingMore = false,
                canSearchLoadMore = false,
                searchPage = 1,
            )
        }
        if (!active) {
            searchJob?.cancel()
            searchLoadMoreJob?.cancel()
        }
    }

    private fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchLoadMoreJob?.cancel()
        _uiState.update { it.copy(isSearchLoading = false, isSearchLoadingMore = false) }
        if (query.isBlank() || query.trim().length < MIN_QUERY_LENGTH) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    hasSearched = false,
                    searchError = null,
                    canSearchLoadMore = false,
                    searchPage = 1,
                )
            }
            return
        }
        val requestedQuery = query
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            _uiState.update { it.copy(isSearchLoading = true, searchError = null) }
            searchAnimeUseCase(requestedQuery, 1)
                .onSuccess { items ->
                    if (_uiState.value.searchQuery != requestedQuery) return@onSuccess
                    _uiState.update {
                        it.copy(
                            searchResults = items,
                            isSearchLoading = false,
                            searchPage = 1,
                            canSearchLoadMore = items.isNotEmpty(),
                            hasSearched = true,
                        )
                    }
                }
                .onError { _, msg ->
                    if (_uiState.value.searchQuery != requestedQuery) return@onError
                    _uiState.update { it.copy(isSearchLoading = false, searchError = msg ?: "Ошибка поиска") }
                }
        }
    }

    private fun searchLoadMore() {
        val s = _uiState.value
        if (s.isSearchLoading || s.isSearchLoadingMore || !s.canSearchLoadMore || s.searchQuery.isBlank()) return
        val nextPage = s.searchPage + 1
        val requestedQuery = s.searchQuery
        _uiState.update { it.copy(isSearchLoadingMore = true) }
        searchLoadMoreJob = viewModelScope.launch {
            searchAnimeUseCase(requestedQuery, nextPage)
                .onSuccess { items ->
                    if (_uiState.value.searchQuery != requestedQuery) return@onSuccess
                    _uiState.update {
                        it.copy(
                            searchResults = (it.searchResults + items).distinctBy { a -> a.id },
                            isSearchLoadingMore = false,
                            searchPage = nextPage,
                            canSearchLoadMore = items.isNotEmpty(),
                        )
                    }
                }
                .onError { _, msg ->
                    if (_uiState.value.searchQuery != requestedQuery) return@onError
                    _uiState.update { it.copy(isSearchLoadingMore = false, searchError = msg ?: "Ошибка загрузки") }
                }
        }
    }

    private fun loadRandomAnime() {
        if (_uiState.value.isLoadingRandom) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRandom = true) }
            animeRepository.getRandomAnimeUrl()
                .onSuccess { url -> _uiState.update { it.copy(isLoadingRandom = false, randomAnimeUrl = url) } }
                .onError { _, _ -> _uiState.update { it.copy(isLoadingRandom = false) } }
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 350L
        private const val MIN_QUERY_LENGTH = 4
    }
}
