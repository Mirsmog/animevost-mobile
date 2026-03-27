package com.animevost.app.feature.home

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAnimeClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var layoutColumns by rememberSaveable { mutableStateOf(3) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Prevent double window inset consumption (outer Scaffold already handles it)
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
                    LayoutSelector(
                        columns = layoutColumns,
                        onSelect = { layoutColumns = it },
                    )
                    Spacer(Modifier.width(4.dp))
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading && state.animeList.isEmpty() -> {
                    HomeShimmer()
                }
                state.error != null && state.animeList.isEmpty() -> {
                    ErrorState(
                        message = state.error!!,
                        onRetry = { viewModel.onEvent(HomeEvent.Refresh) },
                    )
                }
                else -> {
                    // Sticky sort chips — outside the grid, always visible at top
                    SortChipsRow(
                        selectedSort = state.sort,
                        sortAscending = state.sortAscending,
                        onSortSelected = { viewModel.onEvent(HomeEvent.SelectSort(it)) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )

                    val gridState = rememberLazyGridState()

                    // Reliable infinite scroll: restart when list grows, emit only on false→true edge
                    LaunchedEffect(gridState, state.animeList.size) {
                        snapshotFlow {
                            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            val total = gridState.layoutInfo.totalItemsCount
                            total > 0 && last >= total - 8
                        }
                            .distinctUntilChanged()
                            .filter { it }
                            .collect {
                                if (state.canLoadMore && !state.isLoadingMore) {
                                    viewModel.onEvent(HomeEvent.LoadMore)
                                }
                            }
                    }

                    PullToRefreshBox(
                        isRefreshing = state.isLoading && state.animeList.isNotEmpty(),
                        onRefresh = { viewModel.onEvent(HomeEvent.Refresh) },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(layoutColumns),
                            state = gridState,
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                bottom = 16.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            // Featured carousel spans full width
                            if (state.animeList.isNotEmpty()) {
                                item(span = { GridItemSpan(layoutColumns) }) {
                                    FeaturedCarousel(
                                        items = state.animeList.take(3),
                                        onAnimeClick = onAnimeClick,
                                    )
                                }
                            }

                            // Anime cards — horizontal for 1-col, grid card for 2/3-col
                            items(
                                items = state.animeList,
                                key = { it.id },
                            ) { anime ->
                                if (layoutColumns == 1) {
                                    AnimeCardHorizontal(
                                        anime = anime,
                                        onClick = { onAnimeClick(anime.url) },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                } else {
                                    AnimeCard(
                                        anime = anime,
                                        onClick = { onAnimeClick(anime.url) },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }

                            // Load-more spinner
                            if (state.isLoadingMore) {
                                item(span = { GridItemSpan(layoutColumns) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
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
}

@Composable
private fun LayoutSelector(
    columns: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(1, 2, 3).forEach { cols ->
            val isActive = columns == cols
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                    )
                    .clickable { onSelect(cols) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = cols.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortChipsRow(
    selectedSort: SortOption,
    sortAscending: Boolean,
    onSortSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
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
}

