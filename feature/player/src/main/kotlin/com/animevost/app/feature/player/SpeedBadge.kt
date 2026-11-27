package com.animevost.app.feature.player

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SpeedBadge(speed: Float, locked: Boolean, showHint: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Text(
                text = "▶▶ ${formatSpeed(speed)}",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp, end = 6.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClickLabel = "Настроить скорость", onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
            if (locked) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).size(13.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Lock, null, Modifier.size(8.dp), tint = Color.White)
                }
            }
        }
        if (showHint) {
            val transition = rememberInfiniteTransition(label = "Tap speed badge")
            val approach by transition.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(580), RepeatMode.Reverse),
                label = "Fingertip",
            )
            Icon(
                Icons.Filled.TouchApp, contentDescription = null, tint = Color.White,
                modifier = Modifier.size(36.dp).graphicsLayer { translationY = -9.dp.toPx() * approach },
            )
            Text(
                "Нажми на плашку", color = Color.White, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
