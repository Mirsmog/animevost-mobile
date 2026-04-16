package com.animevost.app.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlagCircle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animevost.app.core.domain.model.SegmentType
import com.animevost.app.core.domain.model.SkipSegment

@Composable
internal fun SkipSegmentEditorSheet(
    segments: List<SkipSegment>,
    editingType: SegmentType?,
    pendingStartMs: Long?,
    currentPositionMs: Long,
    onStartCapture: (SegmentType) -> Unit,
    onEndCapture: (SegmentType) -> Unit,
    onDelete: (SegmentType) -> Unit,
    onBeginEdit: (SegmentType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .widthIn(max = 420.dp)
            .background(Color(0xF0111118), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Пропуск сегментов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            SegmentRow(
                label = "Интро",
                segment = segments.firstOrNull { it.type == SegmentType.INTRO },
                isEditing = editingType == SegmentType.INTRO,
                pendingStartMs = if (editingType == SegmentType.INTRO) pendingStartMs else null,
                currentPositionMs = currentPositionMs,
                onStartCapture = { onStartCapture(SegmentType.INTRO) },
                onEndCapture = { onEndCapture(SegmentType.INTRO) },
                onDelete = { onDelete(SegmentType.INTRO) },
                onBeginEdit = { onBeginEdit(SegmentType.INTRO) },
            )
            Spacer(modifier = Modifier.height(8.dp))
            SegmentRow(
                label = "Аутро",
                segment = segments.firstOrNull { it.type == SegmentType.OUTRO },
                isEditing = editingType == SegmentType.OUTRO,
                pendingStartMs = if (editingType == SegmentType.OUTRO) pendingStartMs else null,
                currentPositionMs = currentPositionMs,
                onStartCapture = { onStartCapture(SegmentType.OUTRO) },
                onEndCapture = { onEndCapture(SegmentType.OUTRO) },
                onDelete = { onDelete(SegmentType.OUTRO) },
                onBeginEdit = { onBeginEdit(SegmentType.OUTRO) },
            )
        }
    }
}

@Composable
private fun SegmentRow(
    label: String,
    segment: SkipSegment?,
    isEditing: Boolean,
    pendingStartMs: Long?,
    currentPositionMs: Long,
    onStartCapture: () -> Unit,
    onEndCapture: () -> Unit,
    onDelete: () -> Unit,
    onBeginEdit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            if (segment != null && !isEditing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTime(segment.referenceTimeMs) + " · " +
                            formatTime(segment.referenceTimeMs + segment.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Удалить",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            } else if (!isEditing) {
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBeginEdit,
                        )
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "Задать",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        if (isEditing) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Текущая позиция: ${formatTime(currentPositionMs)}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pendingStartMs == null) {
                    Button(
                        onClick = onStartCapture,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            Icons.Filled.FlagCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Начало", color = Color.White)
                    }
                } else {
                    Text(
                        "Начало: ${formatTime(pendingStartMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = onEndCapture) {
                        Icon(
                            Icons.Filled.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Конец", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SkipSnackbarOverlay(
    message: String,
    visible: Boolean,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .background(Color(0xEE0D0D14), RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
                Text(
                    text = "Отменить",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onUndo,
                    ),
                )
            }
        }
    }
}
