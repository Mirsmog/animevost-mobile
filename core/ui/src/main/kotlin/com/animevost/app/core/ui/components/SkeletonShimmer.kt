package com.animevost.app.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.animevost.app.core.ui.theme.Bg3
import com.animevost.app.core.ui.theme.Bg4

// ── Core shimmer brush ─────────────────────────────────────────

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )
    return Brush.linearGradient(
        colors = listOf(Bg3, Bg4, Bg3),
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 0f),
    )
}

// ── Generic shimmer box ────────────────────────────────────────

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    radius: Int = 8,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius.dp))
            .background(shimmerBrush()),
    )
}

// ── Home screen skeleton ───────────────────────────────────────

@Composable
fun HomeShimmer(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        // Sort chips row + layout button placeholder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(4) {
                    ShimmerBox(
                        modifier = Modifier.height(32.dp).width(72.dp),
                        radius = 16,
                    )
                }
            }
            // Layout toggle button placeholder
            ShimmerBox(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(22.dp),
                radius = 4,
            )
        }

        // Grid card rows (3-col)
        repeat(4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(3) {
                    ShimmerBox(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.67f),
                        radius = 8,
                    )
                }
            }
        }
    }
}

// ── Horizontal list item skeleton ─────────────────────────────

@Composable
fun HorizontalCardShimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        ShimmerBox(modifier = Modifier.width(56.dp).height(80.dp), radius = 6)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp))
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp))
        }
    }
}

// ── Generic list of horizontal shimmer items ──────────────────

@Composable
fun ListShimmer(count: Int = 6, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        repeat(count) {
            HorizontalCardShimmer()
        }
    }
}
