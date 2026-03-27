package com.animevost.app.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.SortOption
import com.animevost.app.core.ui.components.AnimeCard
import com.animevost.app.core.ui.components.AnimeCardFeatured
import com.animevost.app.core.ui.components.AnimeCardHorizontal
import com.animevost.app.core.ui.components.ErrorState
import com.animevost.app.core.ui.components.HomeShimmer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onAnimeClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var isGridMode by rememberSaveable { mutableStateOf(true) }
    val layoutColumns = if (isGridMode) 2 else 1

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AnimeVost",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                actions = {
                    IconButton(onClick = { /* profile */ }) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Профиль",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading && state.animeList.isEmpty() -> {
                HomeShimmer(modifier = Modifier.padding(innerPadding))
            }
            state.error != null && state.animeList.isEmpty() -> {
                ErrorState(
                    message = state.error!!,
                    onRetry = { viewModel.onEvent(HomeEvent.Refresh) },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            else -> {
                val listState = rememberLazyListState()

                // Reliable infinite scroll
                LaunchedEffect(listState, state.animeList.size) {
                    snapshotFlow {
                        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val total = listState.layoutInfo.totalItemsCount
                        total > 0 && last >= total - 4
                    }
                        .distinctUntilChanged()
                        .filter { it }
                        .collect {
                            if (state.canLoadMore && !state.isLoadingMore) {
                                viewModel.onEvent(HomeEvent.LoadMore)
                            }
                        }
                }

                val rows = remember(state.animeList, layoutColumns) {
                    state.animeList.chunked(layoutColumns)
                }

                PullToRefreshBox(
                    isRefreshing = state.isLoading && state.animeList.isNotEmpty(),
                    onRefresh = { viewModel.onEvent(HomeEvent.Refresh) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // Sticky sort + layout row
                        stickyHeader(key = "sort_header") {
                            SortAndLayoutRow(
                                selectedSort = state.sort,
                                sortAscending = state.sortAscending,
                                onSortSelected = { viewModel.onEvent(HomeEvent.SelectSort(it)) },
                                isGridMode = isGridMode,
                                onToggleLayout = { isGridMode = !isGridMode },
                            )
                        }

                        // Card rows
                        items(rows, key = { it.first().id }) { row ->
                            AnimeRow(
                                items = row,
                                columns = layoutColumns,
                                onAnimeClick = onAnimeClick,
                            )
                        }

                        // Load-more spinner
                        if (state.isLoadingMore) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
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
        }
    }
}

// ─── Sort chips + layout icons in one sticky row ──────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortAndLayoutRow(
    selectedSort: SortOption,
    sortAscending: Boolean,
    onSortSelected: (SortOption) -> Unit,
    isGridMode: Boolean,
    onToggleLayout: () -> Unit,
) {
    val bgColor = MaterialTheme.colorScheme.background
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Sort chips — scrollable, takes remaining space
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SortOption.entries.forEach { option ->
                    val isSelected = selectedSort == option
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSortSelected(option) },
                        label = {
                            Text(
                                text = if (isSelected) {
                                    "${option.displayName} ${if (sortAscending) "↑" else "↓"}"
                                } else {
                                    option.displayName
                                },
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
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

            // Soft fade shadow on the right edge of chip row
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(40.dp)
                    .height(48.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, bgColor),
                        ),
                    ),
            )
        }

        // List / Grid toggle icon button
        IconButton(
            onClick = onToggleLayout,
            modifier = Modifier.padding(end = 4.dp),
        ) {
            Icon(
                imageVector = if (isGridMode) Icons.Default.ViewList else Icons.Default.GridView,
                contentDescription = if (isGridMode) "Список" else "Сетка",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ─── Card row (replaces LazyVerticalGrid rows) ────────────────────────────────

@Composable
private fun AnimeRow(
    items: List<AnimePreview>,
    columns: Int,
    onAnimeClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { anime ->
            if (columns == 1) {
                AnimeCardHorizontal(
                    anime = anime,
                    onClick = { onAnimeClick(anime.url) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                AnimeCard(
                    anime = anime,
                    onClick = { onAnimeClick(anime.url) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // Fill remaining slots in last row
        repeat(columns - items.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// ─── Carousel ────────────────────────────────────────────────────────────────

@Composable
private fun FeaturedCarousel(
    items: List<AnimePreview>,
    onAnimeClick: (String) -> Unit,
) {
    val pagerState = rememberPagerState { items.size }

    LaunchedEffect(pagerState.pageCount) {
        while (true) {
            delay(4_000)
            val next = (pagerState.currentPage + 1) % pagerState.pageCount
            pagerState.animateScrollToPage(next)
        }
    }

    Box {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            AnimeCardFeatured(
                anime = items[page],
                onClick = { onAnimeClick(items[page].url) },
            )
        }

        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                repeat(items.size) { index ->
                    val isActive = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 8.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.4f),
                            ),
                    )
                }
            }
        }
    }
}
