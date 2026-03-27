package com.animevost.app.feature.schedule

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.animevost.app.core.domain.model.ScheduleItem
import com.animevost.app.core.ui.components.ErrorState
import com.animevost.app.core.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onAnimeClick: (String) -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Расписание",
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
            DaySelector(
                selectedDay = state.selectedDay,
                onDaySelected = { viewModel.onEvent(ScheduleEvent.SelectDay(it)) },
            )

            when {
                state.isLoading -> {
                    LoadingState()
                }
                state.error != null -> {
                    ErrorState(
                        message = state.error!!,
                        onRetry = { viewModel.onEvent(ScheduleEvent.Refresh) },
                    )
                }
                state.scheduleItems.isEmpty() -> {
                    EmptyScheduleState(dayName = state.selectedDay.fullName)
                }
                else -> {
                    ScheduleList(
                        items = state.scheduleItems,
                        onItemClick = onAnimeClick,
                    )
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
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(DayTab.entries) { day ->
            val isSelected = day == selectedDay
            val isToday = day == DayTab.fromToday()

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    )
                    .clickable { onDaySelected(day) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.shortName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun ScheduleList(
    items: List<ScheduleItem>,
    onItemClick: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.url }) { item ->
            ScheduleItemCard(
                item = item,
                onClick = { onItemClick(item.url) },
            )
        }
    }
}

@Composable
private fun ScheduleItemCard(
    item: ScheduleItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.time.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = item.time,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EmptyScheduleState(dayName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "📅",
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Нет аниме на $dayName",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
