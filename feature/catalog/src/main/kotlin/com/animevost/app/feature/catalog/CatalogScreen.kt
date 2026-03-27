package com.animevost.app.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CatalogScreen(
    onNavigateToFilteredList: (filterType: String, filterValue: String, filterLabel: String) -> Unit,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Каталог",
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SecondaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
                CatalogTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.title) },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                when (state.selectedTab) {
                    CatalogTab.GENRES -> GenreGrid(
                        genres = state.genres,
                        onGenreClick = { genre ->
                            onNavigateToFilteredList("genre", genre.url, genre.name)
                        },
                    )
                    CatalogTab.TYPES -> TypeGrid(
                        types = state.types,
                        onTypeClick = { type ->
                            onNavigateToFilteredList("type", type.name, type.displayName)
                        },
                    )
                    CatalogTab.YEARS -> YearGrid(
                        years = state.years,
                        onYearClick = { year ->
                            onNavigateToFilteredList("year", year, year)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreGrid(
    genres: List<com.animevost.app.core.domain.model.Genre>,
    onGenreClick: (com.animevost.app.core.domain.model.Genre) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        genres.forEach { genre ->
            FilterChip(
                selected = false,
                onClick = { onGenreClick(genre) },
                label = { Text(genre.name) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeGrid(
    types: List<com.animevost.app.core.domain.model.AnimeType>,
    onTypeClick: (com.animevost.app.core.domain.model.AnimeType) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        types.forEach { type ->
            FilterChip(
                selected = false,
                onClick = { onTypeClick(type) },
                label = { Text(type.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun YearGrid(
    years: List<String>,
    onYearClick: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        years.forEach { year ->
            FilterChip(
                selected = false,
                onClick = { onYearClick(year) },
                label = { Text(year) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
    }
}
