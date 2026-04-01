package com.animevost.app.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeType
import com.animevost.app.core.domain.model.SortOption
import com.animevost.app.core.ui.components.AnimeCard
import com.animevost.app.core.ui.components.AnimeCardHorizontal
import com.animevost.app.core.ui.components.ErrorState
import com.animevost.app.core.ui.components.HomeShimmer
import com.animevost.app.core.ui.components.SortBottomSheet
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.res.painterResource
import com.animevost.app.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onAnimeClick: (String) -> Unit,
    onNavigateToFilteredList: (filterType: String, filterValue: String, filterLabel: String) -> Unit = { _, _, _ -> },
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.isSearchActive) {
        if (state.isSearchActive) focusRequester.requestFocus()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    if (state.isSearchActive) {
                        TextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.onEvent(HomeEvent.SearchQueryChanged(it)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp)
                                .focusRequester(focusRequester),
                            placeholder = { Text("Название аниме...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onEvent(HomeEvent.SearchQueryChanged("")) }) {
                                        Icon(Icons.Default.Close, "Очистить", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary,
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = UiR.drawable.animevost_logo),
                                contentDescription = null,
                                modifier = Modifier.size(38.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AnimeVost",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                actions = {
                    if (state.isSearchActive) {
                        IconButton(onClick = { viewModel.onEvent(HomeEvent.ToggleSearch) }) {
                            Icon(Icons.Default.Close, "Закрыть поиск", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        IconButton(onClick = { viewModel.onEvent(HomeEvent.ToggleSearch) }) {
                            Icon(Icons.Default.Search, "Поиск", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading && state.animeList.isEmpty() && !state.isSearchActive -> {
                HomeShimmer(modifier = Modifier.padding(innerPadding))
            }
            state.error != null && state.animeList.isEmpty() && !state.isSearchActive -> {
                ErrorState(
                    message = state.error!!,
                    onRetry = { viewModel.onEvent(HomeEvent.Refresh) },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    if (state.isSearchActive) {
                        SearchContent(
                            state = state,
                            onAnimeClick = onAnimeClick,
                            onLoadMore = { viewModel.onEvent(HomeEvent.SearchLoadMore) },
                            onNavigateToFilteredList = onNavigateToFilteredList,
                        )
                    } else {
                        CatalogContent(
                            state = state,
                            onAnimeClick = onAnimeClick,
                            onSortSelected = { viewModel.onEvent(HomeEvent.SelectSort(it)) },
                            onTypeSelected = { viewModel.onEvent(HomeEvent.SelectType(it)) },
                            onLoadMore = { viewModel.onEvent(HomeEvent.LoadMore) },
                            onRefresh = { viewModel.onEvent(HomeEvent.Refresh) },
                            onNavigateToFilteredList = onNavigateToFilteredList,
                        )
                    }

                }
            }
        }
    }
}

// ─── Catalog content ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CatalogContent(
    state: HomeUiState,
    onAnimeClick: (String) -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onTypeSelected: (AnimeType) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToFilteredList: (String, String, String) -> Unit = { _, _, _ -> },
) {
    val listState = rememberLazyListState()
    var showSortSheet by remember { mutableStateOf(false) }

    LaunchedEffect(listState, state.animeList.size) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 4
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { if (state.canLoadMore && !state.isLoadingMore) onLoadMore() }
    }

    val rows = remember(state.animeList) { state.animeList.chunked(2) }

    PullToRefreshBox(
        isRefreshing = state.isLoading && state.animeList.isNotEmpty(),
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            stickyHeader(key = "filter_header") {
                FilterBar(
                    selectedSort = state.sort,
                    sortAscending = state.sortAscending,
                    selectedType = state.selectedType,
                    onTypeSelected = onTypeSelected,
                    onSortClick = { showSortSheet = true },
                    onSectionClick = onNavigateToFilteredList,
                )
            }
            items(rows, key = { it.first().id }) { row ->
                AnimeGridRow(items = row, onAnimeClick = onAnimeClick)
            }
            if (state.isLoadingMore) {
                item(key = "loading_more") {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            selectedSort = state.sort,
            sortAscending = state.sortAscending,
            onSortSelected = { sort ->
                onSortSelected(sort)
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false },
        )
    }
}

// ─── Search content ───────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchContent(
    state: HomeUiState,
    onAnimeClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onNavigateToFilteredList: (String, String, String) -> Unit,
) {
    when {
        state.isSearchLoading && state.searchResults.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
            }
        }

        !state.hasSearched -> {
            val hasNavData = state.genres.isNotEmpty() || state.years.isNotEmpty()
            if (!hasNavData) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (state.searchQuery.isNotBlank() && state.searchQuery.trim().length < 4) "Введите минимум 4 символа" else "Введите название аниме",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    if (state.searchQuery.isNotBlank() && state.searchQuery.trim().length < 4) {
                        item {
                            Text(
                                "Введите минимум 4 символа",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                    if (state.genres.isNotEmpty()) {
                        item {
                            Text(
                                "Жанры",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                            )
                        }
                        item {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                state.genres.forEach { genre ->
                                    SuggestionChip(
                                        onClick = { onNavigateToFilteredList("genre", genre.url, genre.name) },
                                        label = { Text(genre.name, style = MaterialTheme.typography.labelLarge) },
                                        shape = RoundedCornerShape(16.dp),
                                    )
                                }
                            }
                        }
                    }
                    if (state.years.isNotEmpty()) {
                        item {
                            Text(
                                "Год",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                items(state.years) { year ->
                                    SuggestionChip(
                                        onClick = { onNavigateToFilteredList("year", year, year) },
                                        label = { Text(year, style = MaterialTheme.typography.labelLarge) },
                                        shape = RoundedCornerShape(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        state.hasSearched && state.searchResults.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("😕", style = MaterialTheme.typography.headlineLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Ничего не найдено", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("По запросу «${state.searchQuery}» совпадений нет.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }

        else -> {
            val listState = rememberLazyListState()
            val shouldLoadMore by remember {
                derivedStateOf {
                    val info = listState.layoutInfo
                    val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                    last >= info.totalItemsCount - 4
                }
            }
            LaunchedEffect(shouldLoadMore) {
                if (shouldLoadMore && state.canSearchLoadMore && !state.isSearchLoadingMore) onLoadMore()
            }

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                item {
                    Text("Результаты: ${state.searchResults.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                items(state.searchResults, key = { it.id }) { anime ->
                    AnimeCardHorizontal(anime = anime, onClick = { onAnimeClick(anime.url) })
                    HorizontalDivider(modifier = Modifier.padding(start = 84.dp, end = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                }
                if (state.isSearchLoadingMore) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── Filter bar: type chips + sort button ─────────────────────────────────────

@Composable
private fun FilterBar(
    selectedSort: SortOption,
    sortAscending: Boolean,
    selectedType: AnimeType?,
    onTypeSelected: (AnimeType) -> Unit,
    onSortClick: () -> Unit,
    onSectionClick: (String, String, String) -> Unit = { _, _, _ -> },
) {
    val bgColor = MaterialTheme.colorScheme.background

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Type chips — horizontal scroll with soft right fade
        val fadeBrush = Brush.horizontalGradient(
            0.0f to Color.Black,
            0.88f to Color.Black,
            1.0f to Color.Transparent,
        )
        LazyRow(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(brush = fadeBrush, blendMode = BlendMode.DstIn)
                },
            contentPadding = PaddingValues(start = 12.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Section chips: Онгоинги, Анонсы
            item(key = "ongoing") {
                FilterChip(
                    selected = false,
                    onClick = { onSectionClick("path", "ongoing/", "Онгоинги") },
                    label = {
                        Text("Онгоинги", style = MaterialTheme.typography.labelLarge)
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = Color.Transparent,
                    ),
                )
            }
            item(key = "preview") {
                FilterChip(
                    selected = false,
                    onClick = { onSectionClick("path", "preview/", "Анонсы") },
                    label = {
                        Text("Анонсы", style = MaterialTheme.typography.labelLarge)
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = Color.Transparent,
                    ),
                )
            }
            items(AnimeType.entries.filter { it != AnimeType.UNKNOWN }) { type ->
                val isSelected = selectedType == type
                FilterChip(
                    selected = isSelected,
                    onClick = { onTypeSelected(type) },
                    label = {
                        Text(
                            typeShortName(type),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color.Transparent,
                        selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    ),
                )
            }
        }

        // Sort button — circle icon
        Surface(
            onClick = onSortClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(end = 12.dp).size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = "Сортировка",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun typeShortName(type: AnimeType) = when (type) {
    AnimeType.FULL_MOVIE -> "Фильм"
    AnimeType.SHORT_MOVIE -> "К/М"
    else -> type.displayName
}

// ─── 2-column card row ────────────────────────────────────────────────────────

@Composable
private fun AnimeGridRow(
    items: List<AnimePreview>,
    onAnimeClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { anime ->
            AnimeCard(anime = anime, onClick = { onAnimeClick(anime.url) }, modifier = Modifier.weight(1f))
        }
        if (items.size == 1) Spacer(modifier = Modifier.weight(1f))
    }
}
