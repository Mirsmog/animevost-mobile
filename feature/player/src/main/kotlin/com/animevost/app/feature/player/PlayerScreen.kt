package com.animevost.app.feature.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.HighQuality
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Lock landscape
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation =
                originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    var previousVideoId by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    // -10 = rewind feedback, +10 = forward feedback, null = none
    var seekFeedback by remember { mutableStateOf<Int?>(null) }

    // Auto-hide controls after 3s
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    // Clear seek feedback after 0.8s
    LaunchedEffect(seekFeedback) {
        if (seekFeedback != null) {
            delay(800)
            seekFeedback = null
        }
    }

    LaunchedEffect(state.currentVideoUrl, state.currentEpisode?.videoId) {
        val url = state.currentVideoUrl ?: return@LaunchedEffect
        val currentEpisodeId = state.currentEpisode?.videoId
        val isSameEpisode = previousVideoId != null && previousVideoId == currentEpisodeId
        val currentPosition = exoPlayer.currentPosition
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        if (isSameEpisode && currentPosition > 0) {
            exoPlayer.seekTo(currentPosition)
        }
        exoPlayer.playWhenReady = true
        previousVideoId = currentEpisodeId
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                    },
                    onDoubleTap = { offset ->
                        val isLeftSide = offset.x < size.width / 2f
                        if (isLeftSide) {
                            exoPlayer.seekTo(maxOf(0L, exoPlayer.currentPosition - 10_000L))
                            seekFeedback = -10
                        } else {
                            exoPlayer.seekTo(exoPlayer.currentPosition + 10_000L)
                            seekFeedback = 10
                        }
                        controlsVisible = true
                    },
                )
            },
    ) {
        // Video surface — always visible, no controller (we draw our own)
        if (state.currentVideoUrl != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view -> view.player = exoPlayer },
            )
        }

        // Loading spinner
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
            )
        }

        // Error message
        if (state.error != null) {
            Text(
                text = state.error!!,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Seek feedback indicators
        SeekFeedbackOverlay(
            seekFeedback = seekFeedback,
            modifier = Modifier.fillMaxSize(),
        )

        // Top + Bottom overlays (auto-hide)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top gradient + controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.75f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                ) {
                    PlayerTopOverlay(
                        episodeName = state.currentEpisode?.name ?: "",
                        videoSources = state.videoSources.map { it.quality },
                        selectedQuality = state.selectedQuality,
                        onBack = onBack,
                        onSelectQuality = { viewModel.onEvent(PlayerEvent.SelectQuality(it)) },
                    )
                }

                // Bottom gradient + episode nav
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f),
                                ),
                            ),
                        ),
                ) {
                    PlayerBottomOverlay(
                        hasPrevious = state.hasPrevious,
                        hasNext = state.hasNext,
                        onPrevious = { viewModel.onEvent(PlayerEvent.PreviousEpisode) },
                        onNext = { viewModel.onEvent(PlayerEvent.NextEpisode) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SeekFeedbackOverlay(
    seekFeedback: Int?,
    modifier: Modifier = Modifier,
) {
    if (seekFeedback == null) return
    val isForward = seekFeedback > 0
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(if (isForward) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (isForward) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = if (isForward) "+10 сек" else "-10 сек",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PlayerTopOverlay(
    episodeName: String,
    videoSources: List<String>,
    selectedQuality: String,
    onBack: () -> Unit,
    onSelectQuality: (String) -> Unit,
) {
    var showQualityMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = Color.White,
            )
        }

        Text(
            text = episodeName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.weight(1f),
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
                    .padding(horizontal = 10.dp, vertical = 6.dp),
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
                                fontWeight = if (quality == selectedQuality) FontWeight.Bold else FontWeight.Normal,
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

        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
private fun PlayerBottomOverlay(
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = hasPrevious,
        ) {
            Icon(
                Icons.Filled.SkipPrevious,
                contentDescription = "Предыдущая серия",
                tint = if (hasPrevious) Color.White else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(modifier = Modifier.width(64.dp))
        IconButton(
            onClick = onNext,
            enabled = hasNext,
        ) {
            Icon(
                Icons.Filled.SkipNext,
                contentDescription = "Следующая серия",
                tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(40.dp),
            )
        }
    }
}
