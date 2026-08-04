package com.animevost.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.ui.components.LoadingState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    onAnimeClick: (String) -> Unit,
    onContinueClick: (ContinueWatchingItem) -> Unit,
    viewModel: CollectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val showDashboard = !searchActive
    val catalogHeaderIndex = if (showDashboard) 2 else 0

    LaunchedEffect(searchActive) {
        if (searchActive) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    if (showSortSheet) {
        LibrarySortSheet(
            selectedSort = state.sort,
            sortAscending = state.sortAscending,
            onSortSelected = { viewModel.onEvent(LibraryEvent.SortSelected(it)) },
            onDirectionChanged = { viewModel.onEvent(LibraryEvent.SortDirectionChanged(it)) },
            onDismiss = { showSortSheet = false },
        )
    }

    if (showFilterSheet) {
        LibraryFilterSheet(
            selectedFilter = state.selectedFilter,
            statusCounts = state.statusCounts,
            favoritesCount = state.favoritesCount,
            totalCount = state.totalCount,
            onFilterSelected = { filter ->
                viewModel.onEvent(LibraryEvent.FilterSelected(filter))
                scope.launch { gridState.scrollToItem(catalogHeaderIndex) }
            },
            onDismiss = { showFilterSheet = false },
        )
    }

    Scaffold(
        topBar = {
            LibraryTopBar(
                totalCount = state.totalCount,
                continueCount = state.continueWatching.size,
                searchActive = searchActive,
                query = state.query,
                focusRequester = focusRequester,
                onQueryChange = { viewModel.onEvent(LibraryEvent.QueryChanged(it)) },
                onOpenSearch = { searchActive = true },
                onCloseSearch = {
                    viewModel.onEvent(LibraryEvent.QueryChanged(""))
                    searchActive = false
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    scope.launch { gridState.scrollToItem(0) }
                },
                onClearQuery = { viewModel.onEvent(LibraryEvent.QueryChanged("")) },
                onSearchDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
                onSortClick = { showSortSheet = true },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            LibraryContent(
                state = state,
                gridState = gridState,
                contentPadding = innerPadding,
                showDashboard = showDashboard,
                onAnimeClick = onAnimeClick,
                onContinueClick = onContinueClick,
                onAnimeVisible = viewModel::hydratePreview,
                onFilterSelected = { filter ->
                    viewModel.onEvent(LibraryEvent.FilterSelected(filter))
                    scope.launch { gridState.scrollToItem(catalogHeaderIndex) }
                },
                onFilterClick = { showFilterSheet = true },
                onReset = {
                    if (state.query.isNotBlank()) {
                        viewModel.onEvent(LibraryEvent.QueryChanged(""))
                    } else {
                        viewModel.onEvent(LibraryEvent.FilterSelected(LibraryFilter.All))
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    totalCount: Int,
    continueCount: Int,
    searchActive: Boolean,
    query: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onClearQuery: () -> Unit,
    onSearchDone: () -> Unit,
    onSortClick: () -> Unit,
) {
    if (searchActive) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onCloseSearch) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Закрыть поиск",
                    )
                }
            },
            title = {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("Поиск в библиотеке") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = onClearQuery) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить поиск")
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                    shape = MaterialTheme.shapes.extraLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchDone() }),
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )
    } else {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Библиотека",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = libraryStatsLabel(totalCount, continueCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = {
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                }
                IconButton(onClick = onSortClick) {
                    Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Сортировка")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
    }
}

@Composable
private fun LibraryContent(
    state: LibraryUiState,
    gridState: LazyGridState,
    contentPadding: PaddingValues,
    showDashboard: Boolean,
    onAnimeClick: (String) -> Unit,
    onContinueClick: (ContinueWatchingItem) -> Unit,
    onAnimeVisible: (AnimePreview) -> Unit,
    onFilterSelected: (LibraryFilter) -> Unit,
    onFilterClick: () -> Unit,
    onReset: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 152.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        state = gridState,
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showDashboard) {
            item(
                key = "continue-watching",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "dashboard",
            ) {
                if (state.continueWatching.isNotEmpty()) {
                    ContinueWatchingSection(
                        items = state.continueWatching,
                        onContinueClick = onContinueClick,
                    )
                } else {
                    Spacer(modifier = Modifier.height(0.dp))
                }
            }
            item(
                key = "status-lists",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "dashboard",
            ) {
                StatusTilesGrid(
                    statusCounts = state.statusCounts,
                    favoritesCount = state.favoritesCount,
                    selectedFilter = state.selectedFilter,
                    onTileClick = onFilterSelected,
                )
            }
        }

        item(
            key = "catalog-toolbar",
            span = { GridItemSpan(maxLineSpan) },
            contentType = "toolbar",
        ) {
            CatalogToolbar(
                selectedFilter = state.selectedFilter,
                visibleCount = state.items.size,
                onFilterClick = onFilterClick,
            )
        }

        if (state.items.isEmpty()) {
            item(
                key = "empty",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "empty",
            ) {
                LibraryEmpty(
                    hasQuery = state.query.isNotBlank(),
                    filter = state.selectedFilter,
                    onReset = onReset,
                )
            }
        } else {
            items(
                items = state.items,
                key = { "grid-${it.anime.id}-${it.anime.url}" },
                contentType = { "grid-card" },
            ) { item ->
                LaunchedEffect(item.anime.id, item.anime.title) {
                    onAnimeVisible(item.anime)
                }
                LibraryGridCard(
                    item = item,
                    onClick = { onAnimeClick(item.anime.url) },
                )
            }
        }
    }
}
