@file:Suppress("DEPRECATION")

package com.animevost.app.feature.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class SeekSide { BACK, FORWARD }

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as Activity
    val state by viewModel.uiState.collectAsState()
    val skipSnackbar by viewModel.skipSnackbar.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    // ── Playback state ───────────────────────────────────────────
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    // ── UI state ─────────────────────────────────────────────────
    var controlsVisible by remember { mutableStateOf(true) }
    var showSeekPreview by remember { mutableStateOf(false) }
    var seekPreviewMs by remember { mutableLongStateOf(0L) }

    // ── YouTube-style accumulated seek ───────────────────────────
    val seekScope = rememberCoroutineScope()
    var seekAccum by remember { mutableLongStateOf(0L) }
    var seekJob by remember { mutableStateOf<Job?>(null) }

    // ── Auto-next ────────────────────────────────────────────────
    var showAutoNext by remember { mutableStateOf(false) }
    var autoNextCountdown by remember { mutableIntStateOf(5) }

    // ── Speed boost (tap + hold) ────────────────────────────────
    var isSpeedBoosting by remember { mutableStateOf(false) }

    // ── Speed lock (persistent speed without holding) ─────────
    var isSpeedLocked by remember { mutableStateOf(false) }
    var lockedSpeed by remember { mutableFloatStateOf(2.0f) }
    var showSpeedPopup by remember { mutableStateOf(false) }

    // ── Skip segment editor ──────────────────────────────────
    var showSkipEditor by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val origOrientation = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val window = activity.window
        val insetsCtrl = WindowCompat.getInsetsController(window, window.decorView)
        insetsCtrl.hide(WindowInsetsCompat.Type.systemBars())
        insetsCtrl.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity.requestedOrientation =
                origOrientation.takeIf { it != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
                    ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsCtrl.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }

    var previousVideoId by remember { mutableStateOf<String?>(null) }

    // ── Load video when URL / episode changes ────────────────────
    LaunchedEffect(state.currentVideoUrl, state.currentEpisode?.videoId) {
        val url = state.currentVideoUrl ?: return@LaunchedEffect
        val episodeId = state.currentEpisode?.videoId
        val isSameEpisode = previousVideoId != null && previousVideoId == episodeId
        val savedPos = exoPlayer.currentPosition
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        if (isSameEpisode && savedPos > 0) exoPlayer.seekTo(savedPos)
        exoPlayer.playWhenReady = true
        previousVideoId = episodeId
        showAutoNext = false
    }

    // Seek to saved resume position once the player becomes ready
    LaunchedEffect(state.resumePositionMs) {
        val resumeMs = state.resumePositionMs
        if (resumeMs > 0L) {
            exoPlayer.seekTo(resumeMs)
            viewModel.onEvent(PlayerEvent.ResumeConsumed)
        }
    }

    // ── Player listener ──────────────────────────────────────────
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED && state.hasNext) {
                    showAutoNext = true
                    autoNextCountdown = 5
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (pos > 0L) viewModel.onEvent(PlayerEvent.UpdateProgress(pos, dur))
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // ── Position ticker (every 500 ms) + progress save every 5 s ─
    LaunchedEffect(Unit) {
        var saveCounter = 0
        while (true) {
            delay(500)
            currentPosition = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (dur > 0) duration = dur
            saveCounter++
            if (saveCounter >= 10) {
                saveCounter = 0
                val pos = exoPlayer.currentPosition
                if (pos > 0L && dur > 0L) {
                    viewModel.onEvent(PlayerEvent.UpdateProgress(pos, dur))
                }
            }
        }
    }

    // ── Auto-hide controls (3.5 s) ──────────────────────────────
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3_500)
            controlsVisible = false
        }
    }

    // ── Auto-next countdown ──────────────────────────────────────
    LaunchedEffect(showAutoNext) {
        if (!showAutoNext) return@LaunchedEffect
        for (i in 4 downTo 0) {
            delay(1_000)
            autoNextCountdown = i
        }
        if (showAutoNext) {
            viewModel.onEvent(PlayerEvent.NextEpisode(exoPlayer.currentPosition, exoPlayer.duration))
            showAutoNext = false
        }
    }

    // ── Start skip detection when segments are ready ─────────────
    LaunchedEffect(state.skipSegments, state.currentVideoUrl) {
        if (state.skipSegments.isNotEmpty() && state.currentVideoUrl != null) {
            viewModel.startSkipDetection(exoPlayer)
        }
    }

    // ── Lifecycle pause / resume ─────────────────────────────────
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (isPlaying) exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ═══════════════════════════════════════════════════════════════
    // UI
    // ═══════════════════════════════════════════════════════════════

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Video surface
        if (state.currentVideoUrl != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply { player = exoPlayer; useController = false }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view -> view.player = exoPlayer },
            )
        }

        // ── Gesture layer ────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isSpeedLocked, lockedSpeed) {
                    kotlinx.coroutines.coroutineScope {
                        launch {
                            var lastTapUpMs = 0L
                            detectTapGestures(
                                onPress = { offset ->
                                    val pressTime = System.currentTimeMillis()
                                    val isSecondTap = pressTime - lastTapUpMs < 400 && lastTapUpMs > 0
                                    if (isSecondTap) {
                                        lastTapUpMs = 0L
                                        val releasedQuickly =
                                            kotlinx.coroutines.withTimeoutOrNull(200L) {
                                                tryAwaitRelease()
                                            } != null
                                        if (!releasedQuickly) {
                                            val boostSpeed = if (isSpeedLocked) {
                                                (lockedSpeed * 2f).coerceAtMost(5f)
                                            } else 2.0f
                                            try {
                                                isSpeedBoosting = true
                                                exoPlayer.setPlaybackParameters(PlaybackParameters(boostSpeed))
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                tryAwaitRelease()
                                            } finally {
                                                isSpeedBoosting = false
                                                exoPlayer.setPlaybackParameters(
                                                    PlaybackParameters(if (isSpeedLocked) lockedSpeed else 1.0f),
                                                )
                                            }
                                        }
                                    } else {
                                        tryAwaitRelease()
                                        lastTapUpMs = System.currentTimeMillis()
                                    }
                                },
                                onTap = {
                                    if (!isSpeedBoosting) controlsVisible = !controlsVisible
                                },
                                onDoubleTap = { offset ->
                                    if (isSpeedBoosting) return@detectTapGestures
                                    lastTapUpMs = 0L
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (offset.x < size.width / 2) seekAccum -= 10_000L
                                    else seekAccum += 10_000L
                                    seekJob?.cancel()
                                    seekJob = seekScope.launch {
                                        delay(600)
                                        val accum = seekAccum
                                        exoPlayer.seekTo(
                                            (exoPlayer.currentPosition + accum).coerceIn(0L, duration),
                                        )
                                        seekAccum = 0L
                                    }
                                },
                            )
                        }
                        // Horizontal drag → seek preview
                        launch {
                            var startMs = 0L
                            var totalX = 0f
                            var totalY = 0f
                            var isHorizontal: Boolean? = null
                            detectDragGestures(
                                onDragStart = {
                                    startMs = exoPlayer.currentPosition
                                    totalX = 0f; totalY = 0f; isHorizontal = null
                                },
                                onDrag = { change, drag ->
                                    totalX += drag.x; totalY += drag.y
                                    if (isHorizontal == null && (abs(totalX) > 25 || abs(totalY) > 25)) {
                                        isHorizontal = abs(totalX) > abs(totalY)
                                        if (isHorizontal == true) controlsVisible = false
                                    }
                                    if (isHorizontal == true) {
                                        val delta = (totalX / size.width.toFloat() * 120_000L).toLong()
                                        seekPreviewMs = (startMs + delta).coerceIn(0L, duration)
                                        exoPlayer.seekTo(seekPreviewMs)
                                        showSeekPreview = true
                                    }
                                    change.consume()
                                },
                                onDragEnd = { showSeekPreview = false; isHorizontal = null },
                                onDragCancel = { showSeekPreview = false; isHorizontal = null },
                            )
                        }
                    }
                },
        )

        // ── Seek overlays ────────────────────────────────────────
        SeekAnimOverlay(
            visible = seekAccum < 0,
            isForward = false,
            seekMs = -seekAccum,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        SeekAnimOverlay(
            visible = seekAccum > 0,
            isForward = true,
            seekMs = seekAccum,
            modifier = Modifier.align(Alignment.CenterEnd),
        )

        // ── Horizontal seek time preview ─────────────────────────
        AnimatedVisibility(
            visible = showSeekPreview,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    text = formatTime(seekPreviewMs),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        // ── Buffering spinner ────────────────────────────────────
        if (isBuffering) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center).size(52.dp),
            )
        }

        // ── Initial loading ──────────────────────────────────────
        if (state.isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center),
                strokeWidth = 3.dp,
            )
        }

        // ── Error ────────────────────────────────────────────────
        if (state.error != null) {
            Text(
                text = state.error!!,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // ── Auto-next card ───────────────────────────────────────
        AnimatedVisibility(
            visible = showAutoNext,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
        ) {
            AutoNextCard(
                countdown = autoNextCountdown,
                nextName = state.allEpisodes.getOrNull(state.currentEpisodeIndex + 1)?.name ?: "",
                onConfirm = {
                    viewModel.onEvent(PlayerEvent.NextEpisode(exoPlayer.currentPosition, exoPlayer.duration))
                    showAutoNext = false
                },
                onCancel = { showAutoNext = false },
            )
        }

        // ── Controls overlay (fade in/out) ───────────────────────
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize(),
        ) {
            PlayerControls(
                episodeName = state.currentEpisode?.name ?: "",
                videoSources = state.videoSources.map { it.quality },
                selectedQuality = state.selectedQuality,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                hasPrevious = state.hasPrevious,
                hasNext = state.hasNext,
                onBack = onBack,
                onPlayPause = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                onPrevious = {
                    viewModel.onEvent(PlayerEvent.PreviousEpisode(exoPlayer.currentPosition, exoPlayer.duration))
                },
                onNext = {
                    viewModel.onEvent(PlayerEvent.NextEpisode(exoPlayer.currentPosition, exoPlayer.duration))
                },
                onSeekBack = {
                    exoPlayer.seekTo(maxOf(0L, exoPlayer.currentPosition - 10_000L))
                },
                onSeekForward = {
                    exoPlayer.seekTo(minOf(duration, exoPlayer.currentPosition + 10_000L))
                },
                onSeek = { fraction -> exoPlayer.seekTo((fraction * duration).toLong()) },
                onSelectQuality = { viewModel.onEvent(PlayerEvent.SelectQuality(it)) },
            )
        }

        // ── Speed boost / lock badge (above controls overlay) ────
        AnimatedVisibility(
            visible = isSpeedBoosting || (isSpeedLocked && controlsVisible),
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp),
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .padding(top = 6.dp, end = 6.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { showSpeedPopup = true }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isSpeedBoosting) {
                            val boostSpeed = if (isSpeedLocked) (lockedSpeed * 2f).coerceAtMost(5f) else 2.0f
                            "▶▶ ${formatSpeed(boostSpeed)}"
                        } else "▶▶ ${formatSpeed(lockedSpeed)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                if (isSpeedLocked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(13.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(8.dp),
                        )
                    }
                }
            }
        }

        // ── Speed picker popup (on top of everything) ────────────
        if (showSpeedPopup) {
            SpeedPickerPopup(
                selectedSpeed = lockedSpeed,
                isLocked = isSpeedLocked,
                onSpeedSelect = { speed ->
                    lockedSpeed = speed
                    if (isSpeedLocked) exoPlayer.setPlaybackParameters(PlaybackParameters(speed))
                },
                onLockToggle = {
                    if (isSpeedLocked) {
                        isSpeedLocked = false
                        exoPlayer.setPlaybackParameters(PlaybackParameters(1.0f))
                    } else {
                        isSpeedLocked = true
                        exoPlayer.setPlaybackParameters(PlaybackParameters(lockedSpeed))
                    }
                    showSpeedPopup = false
                },
                onDismiss = { showSpeedPopup = false },
            )
        }

        // ── Skip editor button (top-left, visible with controls) ─
        AnimatedVisibility(
            visible = controlsVisible && !showSkipEditor,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 110.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showSkipEditor = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "⏭ Скип",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }

        // ── Skip segment editor sheet ────────────────────────────
        AnimatedVisibility(
            visible = showSkipEditor,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            SkipSegmentEditorSheet(
                segments = state.skipSegments,
                editingType = state.editingSegmentType,
                pendingStartMs = state.pendingStartMs,
                currentPositionMs = currentPosition,
                onStartCapture = { type ->
                    viewModel.onEvent(PlayerEvent.SaveSkipStart(type, exoPlayer.currentPosition))
                },
                onEndCapture = { type ->
                    viewModel.onEvent(PlayerEvent.SaveSkipEnd(type, exoPlayer.currentPosition))
                },
                onDelete = { type -> viewModel.onEvent(PlayerEvent.DeleteSkip(type)) },
                onBeginEdit = { type -> viewModel.onEvent(PlayerEvent.BeginEditSegment(type)) },
                onDismiss = {
                    viewModel.onEvent(PlayerEvent.CancelEditSegment)
                    showSkipEditor = false
                },
            )
        }

        // ── Skip snackbar ────────────────────────────────────────
        SkipSnackbarOverlay(
            message = skipSnackbar?.message ?: "",
            visible = skipSnackbar != null,
            onUndo = {
                val pos = skipSnackbar?.undoPositionMs ?: return@SkipSnackbarOverlay
                exoPlayer.seekTo(pos)
                viewModel.onEvent(PlayerEvent.UndoSkip(pos))
            },
            onDismiss = { viewModel.onEvent(PlayerEvent.DismissSkipSnackbar) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
        )
    }
}
