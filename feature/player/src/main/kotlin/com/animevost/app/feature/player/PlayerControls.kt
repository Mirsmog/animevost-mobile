package com.animevost.app.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animevost.app.core.domain.model.SkipInterval

// ─── Full player controls overlay ─────────────────────────────────────────────

@Composable
internal fun PlayerControls(
    episodeName: String,
    videoSources: List<String>,
    selectedQuality: String,
    playRequested: Boolean,
    loading: Boolean,
    canSeek: Boolean,
    bufferedPosition: Long,
    timelinePreview: Long?,
    previewSource: String?,
    currentPosition: Long,
    duration: Long,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onBack: () -> Unit,
    onOpenComments: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeek: (Long) -> Unit,
    onTimelinePreview: (Long?) -> Unit,
    onSelectQuality: (String) -> Unit,
    qualityMenuOpen: Boolean,
    onQualityMenuOpenChange: (Boolean) -> Unit,
    skipIntervals: List<SkipInterval> = emptyList(),
) {
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
            IconButton(onClick = onOpenComments) {
                Icon(
                    Icons.AutoMirrored.Filled.Comment,
                    contentDescription = "Комментарии к серии",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Box {
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            onQualityMenuOpenChange(true)
                        }
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
                    expanded = qualityMenuOpen,
                    onDismissRequest = { onQualityMenuOpenChange(false) },
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
                                onQualityMenuOpenChange(false)
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        // ── Center: |◁  ◁10  ▶⏸  10▷  ▷| ──────────────────────
        if (timelinePreview == null) Row(
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
            IconButton(onClick = onSeekBack, enabled = canSeek, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Filled.Replay10,
                    contentDescription = "−10 сек",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
            Box(
                modifier = Modifier.size(76.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                    )
                }
                Box(
                    modifier = Modifier.size(66.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onPlayPause,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (playRequested) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playRequested) "Пауза" else "Воспроизвести",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            IconButton(onClick = onSeekForward, enabled = canSeek, modifier = Modifier.size(48.dp)) {
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

        PlayerTimeline(
            positionMs = currentPosition,
            durationMs = duration,
            bufferedMs = bufferedPosition,
            previewMs = timelinePreview,
            sourceUrl = previewSource,
            enabled = canSeek,
            skipIntervals = skipIntervals,
            onPreview = onTimelinePreview,
            onSeek = onSeek,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                .fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
}
