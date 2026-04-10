@file:Suppress("DEPRECATION")

package com.animevost.app.feature.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.animevost.app.core.domain.model.SkipSegment
import com.animevost.app.core.domain.model.SkipType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import kotlin.math.abs
import kotlin.math.roundToInt

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
    // Signed: positive = forward, negative = backward
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

    // ── Skip segment state ───────────────────────────────────
    val activeSkip = state.skipSegments.firstOrNull { seg ->
        currentPosition in seg.startMs..seg.endMs
    }
    var skipButtonVisible by remember { mutableStateOf(false) }
    var skipDismissed by remember { mutableStateOf<Long?>(null) }

    // Show skip button when entering a segment, auto-hide after 5s
    LaunchedEffect(activeSkip?.startMs, activeSkip?.type) {
        if (activeSkip != null && skipDismissed != activeSkip.startMs) {
            skipButtonVisible = true
            delay(5_000)
            skipButtonVisible = false
        } else {
            skipButtonVisible = false
        }
    }
    // Reset dismissed when leaving segment
    LaunchedEffect(activeSkip) {
        if (activeSkip == null) skipDismissed = null
    }
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
                if (playbackState == Player.STATE_READY) {
                    val dur = exoPlayer.duration
                    if (dur > 0L) viewModel.onEvent(PlayerEvent.VideoReady(dur))
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            // Save final position before releasing
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
            if (saveCounter >= 10) { // every ~5 s
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
                        // Tap + hold = 2x speed / double-tap = seek ±10s / single-tap = toggle
                        launch {
                            var lastTapUpMs = 0L
                            detectTapGestures(
                                onPress = { offset ->
                                    val pressTime = System.currentTimeMillis()
                                    val isSecondTap = pressTime - lastTapUpMs < 400 &&
                                        lastTapUpMs > 0

                                    if (isSecondTap) {
                                        lastTapUpMs = 0L
                                        val releasedQuickly =
                                            kotlinx.coroutines.withTimeoutOrNull(200L) {
                                                tryAwaitRelease()
                                            } != null

                                        if (!releasedQuickly) {
                                            if (!isSpeedLocked) {
                                                isSpeedBoosting = true
                                                exoPlayer.setPlaybackParameters(
                                                    PlaybackParameters(2.0f),
                                                )
                                                haptic.performHapticFeedback(
                                                    HapticFeedbackType.LongPress,
                                                )
                                                tryAwaitRelease()
                                                isSpeedBoosting = false
                                                exoPlayer.setPlaybackParameters(
                                                    PlaybackParameters(
                                                        if (isSpeedLocked) lockedSpeed else 1.0f
                                                    ),
                                                )
                                            } else {
                                                tryAwaitRelease()
                                            }
                                        }
                                    } else {
                                        tryAwaitRelease()
                                        lastTapUpMs = System.currentTimeMillis()
                                    }
                                },
                                onTap = {
                                    if (!isSpeedBoosting) {
                                        controlsVisible = !controlsVisible
                                    }
                                },
                                onDoubleTap = { offset ->
                                    if (isSpeedBoosting) return@detectTapGestures
                                    lastTapUpMs = 0L
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (offset.x < size.width / 2) {
                                        seekAccum -= 10_000L
                                    } else {
                                        seekAccum += 10_000L
                                    }
                                    seekJob?.cancel()
                                    seekJob = seekScope.launch {
                                        delay(600)
                                        val accum = seekAccum
                                        exoPlayer.seekTo(
                                            (exoPlayer.currentPosition + accum).coerceIn(0L, duration)
                                        )
                                        seekAccum = 0L
                                    }
                                },
                            )
                        }
                        // Horizontal drag → seek preview (full width ≈ 2 min)
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
                                    if (isHorizontal == null &&
                                        (abs(totalX) > 25 || abs(totalY) > 25)
                                    ) {
                                        isHorizontal = abs(totalX) > abs(totalY)
                                        if (isHorizontal == true) controlsVisible = false
                                    }
                                    if (isHorizontal == true) {
                                        val delta =
                                            (totalX / size.width.toFloat() * 120_000L).toLong()
                                        seekPreviewMs =
                                            (startMs + delta).coerceIn(0L, duration)
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

        // ── Speed boost / lock indicator ────────────────────────
        AnimatedVisibility(
            visible = isSpeedBoosting || isSpeedLocked,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp),
        ) {
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showSpeedPopup = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isSpeedLocked) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = if (isSpeedBoosting && !isSpeedLocked) "▶▶ 2x"
                           else formatSpeed(lockedSpeed),
                    style = MaterialTheme.typography.titleMedium,
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
                nextName = state.allEpisodes
                    .getOrNull(state.currentEpisodeIndex + 1)?.name ?: "",
                onConfirm = {
                    viewModel.onEvent(PlayerEvent.NextEpisode(exoPlayer.currentPosition, exoPlayer.duration))
                    showAutoNext = false
                },
                onCancel = { showAutoNext = false },
            )
        }

        // ── Skip intro/outro button ──────────────────────────────
        AnimatedVisibility(
            visible = skipButtonVisible && activeSkip != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 90.dp),
        ) {
            activeSkip?.let { skip ->
                SkipButton(
                    label = when (skip.type) {
                        SkipType.INTRO -> "Пропустить интро"
                        SkipType.OUTRO -> "Пропустить аутро"
                    },
                    onClick = {
                        exoPlayer.seekTo(skip.endMs)
                        skipButtonVisible = false
                        skipDismissed = skip.startMs
                    },
                )
            }
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
                skipSegments = state.skipSegments,
                onBack = onBack,
                onPlayPause = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                onPrevious = { viewModel.onEvent(PlayerEvent.PreviousEpisode(exoPlayer.currentPosition, exoPlayer.duration)) },
                onNext = { viewModel.onEvent(PlayerEvent.NextEpisode(exoPlayer.currentPosition, exoPlayer.duration)) },
                onSeekBack = {
                    exoPlayer.seekTo(maxOf(0L, exoPlayer.currentPosition - 10_000L))
                },
                onSeekForward = {
                    exoPlayer.seekTo(minOf(duration, exoPlayer.currentPosition + 10_000L))
                },
                onSeek = { fraction ->
                    exoPlayer.seekTo((fraction * duration).toLong())
                },
                onSelectQuality = { viewModel.onEvent(PlayerEvent.SelectQuality(it)) },
            )
        }

        // ── Speed picker popup (on top of everything) ────────────
        if (showSpeedPopup) {
            SpeedPickerPopup(
                selectedSpeed = lockedSpeed,
                isLocked = isSpeedLocked,
                onSpeedSelect = { speed ->
                    lockedSpeed = speed
                    if (isSpeedLocked) {
                        exoPlayer.setPlaybackParameters(PlaybackParameters(speed))
                    }
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
    }
}

// ─── Seek pill animation (YouTube-style accumulated) ─────────────────────────

@Composable
private fun SeekAnimOverlay(
    visible: Boolean,
    isForward: Boolean,
    seekMs: Long,
    modifier: Modifier = Modifier,
) {
    // Keep last non-zero value so the fade-out doesn't flash "0 сек"
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
                    imageVector = if (isForward) Icons.Filled.FastForward
                    else Icons.Filled.FastRewind,
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
private fun AutoNextCard(
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

// ─── Full player controls overlay ─────────────────────────────────────────────

@Composable
private fun PlayerControls(
    episodeName: String,
    videoSources: List<String>,
    selectedQuality: String,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    hasPrevious: Boolean,
    hasNext: Boolean,
    skipSegments: List<SkipSegment>,
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
            // Quality badge
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
            // Play / Pause — big orange circle
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
                    imageVector = if (isPlaying) Icons.Filled.Pause
                    else Icons.Filled.PlayArrow,
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
                fraction = if (duration > 0) currentPosition.toFloat() / duration.toFloat()
                else 0f,
                onSeek = onSeek,
                skipSegments = skipSegments,
                durationMs = duration,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─── Thin seek bar with orange circle thumb ──────────────────────────────────

@Composable
private fun ThinSeekBar(
    fraction: Float,
    onSeek: (Float) -> Unit,
    skipSegments: List<SkipSegment>,
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val displayFraction = if (dragging) dragFraction else fraction.coerceIn(0f, 1f)
    val thumbDiameter = 14.dp

    // Segment colors
    val introColor = Color(0xFF42A5F5) // blue
    val outroColor = Color(0xFFAB47BC) // purple

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .height(28.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        dragFraction =
                            (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek(dragFraction)
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, _ ->
                        dragFraction =
                            (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek(dragFraction)
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeek(
                        (offset.x / size.width.toFloat()).coerceIn(0f, 1f),
                    )
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackHeight = 3.dp
        val trackShape = RoundedCornerShape(1.5.dp)
        val thumbOffset = (maxWidth * displayFraction - thumbDiameter / 2)
            .coerceAtLeast(0.dp)

        // Inactive track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .background(Color.White.copy(alpha = 0.2f), trackShape),
        )

        // Skip segment highlights
        if (durationMs > 0) {
            for (seg in skipSegments) {
                val startFrac = (seg.startMs.toFloat() / durationMs).coerceIn(0f, 1f)
                val endFrac = (seg.endMs.toFloat() / durationMs).coerceIn(0f, 1f)
                val segWidth = maxWidth * (endFrac - startFrac)
                val segOffset = maxWidth * startFrac
                val segColor = if (seg.type == SkipType.INTRO) introColor else outroColor

                Box(
                    modifier = Modifier
                        .offset(x = segOffset)
                        .width(segWidth)
                        .height(trackHeight)
                        .background(segColor.copy(alpha = 0.7f), trackShape),
                )
            }
        }

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

// ─── Skip button (Netflix-style) ──────────────────────────────────────────────

@Composable
private fun SkipButton(
    label: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.9f),
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(40.dp),
    ) {
        Text(
            text = label,
            color = Color.Black,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ─── Speed picker popup ───────────────────────────────────────────────────────

private val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f, 4.0f, 5.0f)

private fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x"

@Composable
private fun SpeedPickerPopup(
    selectedSpeed: Float,
    isLocked: Boolean,
    onSpeedSelect: (Float) -> Unit,
    onLockToggle: () -> Unit,
    onDismiss: () -> Unit,
) {
    val initialIndex = speedOptions.indexOf(selectedSpeed).coerceAtLeast(0)
    var sliderIndex by remember(selectedSpeed) { mutableFloatStateOf(initialIndex.toFloat()) }
    val currentSpeed = speedOptions[sliderIndex.roundToInt()]

    // Fullscreen overlay — no Dialog window, so centering is always exact
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 260.dp, max = 380.dp)
                .background(Color(0xF0111118), RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Скорость",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.5f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatSpeed(currentSpeed),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderIndex,
                    onValueChange = { raw ->
                        sliderIndex = raw
                        onSpeedSelect(speedOptions[raw.roundToInt()])
                    },
                    valueRange = 0f..(speedOptions.size - 1).toFloat(),
                    steps = speedOptions.size - 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatSpeed(speedOptions.first()),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                    Text(
                        text = formatSpeed(speedOptions.last()),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isLocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(10.dp),
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onLockToggle,
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = null,
                        tint = if (isLocked) MaterialTheme.colorScheme.primary
                               else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = if (isLocked) "Отключить закрепление" else "Закрепить скорость",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isLocked) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isLocked) MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
