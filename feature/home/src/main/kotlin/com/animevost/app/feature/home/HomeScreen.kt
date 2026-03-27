package com.animevost.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.animevost.app.core.ui.components.ErrorState
import com.animevost.app.core.ui.components.HomeShimmer
import com.animevost.app.core.ui.components.SectionHeader
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAnimeClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                val gridState = rememberLazyGridState()

                val shouldLoadMore by remember {
                    derivedStateOf {
                        val info = gridState.layoutInfo
                        val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                        last >= info.totalItemsCount - 6
                    }
                }

                LaunchedEffect(shouldLoadMore) {
                    if (shouldLoadMore && state.canLoadMore && !state.isLoadingMore) {
                        viewModel.onEvent(HomeEvent.LoadMore)
                    }
                }

                PullToRefreshBox(
                    isRefreshing = state.isLoading && state.animeList.isNotEmpty(),
                    onRefresh = { viewModel.onEvent(HomeEvent.Refresh) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = gridState,
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // Featured carousel — top 3 items
                        if (state.animeList.isNotEmpty()) {
                            item(span = { GridItemSpan(3) }) {
                                FeaturedCarousel(
                                    items = state.animeList.take(3),
                                    onAnimeClick = onAnimeClick,
                                )
                            }
                        }

                        // Sort chips
                        item(span = { GridItemSpan(3) }) {
                            SortChipsRow(
                                selectedSort = state.sort,
                                sortAscending = state.sortAscending,
                                onSortSelected = { viewModel.onEvent(HomeEvent.SelectSort(it)) },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }

                        // Section header
                        item(span = { GridItemSpan(3) }) {
                            SectionHeader(title = "Все аниме")
                        }

                        // 3-column grid
                        items(
                            items = state.animeList,
                            key = { it.id },
                        ) { anime ->
                            AnimeCard(
                                anime = anime,
                                onClick = { onAnimeClick(anime.url) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = if (state.animeList.indexOf(anime) % 3 == 0) 12.dp else 0.dp,
                                        end = if (state.animeList.indexOf(anime) % 3 == 2) 12.dp else 0.dp,
                                    ),
                            )
                        }

                        // Load-more indicator
                        if (state.isLoadingMore) {
                            item(span = { GridItemSpan(3) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        repeat(3) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
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
}

@Composable
private fun FeaturedCarousel(
    items: List<AnimePreview>,
    onAnimeClick: (String) -> Unit,
) {
    val pagerState = rememberPagerState { items.size }

    // Auto-advance every 4 seconds
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

        // Page indicator dots
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
