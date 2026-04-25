package com.animevost.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.animevost.app.core.domain.model.AnimeStatus
import com.animevost.app.core.domain.model.SortOption
import com.animevost.app.core.ui.components.AnimeCardHorizontal
import com.animevost.app.core.ui.components.LoadingState
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
    var showSearchField by remember { mutableStateOf(false) }

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
                    if (showSearchField) {
                        TextField(
                            value = state.query,
                            onValueChange = { viewModel.onEvent(CollectionsEvent.QueryChanged(it)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            placeholder = { Text("Поиск") },
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
                        Text(
                            text = "Коллекции",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
                actions = {
                    if (showSearchField) {
                        IconButton(onClick = {
                            viewModel.onEvent(CollectionsEvent.QueryChanged(""))
                            showSearchField = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть поиск")
                        }
                    } else {
                        IconButton(onClick = { showSearchField = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Поиск")
                        }
                    }
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
                        CollectionsTabsRow(
                            selectedTab = state.selectedTab,
                            onTabSelected = { viewModel.onEvent(CollectionsEvent.TabSelected(it)) },
                        )
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
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(2.dp),
                    ),
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
            CollectionSortOption.entries.forEach { option ->
                val mappedSort = option.toSortOption()
                val selected = selectedSort == mappedSort
                CollectionSortRow(
                    icon = option.icon(),
                    label = option.title,
                    isSelected = selected,
                    ascending = if (selected) sortAscending else false,
                    onClick = { onSortSelected(mappedSort) },
                )
            }
        }
    }
}

@Composable
private fun CollectionsTabsRow(
    selectedTab: CollectionsTab,
    onTabSelected: (CollectionsTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CollectionsTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
            ) {
                Text(
                    tab.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected)
                        Color(0xFF111111)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item.listStatus?.let {
                    Text(
                        text = it.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
                val activity = formatActivityTime(item.lastActivityAt)
                if (activity.isNotBlank()) {
                    Text(
                        text = activity,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                CollectionSources(item)
            }
        },
    )
}

@Composable
private fun CollectionSources(item: CollectionItem) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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

    val title: String
        get() = when (this) {
            LAST_ACTIVITY -> "По дате активности"
            TITLE -> "По названию"
        }

    fun toSortOption(): SortOption = when (this) {
        LAST_ACTIVITY -> SortOption.DATE
        TITLE -> SortOption.TITLE
    }

    fun label(ascending: Boolean): String = when (this) {
        LAST_ACTIVITY -> if (ascending) "Сначала старые" else "Сначала новые"
        TITLE -> if (ascending) "Название А-Я" else "Название Я-А"
    }

    fun icon(): ImageVector = when (this) {
        LAST_ACTIVITY -> Icons.Outlined.CalendarMonth
        TITLE -> Icons.Outlined.SortByAlpha
    }
}

@Composable
private fun CollectionSortRow(
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
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = iconColor,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
            )
            Text(
                text = if (isSelected) selectedSortDirectionLabel(label, ascending) else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isSelected) {
            Icon(
                imageVector = if (ascending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (ascending) "По возрастанию" else "По убыванию",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun selectedSortDirectionLabel(label: String, ascending: Boolean): String = when (label) {
    "По названию" -> if (ascending) "А-Я" else "Я-А"
    else -> if (ascending) "Сначала старые" else "Сначала новые"
}
