package com.animevost.app.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Seek pill animation (YouTube-style accumulated) ─────────────────────────

@Composable
internal fun SeekAnimOverlay(
    visible: Boolean,
    isForward: Boolean,
    seekMs: Long,
    modifier: Modifier = Modifier,
) {
    var displaySecs by remember { mutableIntStateOf(0) }
    val secs = (seekMs / 1000).toInt()
    val pulses = remember { mutableStateListOf<Int>() }
    var nextPulseId by remember { mutableIntStateOf(0) }
    val pulseScope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(visible, secs) {
        if (visible && secs > 0) {
            displaySecs = secs
            val pulseId = nextPulseId++
            pulses += pulseId
            pulseScope.launch {
                delay(SEEK_PULSE_DURATION_MS)
                pulses.remove(pulseId)
            }
        } else if (!visible) {
            pulses.clear()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(90)),
        exit = fadeOut(tween(260)),
        modifier = modifier.padding(horizontal = 48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (!isForward) {
                SeekArrowStack(isForward = false, pulses = pulses)
                Spacer(modifier = Modifier.width(20.dp))
            }
            AnimatedContent(
                targetState = displaySecs,
                transitionSpec = {
                    scaleIn(
                        initialScale = 0.78f,
                        animationSpec = tween(130),
                    ) + fadeIn(tween(90)) togetherWith
                        scaleOut(
                            targetScale = 1.18f,
                            animationSpec = tween(110),
                        ) + fadeOut(tween(90))
                },
                label = "seek-duration",
            ) { seconds ->
                Text(
                    text = if (isForward) "+ $seconds" else "− $seconds",
                    color = Color.White,
                    fontSize = 24.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (isForward) {
                Spacer(modifier = Modifier.width(20.dp))
                SeekArrowStack(isForward = true, pulses = pulses)
            }
        }
    }
}

@Composable
private fun SeekArrowStack(
    isForward: Boolean,
    pulses: List<Int>,
) {
    Box(
        modifier = Modifier.size(width = 46.dp, height = 32.dp),
        contentAlignment = if (isForward) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        pulses.forEach { pulseId ->
            androidx.compose.runtime.key(pulseId) {
                AdditionalSeekChevron(isForward = isForward)
            }
        }
        SeekChevronGlyph(isForward = isForward)
    }
}

@Composable
private fun AdditionalSeekChevron(isForward: Boolean) {
    val progress = remember { Animatable(0f) }
    val shiftPx = with(LocalDensity.current) { 22.dp.toPx() }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = SEEK_ADDITIONAL_CHEVRON_MS,
                easing = LinearOutSlowInEasing,
            ),
        )
    }

    val value = progress.value
    val alpha = when {
        value < 0.16f -> value / 0.16f
        else -> ((1f - value) / 0.84f).coerceIn(0f, 1f)
    }
    SeekChevronGlyph(
        isForward = isForward,
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
                translationX = (if (isForward) 1f else -1f) * shiftPx * value
                val scale = 0.88f + 0.12f * (value / 0.25f).coerceAtMost(1f)
                scaleX = scale
                scaleY = scale
            },
    )
}

@Composable
private fun SeekChevronGlyph(
    isForward: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = 22.dp, height = 24.dp)) {
        val path = Path().apply {
            if (isForward) {
                moveTo(size.width * 4f / 22f, size.height * 4f / 32f)
                lineTo(size.width * 16f / 22f, size.height * 16f / 32f)
                lineTo(size.width * 4f / 22f, size.height * 28f / 32f)
            } else {
                moveTo(size.width * 18f / 22f, size.height * 4f / 32f)
                lineTo(size.width * 6f / 22f, size.height * 16f / 32f)
                lineTo(size.width * 18f / 22f, size.height * 28f / 32f)
            }
        }
        drawPath(
            path = path,
            color = Color.Black.copy(alpha = 0.8f),
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

private const val SEEK_ADDITIONAL_CHEVRON_MS = 460
private const val SEEK_PULSE_DURATION_MS = 520L

// ─── Auto-next episode card ───────────────────────────────────────────────────

@Composable
internal fun AutoNextCard(
    countdown: Int,
    nextName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEE0D0D14)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                "Следующая серия",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.6f),
            )
            Text(
                nextName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                ) { Text("Отмена", color = Color.White) }
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) { Text("Смотреть ($countdown)", color = Color.White) }
            }
        }
    }
}
