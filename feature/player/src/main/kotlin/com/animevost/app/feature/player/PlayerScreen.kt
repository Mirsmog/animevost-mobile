@file:Suppress("DEPRECATION")

package com.animevost.app.feature.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onWriteEpisodeComment: (Int) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as Activity
    val componentActivity = activity as ComponentActivity
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activeSkip by viewModel.activeSkip.collectAsStateWithLifecycle()
    val skipIntervals by viewModel.skipIntervals.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val player = rememberPlaybackController()

    if (player == null) {
        BackHandler {
            PlaybackService.stop(context.applicationContext)
            onBack()
        }
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
            )
        }
        return
    }

    // ── Playback state ───────────────────────────────────────────
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    var isBuffering by remember(player) {
        mutableStateOf(player.playbackState == Player.STATE_BUFFERING)
    }
    var currentPosition by rememberSaveable { mutableLongStateOf(player.currentPosition) }
    var duration by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
    var videoWidth by remember { mutableIntStateOf(16) }
    var videoHeight by remember { mutableIntStateOf(9) }
    var isInPictureInPictureMode by remember {
        mutableStateOf(activity.isInPictureInPictureMode)
    }

    // ── UI state ─────────────────────────────────────────────────
    var controlsVisible by remember { mutableStateOf(true) }
    var showSeekPreview by remember { mutableStateOf(false) }
    var seekPreviewMs by remember { mutableLongStateOf(0L) }
    var forcePortraitOnExit by remember { mutableStateOf(false) }
    val forcePortraitOnExitState = rememberUpdatedState(forcePortraitOnExit)

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

    val stopPlayback = {
        val position = player.currentPosition
        val playbackDuration = player.duration
        if (position > 0L) {
            viewModel.onEvent(PlayerEvent.UpdateProgress(position, playbackDuration))
        }
        PlayerPictureInPicture.clear(activity)
        player.stop()
        player.clearMediaItems()
        PlaybackService.stop(context.applicationContext)
    }
    val closePlayer = {
        stopPlayback()
        onBack()
    }

    BackHandler {
        if (state.isCommentsPanelVisible) {
            viewModel.onEvent(PlayerEvent.HideEpisodeComments)
            controlsVisible = true
        } else {
            closePlayer()
        }
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
            activity.requestedOrientation = if (forcePortraitOnExitState.value) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else {
                origOrientation.takeIf { it != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
                    ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            insetsCtrl.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val playbackActive by rememberUpdatedState(
        isPlaying && player.mediaItemCount > 0,
    )
    DisposableEffect(componentActivity) {
        val pipModeListener = Consumer<PictureInPictureModeChangedInfo> { info ->
            isInPictureInPictureMode = info.isInPictureInPictureMode
        }
        val userLeaveHintListener = Runnable {
            PlayerPictureInPicture.onUserLeaveHint(componentActivity, playbackActive)
        }
        componentActivity.addOnPictureInPictureModeChangedListener(pipModeListener)
        componentActivity.addOnUserLeaveHintListener(userLeaveHintListener)
        onDispose {
            componentActivity.removeOnPictureInPictureModeChangedListener(pipModeListener)
            componentActivity.removeOnUserLeaveHintListener(userLeaveHintListener)
            PlayerPictureInPicture.clear(componentActivity)
        }
    }

    LaunchedEffect(playbackActive, videoWidth, videoHeight) {
        PlayerPictureInPicture.update(
            activity = activity,
            playbackActive = playbackActive,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
        )
    }

    // ── Load video when URL / episode changes ────────────────────
    LaunchedEffect(
        state.currentVideoUrl,
        state.currentEpisode?.videoId,
        state.selectedQuality,
        state.resumePositionMs,
        state.animeTitle,
        state.posterUrl,
    ) {
        val url = state.currentVideoUrl ?: return@LaunchedEffect
        val episode = state.currentEpisode ?: return@LaunchedEffect
        val mediaId = playbackMediaId(episode.videoId, state.selectedQuality)
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(state.animeTitle.ifBlank { episode.name })
            .setSubtitle(episode.name)
            .setArtist(episode.name)
            .setAlbumTitle("AnimeVost")
            .apply {
                state.posterUrl.takeIf(String::isNotBlank)?.let { posterUrl ->
                    setArtworkUri(Uri.parse(posterUrl))
                }
            }
            .build()
        val mediaItem = MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(url)
            .setMediaMetadata(mediaMetadata)
            .build()
        val currentItem = player.currentMediaItem
        val sameMedia = currentItem?.mediaId == mediaId
        val sameEpisode = currentItem?.episodeVideoId() == episode.videoId

        if (sameMedia) {
            if (currentItem.mediaMetadata != mediaMetadata) {
                player.replaceMediaItem(player.currentMediaItemIndex, mediaItem)
            }
            if (state.resumePositionMs > 0L) {
                player.seekTo(state.resumePositionMs)
                viewModel.onEvent(PlayerEvent.ResumeConsumed)
            }
            return@LaunchedEffect
        }

        val startPosition = when {
            sameEpisode -> player.currentPosition
            state.resumePositionMs > 0L -> state.resumePositionMs
            else -> 0L
        }
        val shouldPlay = currentItem == null || player.playWhenReady
        player.setMediaItem(mediaItem, startPosition)
        player.prepare()
        player.playWhenReady = shouldPlay
        if (!sameEpisode) {
            currentPosition = 0L
            duration = 0L
        }
        if (state.resumePositionMs > 0L) {
            viewModel.onEvent(PlayerEvent.ResumeConsumed)
        }
        showAutoNext = false
    }

    // ── Player listener ──────────────────────────────────────────
    DisposableEffect(player, state.hasNext) {
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

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                viewModel.checkSkipPosition(newPosition.positionMs, player.duration)
            }
        }
        isPlaying = player.isPlaying
        isBuffering = player.playbackState == Player.STATE_BUFFERING
        player.addListener(listener)
        onDispose {
            val pos = player.currentPosition
            val dur = player.duration
            if (pos > 0L) viewModel.onEvent(PlayerEvent.UpdateProgress(pos, dur))
            player.removeListener(listener)
        }
    }

    // ── Position ticker (every 500 ms) + progress save every 5 s ─
    LaunchedEffect(player) {
        var saveCounter = 0
        while (true) {
            delay(500)
            val position = player.currentPosition
            currentPosition = position
            val dur = player.duration
            if (dur > 0) duration = dur
            viewModel.checkSkipPosition(position, dur)
            saveCounter++
            if (saveCounter >= 10) {
                saveCounter = 0
                val pos = player.currentPosition
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
            viewModel.onEvent(PlayerEvent.NextEpisode(player.currentPosition, player.duration))
            showAutoNext = false
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UI
    // ═══════════════════════════════════════════════════════════════

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Video surface
        if (state.currentVideoUrl != null || player.mediaItemCount > 0) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply { this.player = player; useController = false }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view -> view.player = player },
            )
        }

        if (isInPictureInPictureMode) return@Box

        // ── Gesture layer ────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isSpeedLocked, lockedSpeed) {
                    kotlinx.coroutines.coroutineScope {
                        launch {
                            var suppressNextTap = false
                            detectTapGestures(
                                onPress = {
                                    suppressNextTap = false
                                    val releasedBeforeBoost =
                                        kotlinx.coroutines.withTimeoutOrNull(SPEED_BOOST_HOLD_DELAY_MS) {
                                            tryAwaitRelease()
                                        } != null
                                    if (!releasedBeforeBoost) {
                                        suppressNextTap = true
                                        val boostSpeed = if (isSpeedLocked) {
                                            (lockedSpeed * 2f).coerceAtMost(5f)
                                        } else {
                                            2.0f
                                        }
                                        try {
                                            isSpeedBoosting = true
                                            player.setPlaybackParameters(PlaybackParameters(boostSpeed))
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            tryAwaitRelease()
                                        } finally {
                                            isSpeedBoosting = false
                                            player.setPlaybackParameters(
                                                PlaybackParameters(if (isSpeedLocked) lockedSpeed else 1.0f),
                                            )
                                        }
                                    }
                                },
                                onTap = {
                                    if (suppressNextTap) {
                                        suppressNextTap = false
                                    } else {
                                        controlsVisible = !controlsVisible
                                    }
                                },
                                onDoubleTap = { offset ->
                                    if (isSpeedBoosting) return@detectTapGestures
                                    suppressNextTap = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (offset.x < size.width / 2) seekAccum -= 10_000L
                                    else seekAccum += 10_000L
                                    seekJob?.cancel()
                                    seekJob = seekScope.launch {
                                        delay(600)
                                        val accum = seekAccum
                                        player.seekTo(
                                            (player.currentPosition + accum).coerceIn(0L, duration),
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
                                    startMs = player.currentPosition
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
                                        player.seekTo(seekPreviewMs)
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
                    viewModel.onEvent(PlayerEvent.NextEpisode(player.currentPosition, player.duration))
                    showAutoNext = false
                },
                onCancel = { showAutoNext = false },
            )
        }

        // ── Skip intro/outro button ─────────────────────────────
        run {
            val skip = activeSkip
            val isEd = skip?.type == com.animevost.app.core.domain.model.SkipType.ED
            val showAsNextEpisode = isEd && state.hasNext
            val visible = skip != null && !(isEd && !state.hasNext)
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(220)) + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut(tween(160)) + slideOutVertically(targetOffsetY = { it / 2 }),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 80.dp),
            ) {
                if (skip != null) {
                    val label = when {
                        showAsNextEpisode -> "Следующая серия"
                        skip.type == com.animevost.app.core.domain.model.SkipType.OP -> "Пропустить интро"
                        else -> "Пропустить концовку"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(6.dp))
                            .clickable {
                                if (showAsNextEpisode) {
                                    viewModel.onEvent(
                                        PlayerEvent.NextEpisode(
                                            player.currentPosition,
                                            player.duration,
                                        ),
                                    )
                                } else {
                                    player.seekTo(skip.endMs)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Icon(
                            imageVector = if (showAsNextEpisode) {
                                Icons.Filled.SkipNext
                            } else {
                                Icons.Filled.FastForward
                            },
                            contentDescription = null,
                            tint = Color(0xFF111111),
                            modifier = Modifier.size(18.dp),
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF111111),
                        )
                    }
                }
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
                onBack = closePlayer,
                onOpenComments = {
                    controlsVisible = false
                    viewModel.onEvent(PlayerEvent.ShowEpisodeComments)
                },
                onPlayPause = {
                    if (player.isPlaying) player.pause() else player.play()
                },
                onPrevious = {
                    viewModel.onEvent(PlayerEvent.PreviousEpisode(player.currentPosition, player.duration))
                },
                onNext = {
                    viewModel.onEvent(PlayerEvent.NextEpisode(player.currentPosition, player.duration))
                },
                onSeekBack = {
                    player.seekTo(maxOf(0L, player.currentPosition - 10_000L))
                },
                onSeekForward = {
                    player.seekTo(minOf(duration, player.currentPosition + 10_000L))
                },
                onSeek = { fraction -> player.seekTo((fraction * duration).toLong()) },
                onSelectQuality = { viewModel.onEvent(PlayerEvent.SelectQuality(it)) },
                skipIntervals = skipIntervals,
            )
        }

        // ── Speed boost / lock badge (above controls overlay) ────
        AnimatedVisibility(
            visible = isSpeedBoosting || (isSpeedLocked && controlsVisible),
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp),
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
                    if (isSpeedLocked) player.setPlaybackParameters(PlaybackParameters(speed))
                },
                onLockToggle = {
                    if (isSpeedLocked) {
                        isSpeedLocked = false
                        player.setPlaybackParameters(PlaybackParameters(1.0f))
                    } else {
                        isSpeedLocked = true
                        player.setPlaybackParameters(PlaybackParameters(lockedSpeed))
                    }
                    showSpeedPopup = false
                },
                onDismiss = { showSpeedPopup = false },
            )
        }

        AnimatedVisibility(
            visible = state.isCommentsPanelVisible,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(180)),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(140)),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            EpisodeCommentsPanel(
                episodeNumber = state.commentsEpisodeNumber ?: state.currentEpisodeIndex + 1,
                comments = state.episodeComments,
                isLoading = state.isLoadingEpisodeComments,
                hasMore = state.hasMoreEpisodeComments,
                error = state.episodeCommentsError,
                onClose = {
                    viewModel.onEvent(PlayerEvent.HideEpisodeComments)
                    controlsVisible = true
                },
                onLoadMore = {
                    viewModel.onEvent(PlayerEvent.LoadMoreEpisodeComments)
                },
                onRetry = {
                    viewModel.onEvent(PlayerEvent.RetryEpisodeComments)
                },
                onWriteComment = {
                    forcePortraitOnExit = true
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    val episodeNumber = state.commentsEpisodeNumber
                        ?: state.currentEpisode?.number
                        ?: state.currentEpisodeIndex + 1
                    stopPlayback()
                    onWriteEpisodeComment(episodeNumber)
                },
            )
        }
    }
}

private const val SPEED_BOOST_HOLD_DELAY_MS = 300L

private const val MEDIA_ID_SEPARATOR = "::quality::"

private fun playbackMediaId(videoId: String, quality: String): String =
    "$videoId$MEDIA_ID_SEPARATOR$quality"

private fun MediaItem.episodeVideoId(): String = mediaId.substringBefore(MEDIA_ID_SEPARATOR)
