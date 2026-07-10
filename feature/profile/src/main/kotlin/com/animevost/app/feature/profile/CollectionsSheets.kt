package com.animevost.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animevost.app.core.domain.model.AnimeStatus
import com.animevost.app.core.domain.model.SortOption
import com.animevost.app.core.ui.theme.StatusFavorite
import com.animevost.app.core.ui.theme.accentColor

private data class FilterSheetItem(
    val filter: LibraryFilter,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val count: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryFilterSheet(
    selectedFilter: LibraryFilter,
    statusCounts: Map<AnimeStatus, Int>,
    favoritesCount: Int,
    totalCount: Int,
    onFilterSelected: (LibraryFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val items = listOf(
        FilterSheetItem(
            LibraryFilter.All,
            "Все тайтлы",
            Icons.Outlined.Schedule,
            MaterialTheme.colorScheme.primary,
            totalCount,
        ),
        FilterSheetItem(
            LibraryFilter.Favorites,
            "Избранное",
            Icons.Filled.Favorite,
            StatusFavorite,
            favoritesCount,
        ),
        FilterSheetItem(
            LibraryFilter.Status(AnimeStatus.WATCHING),
            AnimeStatus.WATCHING.label,
            Icons.Outlined.PlayCircle,
            AnimeStatus.WATCHING.accentColor(),
            statusCounts[AnimeStatus.WATCHING] ?: 0,
        ),
        FilterSheetItem(
            LibraryFilter.Status(AnimeStatus.PLANNED),
            AnimeStatus.PLANNED.label,
            Icons.Outlined.Bookmark,
            AnimeStatus.PLANNED.accentColor(),
            statusCounts[AnimeStatus.PLANNED] ?: 0,
        ),
        FilterSheetItem(
            LibraryFilter.Status(AnimeStatus.WATCHED),
            AnimeStatus.WATCHED.label,
            Icons.Outlined.CheckCircle,
            AnimeStatus.WATCHED.accentColor(),
            statusCounts[AnimeStatus.WATCHED] ?: 0,
        ),
        FilterSheetItem(
            LibraryFilter.Status(AnimeStatus.ON_HOLD),
            AnimeStatus.ON_HOLD.label,
            Icons.Outlined.Schedule,
            AnimeStatus.ON_HOLD.accentColor(),
            statusCounts[AnimeStatus.ON_HOLD] ?: 0,
        ),
        FilterSheetItem(
            LibraryFilter.Status(AnimeStatus.DROPPED),
            AnimeStatus.DROPPED.label,
            Icons.Outlined.Schedule,
            AnimeStatus.DROPPED.accentColor(),
            statusCounts[AnimeStatus.DROPPED] ?: 0,
        ),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { SheetDragHandle() },
    ) {
        Column(modifier = Modifier.padding(bottom = 28.dp)) {
            SheetTitle("Показать")
            items.forEach { item ->
                FilterRow(
                    item = item,
                    selected = selectedFilter == item.filter,
                    onClick = {
                        onFilterSelected(item.filter)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    item: FilterSheetItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) item.color.copy(alpha = 0.12f) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(background)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = item.color.copy(alpha = 0.18f),
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.color,
                modifier = Modifier.padding(8.dp).size(20.dp),
            )
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = item.count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) item.color else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibrarySortSheet(
    selectedSort: SortOption,
    sortAscending: Boolean,
    onSortSelected: (SortOption) -> Unit,
    onDirectionChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { SheetDragHandle() },
    ) {
        Column(modifier = Modifier.padding(bottom = 28.dp)) {
            SheetTitle("Сортировка")
            SortRow(
                icon = Icons.Outlined.Schedule,
                label = "По дате активности",
                selected = selectedSort == SortOption.DATE,
                onClick = { onSortSelected(SortOption.DATE) },
            )
            SortRow(
                icon = Icons.Outlined.SortByAlpha,
                label = "По названию",
                selected = selectedSort == SortOption.TITLE,
                onClick = { onSortSelected(SortOption.TITLE) },
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            )
            Text(
                text = "Порядок",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = sortAscending,
                    onClick = { onDirectionChanged(true) },
                    label = {
                        Text(if (selectedSort == SortOption.TITLE) "А - Я" else "Сначала старые")
                    },
                )
                FilterChip(
                    selected = !sortAscending,
                    onClick = { onDirectionChanged(false) },
                    label = {
                        Text(if (selectedSort == SortOption.TITLE) "Я - А" else "Сначала новые")
                    },
                )
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text("Готово")
            }
        }
    }
}

@Composable
private fun SortRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = contentColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Выбрано",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 10.dp)
            .width(36.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
    )
}
