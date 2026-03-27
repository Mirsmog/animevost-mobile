package com.animevost.app.feature.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

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

    // Track previous videoId to detect quality-only changes vs episode changes
    var previousVideoId by remember { mutableStateOf<String?>(null) }

    // Set media when URL changes, restoring position on quality switch
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

    // Lifecycle handling
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
            .background(Color.Black),
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (state.error != null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.error!!,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        if (state.currentVideoUrl != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.player = exoPlayer
                },
            )
        }

        // Top overlay
        PlayerTopOverlay(
            episodeName = state.currentEpisode?.name ?: "",
            videoSources = state.videoSources.map { it.quality },
            selectedQuality = state.selectedQuality,
            onBack = onBack,
            onSelectQuality = { viewModel.onEvent(PlayerEvent.SelectQuality(it)) },
        )

        // Episode navigation
        PlayerBottomOverlay(
            hasPrevious = state.hasPrevious,
            hasNext = state.hasNext,
            onPrevious = { viewModel.onEvent(PlayerEvent.PreviousEpisode) },
            onNext = { viewModel.onEvent(PlayerEvent.NextEpisode) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
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
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 4.dp, vertical = 4.dp),
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
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.HighQuality,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = selectedQuality,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            DropdownMenu(
                expanded = showQualityMenu,
                onDismissRequest = { showQualityMenu = false },
            ) {
                videoSources.forEach { quality ->
                    DropdownMenuItem(
                        text = { Text(quality) },
                        onClick = {
                            onSelectQuality(quality)
                            showQualityMenu = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerBottomOverlay(
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
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
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(modifier = Modifier.width(48.dp))
        IconButton(
            onClick = onNext,
            enabled = hasNext,
        ) {
            Icon(
                Icons.Filled.SkipNext,
                contentDescription = "Следующая серия",
                tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(36.dp),
            )
        }
    }
}
