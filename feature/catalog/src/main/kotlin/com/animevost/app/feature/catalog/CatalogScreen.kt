package com.animevost.app.feature.catalog

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.animevost.app.core.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CatalogScreen(
    onNavigateToFilteredList: (filterType: String, filterValue: String, filterLabel: String) -> Unit,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Каталог",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BrowseCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.PlayCircle,
                    label = "Онгоинги",
                    onClick = { onNavigateToFilteredList("path", "ongoing/", "Онгоинги") },
                )
                BrowseCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.NewReleases,
                    label = "Анонсы",
                    onClick = { onNavigateToFilteredList("path", "preview/", "Анонсы") },
                )
            }

            SecondaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
                CatalogTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                tab.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (state.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                when (state.selectedTab) {
                    CatalogTab.GENRES -> {
                        item {
                            SectionHeader(title = "Жанры")
                        }
                        item {
                            GenreCardGrid(
                                genres = state.genres,
                                onGenreClick = { genre ->
                                    onNavigateToFilteredList("genre", genre.url, genre.name)
                                },
                            )
                        }
                    }
                    CatalogTab.TYPES -> {
                        item {
                            SectionHeader(title = "Тип")
                        }
                        item {
                            ChipWrap {
                                state.types.forEach { type ->
                                    CatalogChip(
                                        label = type.displayName,
                                        onClick = {
                                            onNavigateToFilteredList("type", type.name, type.displayName)
                                        },
                                    )
                                }
                            }
                        }
                    }
                    CatalogTab.YEARS -> {
                        item {
                            SectionHeader(title = "Год выхода")
                        }
                        item {
                            YearRow(
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
    }
}

// 2-column genre card grid with accent color backgrounds
@Composable
private fun BrowseCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun GenreCardGrid(
    genres: List<com.animevost.app.core.domain.model.Genre>,
    onGenreClick: (com.animevost.app.core.domain.model.Genre) -> Unit,
) {
    val genreColors = listOf(
        Color(0xFF3D1A00), Color(0xFF001A3D), Color(0xFF1A003D), Color(0xFF003D1A),
        Color(0xFF3D003D), Color(0xFF1A1A00), Color(0xFF003D3D), Color(0xFF3D1A1A),
    )
    Column(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        genres.chunked(2).forEachIndexed { rowIndex, rowGenres ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowGenres.forEachIndexed { colIndex, genre ->
                    val colorIndex = (rowIndex * 2 + colIndex) % genreColors.size
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(genreColors[colorIndex])
                            .clickable { onGenreClick(genre) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = genre.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
                // Fill empty space if odd number of genres
                if (rowGenres.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun YearRow(
    years: List<String>,
    onYearClick: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(years) { year ->
            CatalogChip(label = year, onClick = { onYearClick(year) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipWrap(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = { content() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogChip(label: String, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = RoundedCornerShape(16.dp),
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
