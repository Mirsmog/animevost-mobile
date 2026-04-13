package com.animevost.app.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ─── Full player controls overlay ─────────────────────────────────────────────

@Composable
internal fun PlayerControls(
    episodeName: String,
    videoSources: List<String>,
    selectedQuality: String,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeek: (Float) -> Unit,
    onSelectQuality: (String) -> Unit,
) {
    var showQualityMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f)),
    ) {
        // ── Top bar: ← name  [quality] ──────────────────────────
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = episodeName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
            )
            Box {
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { showQualityMenu = true }
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            RoundedCornerShape(6.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Filled.HighQuality,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = selectedQuality,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                DropdownMenu(
                    expanded = showQualityMenu,
                    onDismissRequest = { showQualityMenu = false },
                ) {
                    videoSources.forEach { quality ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    quality,
                                    fontWeight = if (quality == selectedQuality)
                                        FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            onClick = {
                                onSelectQuality(quality)
                                showQualityMenu = false
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        // ── Center: |◁  ◁10  ▶⏸  10▷  ▷| ──────────────────────
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPrevious,
                enabled = hasPrevious,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Предыдущая серия",
                    tint = if (hasPrevious) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(34.dp),
                )
            }
            IconButton(onClick = onSeekBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Filled.Replay10,
                    contentDescription = "−10 сек",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPlayPause,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp),
                )
            }
            IconButton(onClick = onSeekForward, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Filled.Forward10,
                    contentDescription = "+10 сек",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
            IconButton(
                onClick = onNext,
                enabled = hasNext,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Следующая серия",
                    tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(34.dp),
                )
            }
        }

        // ── Bottom: time + progress slider ──────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    formatTime(currentPosition),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
                Text(
                    formatTime(duration),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
            ThinSeekBar(
                fraction = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                onSeek = onSeek,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─── Thin seek bar with orange circle thumb ──────────────────────────────────

@Composable
internal fun ThinSeekBar(
    fraction: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val displayFraction = if (dragging) dragFraction else fraction.coerceIn(0f, 1f)
    val thumbDiameter = 14.dp
    val trackShape = RoundedCornerShape(1.5.dp)

    BoxWithConstraints(
        modifier = modifier
            .height(28.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek(dragFraction)
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, _ ->
                        dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek(dragFraction)
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackHeight = 3.dp
        val thumbOffset = (maxWidth * displayFraction - thumbDiameter / 2).coerceAtLeast(0.dp)

        // Inactive track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .background(Color.White.copy(alpha = 0.2f), trackShape),
        )
        // Active track
        Box(
            modifier = Modifier
                .fillMaxWidth(displayFraction)
                .height(trackHeight)
                .background(primary, trackShape),
        )
        // Thumb circle
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbDiameter)
                .background(primary, CircleShape),
        )
    }
}
