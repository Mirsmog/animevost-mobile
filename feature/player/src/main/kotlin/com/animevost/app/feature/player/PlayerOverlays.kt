package com.animevost.app.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    if (secs > 0) displaySecs = secs

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(80)),
        exit = fadeOut(tween(400)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(width = 100.dp, height = 160.dp)
                .background(
                    Color.White.copy(alpha = 0.18f),
                    if (isForward) RoundedCornerShape(topStart = 90.dp, bottomStart = 90.dp)
                    else RoundedCornerShape(topEnd = 90.dp, bottomEnd = 90.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = if (isForward) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
                AnimatedContent(
                    targetState = displaySecs,
                    transitionSpec = {
                        slideInVertically { -it } + fadeIn(tween(120)) togetherWith
                            slideOutVertically { it } + fadeOut(tween(80))
                    },
                    label = "seek_secs",
                ) { s ->
                    Text(
                        text = if (isForward) "+${s} сек" else "−${s} сек",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

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
