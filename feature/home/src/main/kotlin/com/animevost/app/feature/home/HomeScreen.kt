package com.animevost.app.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
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
import com.animevost.app.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAnimeClick: (String) -> Unit,
    onNavigateToFilteredList: (filterType: String, filterValue: String, filterLabel: String) -> Unit = { _, _, _ -> },
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.isSearchActive) {
        if (state.isSearchActive) focusRequester.requestFocus()
    }

    LaunchedEffect(state.randomAnimeUrl) {
        state.randomAnimeUrl?.let { url ->
            onAnimeClick(url)
            viewModel.onEvent(HomeEvent.ConsumedRandomAnime)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            if (!state.isSearchActive) {
                FloatingActionButton(
                    onClick = { viewModel.onEvent(HomeEvent.RandomAnime) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    if (state.isLoadingRandom) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Casino,
                            contentDescription = "Случайное аниме",
                        )
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    if (state.isSearchActive) {
                        TextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.onEvent(HomeEvent.SearchQueryChanged(it)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 4.dp)
                                .focusRequester(focusRequester),
                            placeholder = {
                                Text(
                                    "Название аниме...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onEvent(HomeEvent.SearchQueryChanged("")) }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Очистить",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
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
                                painter = painterResource(UiR.drawable.animevost_logo),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
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
                        TextButton(
                            onClick = {
                                keyboardController?.hide()
                                viewModel.onEvent(HomeEvent.ToggleSearch)
                            },
                        ) {
                            Text(
                                "Отмена",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    } else {
                        IconButton(onClick = { viewModel.onEvent(HomeEvent.ToggleSearch) }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Поиск",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var filtersVisible by remember { mutableStateOf(true) }

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

    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val atTop = index == 0 && offset <= 8
                val scrollingDown = index > previousIndex || (index == previousIndex && offset > previousOffset + 8)
                val scrollingUp = index < previousIndex || (index == previousIndex && offset < previousOffset - 4)

                when {
                    atTop -> filtersVisible = true
                    scrollingDown -> filtersVisible = false
                    scrollingUp -> filtersVisible = true
                }

                previousIndex = index
                previousOffset = offset
            }
    }

    val rows = remember(state.animeList) { state.animeList.chunked(2) }

    PullToRefreshBox(
        isRefreshing = state.isLoading && state.animeList.isNotEmpty(),
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = filtersVisible,
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = 180),
                    initialOffsetY = { -it },
                ) + expandVertically(
                    animationSpec = tween(durationMillis = 180),
                    expandFrom = Alignment.Top,
                ) + fadeIn(animationSpec = tween(durationMillis = 120)),
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = 160),
                    targetOffsetY = { -it },
                ) + shrinkVertically(
                    animationSpec = tween(durationMillis = 160),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(animationSpec = tween(durationMillis = 100)),
            ) {
                FilterBar(
                    selectedSort = state.sort,
                    sortAscending = state.sortAscending,
                    selectedType = state.selectedType,
                    onTypeSelected = onTypeSelected,
                    onSortClick = { showSortSheet = true },
                    onSectionClick = onNavigateToFilteredList,
                )
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(rows, key = { it.first().id }) { row ->
                    AnimeGridRow(items = row, onAnimeClick = onAnimeClick)
                }
                if (state.isLoadingMore) {
                    item(key = "loading_more") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                            )
                        }
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
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        !state.hasSearched -> {
            val hasNavData = state.genres.isNotEmpty() || state.years.isNotEmpty()
            if (!hasNavData) {
                SearchHint(query = state.searchQuery)
            } else {
                SearchBrowseContent(
                    state = state,
                    onNavigateToFilteredList = onNavigateToFilteredList,
                )
            }
        }

        state.hasSearched && state.searchResults.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp),
                ) {
                    Text("😕", style = MaterialTheme.typography.headlineLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Ничего не найдено",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "По запросу «${state.searchQuery}» совпадений нет.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
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

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp, 14.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Результаты: ${state.searchResults.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.searchResults, key = { it.id }) { anime ->
                    AnimeCardHorizontal(anime = anime, onClick = { onAnimeClick(anime.url) })
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 84.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    )
                }
                if (state.isSearchLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Search: empty hint ───────────────────────────────────────────────────────

@Composable
private fun SearchHint(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (query.isNotBlank() && query.trim().length < 4)
                    "Введите минимум 4 символа"
                else
                    "Введите название аниме",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Search: browse genres / years with tabs ──────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchBrowseContent(
    state: HomeUiState,
    onNavigateToFilteredList: (String, String, String) -> Unit,
) {
    val hasBoth = state.genres.isNotEmpty() && state.years.isNotEmpty()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Short-query warning
        if (state.searchQuery.isNotBlank() && state.searchQuery.trim().length < 4) {
            Text(
                "Введите минимум 4 символа",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // iOS-style segmented control — only when both data sets are available
        if (hasBoth) {
            SearchSegmentedControl(
                tabs = listOf("Жанры", "Годы"),
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }

        // Animated tab content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    slideInHorizontally { it * dir } togetherWith slideOutHorizontally { -it * dir }
                },
                modifier = Modifier.fillMaxSize(),
                label = "browse_tab",
            ) { tab ->
                val showGenres = !hasBoth || tab == 0
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 80.dp),
                ) {
                    if (showGenres && state.genres.isNotEmpty()) {
                        if (!hasBoth) {
                            BrowseSectionHeader("Жанры")
                        }
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.genres.forEach { genre ->
                                GenreBrowseChip(
                                    name = genre.name,
                                    onClick = { onNavigateToFilteredList("genre", genre.url, genre.name) },
                                )
                            }
                        }
                    }
                    if (!showGenres && state.years.isNotEmpty()) {
                        if (!hasBoth) {
                            BrowseSectionHeader("Год выхода")
                        }
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.years.forEach { year ->
                                YearBrowseTile(
                                    year = year,
                                    onClick = { onNavigateToFilteredList("year", year, year) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSegmentedControl(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
    ) {
        tabs.forEachIndexed { index, title ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selectedIndex == index) MaterialTheme.colorScheme.surface
                        else Color.Transparent,
                    )
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selectedIndex == index) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selectedIndex == index)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BrowseSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 2.dp),
    )
}

@Composable
private fun GenreBrowseChip(name: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun YearBrowseTile(year: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.width(72.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
            Text(
                year,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 0.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
            item(key = "ongoing") {
                FilterChip(
                    selected = false,
                    onClick = { onSectionClick("path", "ongoing/", "Онгоинги") },
                    label = { Text("Онгоинги", style = MaterialTheme.typography.labelLarge) },
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
                    label = { Text("Анонсы", style = MaterialTheme.typography.labelLarge) },
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
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color(0xFF111111),
                        selectedLeadingIconColor = Color(0xFF111111),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color.Transparent,
                        selectedBorderColor = Color.Transparent,
                    ),
                )
            }
        }

        // Sort pill showing current selection
        Surface(
            onClick = onSortClick,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .padding(end = 12.dp)
                .height(32.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp),
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Сортировка",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    sortShortName(selectedSort, sortAscending),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

private fun sortShortName(sort: SortOption, ascending: Boolean): String {
    val arrow = if (ascending) "↑" else "↓"
    return when (sort) {
        SortOption.DATE     -> "Дата $arrow"
        SortOption.RATING   -> "Рейт $arrow"
        SortOption.VIEWS    -> "Просм $arrow"
        SortOption.COMMENTS -> "Комм $arrow"
        SortOption.TITLE    -> "А-Я $arrow"
    }
}

// ─── 2-column card row ────────────────────────────────────────────────────────

@Composable
private fun AnimeGridRow(
    items: List<AnimePreview>,
    onAnimeClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { anime ->
            AnimeCard(anime = anime, onClick = { onAnimeClick(anime.url) }, modifier = Modifier.weight(1f))
        }
        if (items.size == 1) Spacer(modifier = Modifier.weight(1f))
    }
}
