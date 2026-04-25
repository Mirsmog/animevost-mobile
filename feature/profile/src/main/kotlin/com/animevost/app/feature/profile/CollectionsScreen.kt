package com.animevost.app.feature.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.animevost.app.core.domain.model.AnimeStatus
import com.animevost.app.core.domain.model.LibraryViewMode
import com.animevost.app.core.domain.model.SortOption
import com.animevost.app.core.ui.components.AnimeCard
import com.animevost.app.core.ui.components.AnimeCardHorizontal
import com.animevost.app.core.ui.components.LoadingState
import com.animevost.app.core.ui.theme.StatusFavorite
import com.animevost.app.core.ui.theme.accentColor
import kotlinx.coroutines.launch

private const val SECTION_INDEX_ALL = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    onAnimeClick: (String) -> Unit,
    viewModel: CollectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showSortSheet by remember { mutableStateOf(false) }
    var showSearchField by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    if (showSortSheet) {
        LibrarySortSheet(
            selectedSort = state.sort,
            sortAscending = state.sortAscending,
            onSortSelected = {
                viewModel.onEvent(LibraryEvent.SortSelected(it))
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false },
        )
    }

    Scaffold(
        topBar = {
            LibraryTopBar(
                totalCount = state.totalCount,
                continueCount = state.continueWatching.size,
                showSearchField = showSearchField,
                query = state.query,
                onQueryChange = { viewModel.onEvent(LibraryEvent.QueryChanged(it)) },
                onSearchToggle = {
                    if (showSearchField) {
                        viewModel.onEvent(LibraryEvent.QueryChanged(""))
                    }
                    showSearchField = !showSearchField
                },
                onSortClick = { showSortSheet = true },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            state.isLoading -> {
                LoadingState(modifier = Modifier.padding(innerPadding))
            }

            else -> {
                LibraryContent(
                    state = state,
                    listState = listState,
                    contentPadding = innerPadding,
                    onAnimeClick = onAnimeClick,
                    onFilterSelected = { filter ->
                        viewModel.onEvent(LibraryEvent.FilterSelected(filter))
                        scope.launch { listState.animateScrollToItem(SECTION_INDEX_ALL) }
                    },
                    onViewModeToggled = { viewModel.onEvent(LibraryEvent.ViewModeToggled) },
                )
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    totalCount: Int,
    continueCount: Int,
    showSearchField: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onSortClick: () -> Unit,
) {
    TopAppBar(
        title = {
            if (showSearchField) {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp),
                    placeholder = { Text("Поиск в библиотеке") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {}),
                )
            } else {
                Column {
                    Text(
                        text = "Моя библиотека",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = libraryStatsLabel(totalCount, continueCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        actions = {
            if (showSearchField) {
                IconButton(onClick = onSearchToggle) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть поиск")
                }
            } else {
                IconButton(onClick = onSearchToggle) {
                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                }
                IconButton(onClick = onSortClick) {
                    Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Сортировка")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

// ── Content ───────────────────────────────────────────────────────────────────

@Composable
private fun LibraryContent(
    state: LibraryUiState,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onAnimeClick: (String) -> Unit,
    onFilterSelected: (LibraryFilter) -> Unit,
    onViewModeToggled: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        state = listState,
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // index 0 — continue rail (or empty placeholder)
        item(key = "continue-rail") {
            if (state.continueWatching.isNotEmpty()) {
                ContinueWatchingSection(
                    items = state.continueWatching,
                    onAnimeClick = onAnimeClick,
                )
            } else {
                Spacer(Modifier.height(0.dp))
            }
        }

        // index 1 — status tiles
        item(key = "status-tiles") {
            StatusTilesGrid(
                statusCounts = state.statusCounts,
                favoritesCount = state.favoritesCount,
                selectedFilter = state.selectedFilter,
                onTileClick = onFilterSelected,
            )
        }

        // index 2 — all section header (with toggle + chips)
        item(key = "all-header") {
            AllSectionHeader(
                viewMode = state.viewMode,
                onViewModeToggled = onViewModeToggled,
                selectedFilter = state.selectedFilter,
                statusCounts = state.statusCounts,
                favoritesCount = state.favoritesCount,
                totalCount = state.totalCount,
                onFilterSelected = onFilterSelected,
            )
        }

        // index 3+ — content
        if (state.items.isEmpty()) {
            item(key = "empty") {
                LibraryEmpty(
                    hasQuery = state.query.isNotBlank(),
                    filter = state.selectedFilter,
                )
            }
        } else if (state.viewMode == LibraryViewMode.GRID) {
            // 3 cards per row
            val rows = state.items.chunked(3)
            items(items = rows, key = { row -> "grid-row-${row.first().anime.id}" }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { item ->
                        Box(modifier = Modifier.weight(1f)) {
                            LibraryGridCard(
                                item = item,
                                onClick = { onAnimeClick(item.anime.url) },
                            )
                        }
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            items(
                items = state.items,
                key = { "list-${it.anime.id}-${it.anime.url}" },
            ) { item ->
                AnimeCardHorizontal(
                    anime = item.anime,
                    onClick = { onAnimeClick(item.anime.url) },
                    trailingContent = {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            item.listStatus?.let { status ->
                                StatusPill(status = status)
                            }
                            if (item.isFavorite) {
                                Icon(
                                    Icons.Filled.Favorite,
                                    contentDescription = "В избранном",
                                    modifier = Modifier.size(14.dp),
                                    tint = StatusFavorite,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

// ── Continue watching ─────────────────────────────────────────────────────────

@Composable
private fun ContinueWatchingSection(
    items: List<ContinueWatchingItem>,
    onAnimeClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = "Продолжить смотреть",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = items, key = { it.anime.id }) { item ->
                ContinueWatchingCard(
                    item = item,
                    onClick = { onAnimeClick(item.anime.url) },
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.67f)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.anime.posterUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build(),
                contentDescription = item.anime.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            // Play overlay
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(38.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            // Episode badge
            if (item.episodeIndex > 0) {
                Text(
                    text = "Эп. ${item.episodeIndex}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            // Progress bar
            val animatedProgress by animateFloatAsState(
                targetValue = item.progressFraction,
                label = "progress",
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.3f),
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
        Text(
            text = item.anime.title.ifBlank { item.anime.titleOriginal },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.episodeName.isNotBlank()) {
            Text(
                text = item.episodeName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Status tiles 2×3 ──────────────────────────────────────────────────────────

private data class StatusTileSpec(
    val filter: LibraryFilter,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val count: Int,
)

@Composable
private fun StatusTilesGrid(
    statusCounts: Map<AnimeStatus, Int>,
    favoritesCount: Int,
    selectedFilter: LibraryFilter,
    onTileClick: (LibraryFilter) -> Unit,
) {
    val tiles = remember(statusCounts, favoritesCount) {
        listOf(
            StatusTileSpec(
                filter = LibraryFilter.Status(AnimeStatus.WATCHING),
                label = AnimeStatus.WATCHING.label,
                icon = Icons.Outlined.PlayCircle,
                color = AnimeStatus.WATCHING.accentColor(),
                count = statusCounts[AnimeStatus.WATCHING] ?: 0,
            ),
            StatusTileSpec(
                filter = LibraryFilter.Status(AnimeStatus.PLANNED),
                label = AnimeStatus.PLANNED.label,
                icon = Icons.Outlined.Bookmark,
                color = AnimeStatus.PLANNED.accentColor(),
                count = statusCounts[AnimeStatus.PLANNED] ?: 0,
            ),
            StatusTileSpec(
                filter = LibraryFilter.Status(AnimeStatus.WATCHED),
                label = AnimeStatus.WATCHED.label,
                icon = Icons.Outlined.CheckCircle,
                color = AnimeStatus.WATCHED.accentColor(),
                count = statusCounts[AnimeStatus.WATCHED] ?: 0,
            ),
            StatusTileSpec(
                filter = LibraryFilter.Status(AnimeStatus.ON_HOLD),
                label = AnimeStatus.ON_HOLD.label,
                icon = Icons.Outlined.PauseCircle,
                color = AnimeStatus.ON_HOLD.accentColor(),
                count = statusCounts[AnimeStatus.ON_HOLD] ?: 0,
            ),
            StatusTileSpec(
                filter = LibraryFilter.Status(AnimeStatus.DROPPED),
                label = AnimeStatus.DROPPED.label,
                icon = Icons.Outlined.RemoveCircle,
                color = AnimeStatus.DROPPED.accentColor(),
                count = statusCounts[AnimeStatus.DROPPED] ?: 0,
            ),
            StatusTileSpec(
                filter = LibraryFilter.Favorites,
                label = "Избранное",
                icon = Icons.Filled.Favorite,
                color = StatusFavorite,
                count = favoritesCount,
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeader(title = "Списки")
        // 2 rows × 3 columns
        tiles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { tile ->
                    StatusTile(
                        spec = tile,
                        isSelected = selectedFilter == tile.filter,
                        onClick = { onTileClick(tile.filter) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusTile(
    spec: StatusTileSpec,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgAlpha = if (isSelected) 0.22f else 0.10f
    val borderAlpha = if (isSelected) 1f else 0f
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(spec.color.copy(alpha = bgAlpha))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(spec.color.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = spec.icon,
                        contentDescription = null,
                        tint = spec.color,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = spec.count.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = spec.color,
                )
            }
            Text(
                text = spec.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // selection indicator border
        if (borderAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Transparent),
            )
        }
    }
}

// ── "Все" section ─────────────────────────────────────────────────────────────

@Composable
private fun AllSectionHeader(
    viewMode: LibraryViewMode,
    onViewModeToggled: () -> Unit,
    selectedFilter: LibraryFilter,
    statusCounts: Map<AnimeStatus, Int>,
    favoritesCount: Int,
    totalCount: Int,
    onFilterSelected: (LibraryFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(
                title = "Все",
                modifier = Modifier.weight(1f),
            )
            ViewModeToggle(viewMode = viewMode, onToggle = onViewModeToggled)
        }

        LibraryFilterChips(
            selectedFilter = selectedFilter,
            statusCounts = statusCounts,
            favoritesCount = favoritesCount,
            totalCount = totalCount,
            onFilterSelected = onFilterSelected,
        )
    }
}

@Composable
private fun ViewModeToggle(
    viewMode: LibraryViewMode,
    onToggle: () -> Unit,
) {
    val (icon, contentDesc) = when (viewMode) {
        LibraryViewMode.GRID -> Icons.AutoMirrored.Outlined.ViewList to "Список"
        LibraryViewMode.LIST -> Icons.Outlined.GridView to "Сетка"
    }
    IconButton(onClick = onToggle) {
        Icon(icon, contentDescription = contentDesc)
    }
}

@Composable
private fun LibraryFilterChips(
    selectedFilter: LibraryFilter,
    statusCounts: Map<AnimeStatus, Int>,
    favoritesCount: Int,
    totalCount: Int,
    onFilterSelected: (LibraryFilter) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item("all") {
            LibraryChip(
                label = "Все",
                count = totalCount,
                selected = selectedFilter == LibraryFilter.All,
                onClick = { onFilterSelected(LibraryFilter.All) },
            )
        }
        item("favorites") {
            LibraryChip(
                label = "Избранное",
                count = favoritesCount,
                selected = selectedFilter == LibraryFilter.Favorites,
                onClick = { onFilterSelected(LibraryFilter.Favorites) },
            )
        }
        items(items = AnimeStatus.entries, key = { "status-${it.code}" }) { status ->
            val count = statusCounts[status] ?: 0
            if (count > 0) {
                LibraryChip(
                    label = status.label,
                    count = count,
                    selected = selectedFilter == LibraryFilter.Status(status),
                    onClick = { onFilterSelected(LibraryFilter.Status(status)) },
                )
            }
        }
    }
}

@Composable
private fun LibraryChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        label = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label)
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

// ── Grid card with status pill ────────────────────────────────────────────────

@Composable
private fun LibraryGridCard(
    item: LibraryItem,
    onClick: () -> Unit,
) {
    Box {
        AnimeCard(
            anime = item.anime,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        )
        // Status pill in top-left corner — does not eclipse type badge in top-right
        item.listStatus?.let { status ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(status.accentColor().copy(alpha = 0.85f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = status.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
        if (item.isFavorite && item.listStatus == null) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "В избранном",
                tint = StatusFavorite,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(16.dp),
            )
        }
    }
}

// ── Status pill (small inline) ────────────────────────────────────────────────

@Composable
private fun StatusPill(status: AnimeStatus) {
    val color = status.accentColor()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier,
    )
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun LibraryEmpty(
    hasQuery: Boolean,
    filter: LibraryFilter,
) {
    val message = when {
        hasQuery -> "Ничего не найдено"
        filter == LibraryFilter.Favorites -> "Избранное пока пусто"
        filter is LibraryFilter.Status -> "Список «${filter.status.label}» пока пуст"
        else -> "Библиотека пока пуста"
    }
    val hint = when {
        hasQuery -> "Попробуйте изменить запрос"
        else -> "Добавляйте тайтлы в избранное и списки — они появятся здесь"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

// ── Sort sheet (DATE / TITLE only) ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibrarySortSheet(
    selectedSort: SortOption,
    sortAscending: Boolean,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
            )
        },
    ) {
        Column(modifier = Modifier.padding(bottom = 28.dp)) {
            Text(
                text = "Сортировка",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            LibrarySortRow(
                icon = Icons.Outlined.Schedule,
                label = "По дате активности",
                isSelected = selectedSort == SortOption.DATE,
                ascending = if (selectedSort == SortOption.DATE) sortAscending else false,
                onClick = { onSortSelected(SortOption.DATE) },
            )
            LibrarySortRow(
                icon = Icons.Outlined.SortByAlpha,
                label = "По названию",
                isSelected = selectedSort == SortOption.TITLE,
                ascending = if (selectedSort == SortOption.TITLE) sortAscending else false,
                onClick = { onSortSelected(SortOption.TITLE) },
            )
        }
    }
}

@Composable
private fun LibrarySortRow(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    ascending: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val iconColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Text(
                text = if (ascending) "↑" else "↓",
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun libraryStatsLabel(total: Int, continueCount: Int): String {
    val titlesPart = "$total ${pluralTitles(total)}"
    if (continueCount <= 0) return titlesPart
    return "$titlesPart · $continueCount ${pluralContinue(continueCount)}"
}

private fun pluralTitles(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1 && mod100 != 11 -> "тайтл"
        mod10 in 2..4 && mod100 !in 12..14 -> "тайтла"
        else -> "тайтлов"
    }
}

private fun pluralContinue(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1 && mod100 != 11 -> "продолжается"
        else -> "продолжаются"
    }
}
