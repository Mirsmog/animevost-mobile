package com.animevost.app.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.animevost.app.core.domain.model.ScheduleItem
import com.animevost.app.core.ui.components.ErrorState
import com.animevost.app.core.ui.components.LoadingState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onAnimeClick: (String) -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val pager = rememberPagerState(initialPage = DayTab.fromToday().ordinal) { DayTab.entries.size }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Расписание",
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
            DaySelector(
                selectedDay = DayTab.entries[pager.currentPage],
                onDaySelected = { day -> scope.launch { pager.animateScrollToPage(day.ordinal) } },
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(
                    message = state.error!!,
                    onRetry = { viewModel.onEvent(ScheduleEvent.Refresh) },
                )
                else -> HorizontalPager(
                    state = pager,
                    key = { DayTab.entries[it].name },
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    val day = DayTab.entries[page]
                    val items = state.allSchedule[day.fullName].orEmpty()
                    if (items.isEmpty()) {
                        EmptyScheduleState(day.fullName)
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = day.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${items.size} аниме",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            ScheduleList(items = items, onItemClick = onAnimeClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySelector(
    selectedDay: DayTab,
    onDaySelected: (DayTab) -> Unit,
) {
    val today = remember { DayTab.fromToday() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        DayTab.entries.forEach { day ->
            DayChip(
                day = day,
                isSelected = day == selectedDay,
                isToday = day == today,
                onClick = { onDaySelected(day) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayChip(
    day: DayTab,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = day.shortName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else -> Color.Transparent
                    },
                ),
        )
    }
}

@Composable
private fun ScheduleList(
    items: List<ScheduleItem>,
    onItemClick: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(items, key = { index, item -> "${item.url}#$index" }) { index, item ->
            ScheduleItemRow(
                item = item,
                onClick = { onItemClick(item.url) },
            )
            if (index < items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 82.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                )
            }
        }
    }
}

@Composable
private fun ScheduleItemRow(
    item: ScheduleItem,
    onClick: () -> Unit,
) {
    val isAllDay = item.time.trim().equals("В течение дня", ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = if (isAllDay) "В\nтечение\nдня" else item.time.ifBlank { "—" },
            style = if (isAllDay) {
                MaterialTheme.typography.labelLarge.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 15.5.sp,
                )
            } else {
                MaterialTheme.typography.labelLarge
            },
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            color = if (item.time.isNotBlank()) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.width(48.dp),
        )

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyScheduleState(dayName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "📅",
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = "На $dayName ничего нет",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
