package com.animevost.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.animevost.app.core.ui.components.AnimeCard
import com.animevost.app.core.ui.components.ErrorState
import com.animevost.app.core.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAnimeClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AnimeVost",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading && state.animeList.isEmpty() -> {
                LoadingState(modifier = Modifier.padding(innerPadding))
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
                        val layoutInfo = gridState.layoutInfo
                        val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        lastVisible >= layoutInfo.totalItemsCount - 4
                    }
                }

                LaunchedEffect(shouldLoadMore) {
                    if (shouldLoadMore && state.canLoadMore) {
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
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = state.animeList,
                            key = { it.id },
                        ) { anime ->
                            AnimeCard(
                                anime = anime,
                                onClick = { onAnimeClick(anime.url) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        if (state.isLoadingMore) {
                            item(span = { GridItemSpan(2) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
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
