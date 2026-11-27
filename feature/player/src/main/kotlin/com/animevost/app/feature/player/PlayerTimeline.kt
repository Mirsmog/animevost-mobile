package com.animevost.app.feature.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.animevost.app.core.domain.model.SkipInterval
import com.animevost.app.core.domain.model.SkipType
import kotlin.math.roundToLong

@Composable
internal fun PlayerTimeline(
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    previewMs: Long?,
    sourceUrl: String?,
    enabled: Boolean,
    skipIntervals: List<SkipInterval>,
    onPreview: (Long?) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayMs = previewMs ?: positionMs
    val total = durationMs.coerceAtLeast(1L)
    val fraction = (displayMs.toFloat() / total).coerceIn(0f, 1f)
    val preview by rememberUpdatedState(onPreview)
    val commit by rememberUpdatedState(onSeek)
    val playhead by rememberUpdatedState(positionMs)
    var originMs by remember(sourceUrl) { mutableLongStateOf(0L) }
    val image = rememberTimelineFrame(sourceUrl, previewMs)
    val accent = MaterialTheme.colorScheme.primary
    val inset = 8.dp

    Column(modifier) {
        if (previewMs != null) {
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = AbsoluteAlignment.BottomLeft) {
                val bubbleWidth = minOf(116.dp, maxWidth)
                val thumbX = inset + (maxWidth - inset * 2).coerceAtLeast(0.dp) * fraction
                val x = (thumbX - bubbleWidth / 2).coerceIn(0.dp, maxWidth - bubbleWidth)
                Box(Modifier.absoluteOffset(x = x).width(bubbleWidth), contentAlignment = Alignment.BottomCenter) {
                    Column(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.88f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (image != null) {
                            Image(
                                bitmap = image,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.width(bubbleWidth).aspectRatio(16f / 9f),
                            )
                        }
                        Text(
                            formatTime(previewMs), Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White, style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (previewMs == null) formatTime(positionMs) else formatSeekDelta(previewMs - originMs),
                color = Color.White, style = MaterialTheme.typography.labelLarge,
            )
            Text(formatTime(durationMs), color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelLarge)
        }

        Canvas(
            modifier = Modifier.fillMaxWidth().height(48.dp)
                .semantics {
                    contentDescription = "Позиция воспроизведения"
                    stateDescription = formatTime(displayMs)
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                    if (enabled) {
                        setProgress { value ->
                            commit((value.coerceIn(0f, 1f).toDouble() * total).roundToLong())
                            true
                        }
                    } else disabled()
                }
                .pointerInput(sourceUrl, durationMs, enabled) {
                    if (!enabled) return@pointerInput
                    val start = inset.toPx()
                    fun timeAt(x: Float): Long {
                        val f = ((x - start) / (size.width - 2 * start).coerceAtLeast(1f)).coerceIn(0f, 1f)
                        return (f.toDouble() * total).roundToLong()
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        try {
                            originMs = playhead
                            preview(timeAt(down.position.x))
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.size != 1) break
                                val finger = event.changes.single()
                                if (finger.id != down.id || finger.isConsumed) break
                                val target = timeAt(finger.position.x)
                                val released = finger.changedToUp()
                                finger.consume()
                                if (released) {
                                    commit(target)
                                    break
                                }
                                preview(target)
                            }
                        } finally {
                            preview(null)
                        }
                    }
                },
        ) {
            val start = inset.toPx().coerceAtMost(size.width / 2)
            val end = size.width - start
            val y = size.height / 2
            val thickness = if (previewMs != null) 5.dp.toPx() else 3.dp.toPx()
            fun xAt(ms: Long) = start + (end - start) * (ms.toFloat() / total).coerceIn(0f, 1f)
            fun track(color: Color, from: Float, to: Float) {
                if (to > from) drawLine(color, Offset(from, y), Offset(to, y), thickness, StrokeCap.Round)
            }
            track(Color.White.copy(alpha = 0.22f), start, end)
            track(Color.White.copy(alpha = 0.45f), start, xAt(bufferedMs))
            track(accent, start, xAt(displayMs))
            for (interval in skipIntervals) {
                val color = if (interval.type == SkipType.OP) Color(0xFFFFB300) else Color(0xFF26C6DA)
                track(color, xAt(interval.startMs), xAt(interval.endMs))
            }
            drawCircle(accent, if (previewMs != null) start else 6.dp.toPx(), Offset(xAt(displayMs), y))
        }
    }
}
