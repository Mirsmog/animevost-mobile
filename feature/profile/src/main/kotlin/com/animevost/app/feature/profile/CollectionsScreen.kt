package com.animevost.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.clickable
import com.animevost.app.core.domain.model.AnimeStatus
import com.animevost.app.core.domain.model.SortOption
import com.animevost.app.core.ui.components.AnimeCardHorizontal
import com.animevost.app.core.ui.components.LoadingState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    onAnimeClick: (String) -> Unit,
    viewModel: CollectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showSortSheet by remember { mutableStateOf(false) }

    if (showSortSheet) {
        CollectionsSortSheet(
            selectedSort = state.sort,
            sortAscending = state.sortAscending,
            onSortSelected = {
                viewModel.onEvent(CollectionsEvent.SortSelected(it))
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Коллекции",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            state.isLoading -> {
                LoadingState(modifier = Modifier.padding(innerPadding))
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item {
                        CollectionsSearchField(
                            query = state.query,
                            onQueryChange = { viewModel.onEvent(CollectionsEvent.QueryChanged(it)) },
                        )
                    }
                    item {
                        CollectionsTabs(
                            selectedTab = state.selectedTab,
                            tabCounts = state.tabCounts,
                            onTabSelected = { viewModel.onEvent(CollectionsEvent.TabSelected(it)) },
                        )
                    }
                    if (state.summaryCards.any { it.count > 0 }) {
                        item {
                            CollectionsSummaryCards(
                                cards = state.summaryCards,
                                onCardClick = { card ->
                                    viewModel.onEvent(CollectionsEvent.TabSelected(card.tab))
                                    if (card.tab == CollectionsTab.LISTS) {
                                        viewModel.onEvent(CollectionsEvent.StatusSelected(card.status))
                                    }
                                },
                            )
                        }
                    }
                    if (state.selectedTab == CollectionsTab.LISTS && state.availableStatuses.isNotEmpty()) {
                        item {
                            ListStatusChips(
                                selectedStatus = state.selectedStatus,
                                statuses = state.availableStatuses,
                                statusCounts = state.statusCounts,
                                onStatusSelected = { viewModel.onEvent(CollectionsEvent.StatusSelected(it)) },
                            )
                        }
                    }
                    item {
                        CollectionsSummary(
                            query = state.query,
                            selectedTab = state.selectedTab,
                            selectedStatus = state.selectedStatus,
                            count = state.items.size,
                        )
                    }
                    item {
                        CollectionsToolbar(
                            count = state.items.size,
                            sort = state.sort,
                            sortAscending = state.sortAscending,
                            onSortClick = { showSortSheet = true },
                        )
                    }

                    if (state.items.isEmpty()) {
                        item {
                            EmptyCollectionsState(
                                hasQuery = state.query.isNotBlank(),
                                tab = state.selectedTab,
                            )
                        }
                    } else {
                        items(
                            items = state.items,
                            key = { it.anime.url.ifBlank { it.anime.id.toString() } },
                        ) { item ->
                            CollectionRow(
                                item = item,
                                onAnimeClick = onAnimeClick,
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 84.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionsSortSheet(
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
    ) {
        Column(modifier = Modifier.padding(bottom = 28.dp)) {
            Text(
                text = "Сортировка",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            CollectionSortOption.entries.forEach { option ->
                val mappedSort = option.toSortOption()
                val selected = selectedSort == mappedSort
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = selected,
                        onClick = { onSortSelected(mappedSort) },
                        label = {
                            Text(
                                text = option.label(sortAscending && selected),
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        placeholder = { Text("Поиск по названию") },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Очистить")
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
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {}),
    )
}

@Composable
private fun CollectionsSummaryCards(
    cards: List<CollectionSummaryCard>,
    onCardClick: (CollectionSummaryCard) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(cards.filter { it.count > 0 }, key = { it.title }) { card ->
            Surface(
                modifier = Modifier
                    .width(132.dp)
                    .aspectRatio(1.2f)
                    .clickable { onCardClick(card) },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = card.count.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Открыть",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionsTabs(
    selectedTab: CollectionsTab,
    tabCounts: Map<CollectionsTab, Int>,
    onTabSelected: (CollectionsTab) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(CollectionsTab.entries) { tab ->
            val isSelected = selectedTab == tab
            FilterChip(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                label = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Text(
                            text = (tabCounts[tab] ?: 0).toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color(0xFF111111).copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color(0xFF111111),
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

@Composable
private fun ListStatusChips(
    selectedStatus: AnimeStatus?,
    statuses: List<AnimeStatus>,
    statusCounts: Map<AnimeStatus, Int>,
    onStatusSelected: (AnimeStatus?) -> Unit,
) {
    val totalCount = statuses.sumOf { statusCounts[it] ?: 0 }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedStatus == null,
                onClick = { onStatusSelected(null) },
                label = { StatusChipLabel(label = "Все", count = totalCount) },
                shape = RoundedCornerShape(18.dp),
            )
        }
        items(statuses) { status ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onStatusSelected(if (selectedStatus == status) null else status) },
                label = { StatusChipLabel(label = status.label, count = statusCounts[status] ?: 0) },
                shape = RoundedCornerShape(18.dp),
            )
        }
    }
}

@Composable
private fun StatusChipLabel(
    label: String,
    count: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun CollectionsSummary(
    query: String,
    selectedTab: CollectionsTab,
    selectedStatus: AnimeStatus?,
    count: Int,
) {
    val title = when {
        query.isNotBlank() -> if (count == 0) "По запросу ничего не найдено" else "Найдено $count"
        selectedTab == CollectionsTab.ALL -> "$count в коллекции"
        else -> "${selectedTab.title}: $count"
    }
    val subtitle = buildString {
        if (selectedTab != CollectionsTab.ALL) append(selectedTab.title)
        selectedStatus?.let {
            if (isNotEmpty()) append(" • ")
            append(it.label)
        }
        if (query.isNotBlank()) {
            if (isNotEmpty()) append(" • ")
            append("поиск «$query»")
        }
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CollectionsToolbar(
    count: Int,
    sort: SortOption,
    sortAscending: Boolean,
    onSortClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = countLabel(count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Surface(
            onClick = onSortClick,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Сортировка",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = sortShortName(sort, sortAscending),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CollectionRow(
    item: CollectionItem,
    onAnimeClick: (String) -> Unit,
) {
    AnimeCardHorizontal(
        anime = item.anime,
        onClick = { onAnimeClick(item.anime.url) },
        trailingContent = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CollectionSources(item)
                if (item.anime.episodeInfo.isNotBlank()) {
                    Text(
                        text = item.anime.episodeInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = formatActivityTime(item.lastActivityAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
                item.listStatus?.let {
                    Text(
                        text = it.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
    )
}

@Composable
private fun CollectionSources(item: CollectionItem) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (CollectionSource.FAVORITES in item.sources) {
            Icon(
                Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        if (CollectionSource.HISTORY in item.sources) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (CollectionSource.LISTS in item.sources) {
            Icon(
                Icons.Default.FolderCopy,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = item.sources.joinToString(separator = " • ") { it.label },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyCollectionsState(
    hasQuery: Boolean,
    tab: CollectionsTab,
) {
    val message = when {
        hasQuery -> "Ничего не найдено"
        tab == CollectionsTab.HISTORY -> "История просмотра пока пуста"
        tab == CollectionsTab.FAVORITES -> "Избранное пока пусто"
        tab == CollectionsTab.LISTS -> "Списки пока пусты"
        else -> "Коллекции пока пусты"
    }

    val hint = when {
        hasQuery -> "Попробуйте изменить запрос"
        else -> "Здесь появятся избранное, история и пользовательские списки"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = when (tab) {
                CollectionsTab.HISTORY -> Icons.Default.History
                CollectionsTab.FAVORITES -> Icons.Outlined.FavoriteBorder
                CollectionsTab.LISTS -> Icons.Default.FolderCopy
                CollectionsTab.ALL -> Icons.Outlined.Schedule
            },
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        if (!hasQuery) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Попробуйте открыть профиль на сайте или добавить тайтлы в избранное и списки",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val CollectionSource.label: String
    get() = when (this) {
        CollectionSource.FAVORITES -> "избранное"
        CollectionSource.HISTORY -> "история"
        CollectionSource.LISTS -> "список"
    }

private fun countLabel(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    val suffix = when {
        mod10 == 1 && mod100 != 11 -> "аниме"
        mod10 in 2..4 && mod100 !in 12..14 -> "аниме"
        else -> "аниме"
    }
    return "$count $suffix"
}

private fun sortShortName(sort: SortOption, ascending: Boolean): String {
    val arrow = if (ascending) "↑" else "↓"
    return when (sort) {
        SortOption.TITLE -> "А-Я $arrow"
        else -> "Дата $arrow"
    }
}

private fun formatActivityTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val hour = 60 * 60 * 1000L
    val day = 24 * hour
    return when {
        diff < hour -> "только что"
        diff < day -> "сегодня"
        diff < day * 2 -> "вчера"
        diff < day * 7 -> "${diff / day} дн. назад"
        else -> SimpleDateFormat("d MMM", Locale("ru")).format(Date(timestamp))
    }
}

private enum class CollectionSortOption {
    LAST_ACTIVITY,
    TITLE,
    ;

    fun toSortOption(): SortOption = when (this) {
        LAST_ACTIVITY -> SortOption.DATE
        TITLE -> SortOption.TITLE
    }

    fun label(ascending: Boolean): String = when (this) {
        LAST_ACTIVITY -> if (ascending) "Сначала старые" else "Сначала новые"
        TITLE -> if (ascending) "Название А-Я" else "Название Я-А"
    }
}
