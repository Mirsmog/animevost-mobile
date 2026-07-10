package com.animevost.app.feature.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.animevost.app.core.domain.model.AnimeStatus
import com.animevost.app.core.ui.components.AnimeCard
import com.animevost.app.core.ui.theme.StatusFavorite
import com.animevost.app.core.ui.theme.accentColor
import kotlin.math.roundToInt

@Composable
internal fun ContinueWatchingSection(
    items: List<ContinueWatchingItem>,
    onContinueClick: (ContinueWatchingItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = "Продолжить смотреть")
        LazyRow(
            contentPadding = PaddingValues(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = items, key = { "continue-${it.anime.id}-${it.episodeVideoId}" }) { item ->
                ContinueWatchingCard(
                    item = item,
                    onClick = { onContinueClick(item) },
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
    val progress by animateFloatAsState(
        targetValue = item.progressFraction,
        label = "watch-progress",
    )

    Surface(
        modifier = Modifier
            .width(286.dp)
            .height(148.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.anime.posterUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build(),
                contentDescription = item.anime.title,
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(14.dp),
            ) {
                Text(
                    text = item.anime.title.ifBlank { item.anime.titleOriginal },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = episodeLabel(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Продолжить · ${(progress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                            gapSize = 0.dp,
                            drawStopIndicator = {},
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun episodeLabel(item: ContinueWatchingItem): String = when {
    item.episodeName.isNotBlank() -> item.episodeName
    item.episodeIndex > 0 -> "Серия ${item.episodeIndex + 1}"
    else -> "Текущая серия"
}

private data class StatusTileSpec(
    val filter: LibraryFilter,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val count: Int,
)

@Composable
internal fun StatusTilesGrid(
    statusCounts: Map<AnimeStatus, Int>,
    favoritesCount: Int,
    selectedFilter: LibraryFilter,
    onTileClick: (LibraryFilter) -> Unit,
) {
    val tiles = remember(statusCounts, favoritesCount) {
        listOf(
            StatusTileSpec(
                LibraryFilter.Status(AnimeStatus.WATCHING),
                AnimeStatus.WATCHING.label,
                Icons.Outlined.PlayCircle,
                AnimeStatus.WATCHING.accentColor(),
                statusCounts[AnimeStatus.WATCHING] ?: 0,
            ),
            StatusTileSpec(
                LibraryFilter.Status(AnimeStatus.PLANNED),
                AnimeStatus.PLANNED.label,
                Icons.Outlined.Bookmark,
                AnimeStatus.PLANNED.accentColor(),
                statusCounts[AnimeStatus.PLANNED] ?: 0,
            ),
            StatusTileSpec(
                LibraryFilter.Status(AnimeStatus.WATCHED),
                AnimeStatus.WATCHED.label,
                Icons.Outlined.CheckCircle,
                AnimeStatus.WATCHED.accentColor(),
                statusCounts[AnimeStatus.WATCHED] ?: 0,
            ),
            StatusTileSpec(
                LibraryFilter.Status(AnimeStatus.ON_HOLD),
                AnimeStatus.ON_HOLD.label,
                Icons.Outlined.PauseCircle,
                AnimeStatus.ON_HOLD.accentColor(),
                statusCounts[AnimeStatus.ON_HOLD] ?: 0,
            ),
            StatusTileSpec(
                LibraryFilter.Status(AnimeStatus.DROPPED),
                AnimeStatus.DROPPED.label,
                Icons.Outlined.RemoveCircle,
                AnimeStatus.DROPPED.accentColor(),
                statusCounts[AnimeStatus.DROPPED] ?: 0,
            ),
            StatusTileSpec(
                LibraryFilter.Favorites,
                "Избранное",
                Icons.Filled.Favorite,
                StatusFavorite,
                favoritesCount,
            ),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "Мои списки")
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
    Surface(
        modifier = modifier.selectable(
            selected = isSelected,
            role = Role.RadioButton,
            onClick = onClick,
        ),
        shape = RoundedCornerShape(16.dp),
        color = spec.color.copy(alpha = if (isSelected) 0.22f else 0.11f),
        border = if (isSelected) BorderStroke(1.dp, spec.color.copy(alpha = 0.9f)) else null,
        tonalElevation = if (isSelected) 2.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(spec.color.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = spec.icon,
                        contentDescription = null,
                        tint = spec.color,
                        modifier = Modifier.size(19.dp),
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
    }
}

@Composable
internal fun CatalogToolbar(
    selectedFilter: LibraryFilter,
    visibleCount: Int,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier.padding(
                top = 4.dp,
                bottom = 4.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedFilter.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "$visibleCount ${pluralTitles(visibleCount)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = "Фильтр",
                    tint = if (selectedFilter == LibraryFilter.All) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }
}

@Composable
internal fun LibraryGridCard(
    item: LibraryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        AnimeCard(
            anime = item.anime,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        )
        item.listStatus?.let { status ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(status.accentColor().copy(alpha = 0.9f))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
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
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp),
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.55f),
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "В избранном",
                    tint = StatusFavorite,
                    modifier = Modifier.padding(6.dp).size(16.dp),
                )
            }
        }
    }
}


@Composable
internal fun LibraryEmpty(
    hasQuery: Boolean,
    filter: LibraryFilter,
    onReset: () -> Unit,
) {
    val message = when {
        hasQuery -> "Ничего не найдено"
        filter == LibraryFilter.Favorites -> "Избранное пока пусто"
        filter is LibraryFilter.Status -> "Список «${filter.status.label}» пока пуст"
        else -> "Библиотека пока пуста"
    }
    val hint = if (hasQuery) {
        "Попробуйте изменить запрос"
    } else {
        "Добавляйте тайтлы в избранное и списки"
    }
    val icon = when {
        hasQuery -> Icons.Outlined.SearchOff
        filter == LibraryFilter.Favorites -> Icons.Outlined.FavoriteBorder
        else -> Icons.Outlined.Schedule
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(18.dp).size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (hasQuery || filter != LibraryFilter.All) {
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(onClick = onReset) {
                Text(if (hasQuery) "Очистить поиск" else "Показать все")
            }
        }
    }
}

@Composable
internal fun SectionHeader(
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

internal fun LibraryFilter.displayName(): String = when (this) {
    LibraryFilter.All -> "Все тайтлы"
    LibraryFilter.Favorites -> "Избранное"
    is LibraryFilter.Status -> status.label
}

internal fun libraryStatsLabel(total: Int, continueCount: Int): String {
    val titlesPart = "$total ${pluralTitles(total)}"
    if (continueCount <= 0) return titlesPart
    return "$titlesPart · $continueCount ${pluralContinue(continueCount)}"
}

internal fun pluralTitles(number: Int): String {
    val mod10 = number % 10
    val mod100 = number % 100
    return when {
        mod10 == 1 && mod100 != 11 -> "тайтл"
        mod10 in 2..4 && mod100 !in 12..14 -> "тайтла"
        else -> "тайтлов"
    }
}

private fun pluralContinue(number: Int): String {
    val mod10 = number % 10
    val mod100 = number % 100
    return if (mod10 == 1 && mod100 != 11) "продолжается" else "продолжаются"
}
