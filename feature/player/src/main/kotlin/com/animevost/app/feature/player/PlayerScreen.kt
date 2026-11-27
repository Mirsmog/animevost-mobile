@file:Suppress("DEPRECATION")

package com.animevost.app.feature.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
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
    var playbackState by remember(player) { mutableIntStateOf(player.playbackState) }
    var playWhenReady by remember(player) { mutableStateOf(player.playWhenReady) }
    val playRequested = playWhenReady && playbackState != Player.STATE_ENDED
    var currentPosition by rememberSaveable { mutableLongStateOf(player.currentPosition) }
    var duration by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
    var bufferedPosition by remember(player) { mutableLongStateOf(player.bufferedPosition.coerceAtLeast(0L)) }
    var playingEpisodeId by remember(player) { mutableStateOf(player.currentMediaItem?.episodeVideoId()) }
    var mediaItemSeekable by remember(player) { mutableStateOf(player.isCurrentMediaItemSeekable) }
    val episodeId = state.currentEpisode?.videoId
    val currentEpisodeLoaded = episodeId != null && episodeId == playingEpisodeId
    val canSeek = currentEpisodeLoaded && mediaItemSeekable && duration > 0
    var videoWidth by remember { mutableIntStateOf(16) }
    var videoHeight by remember { mutableIntStateOf(9) }
    var isInPictureInPictureMode by remember {
        mutableStateOf(activity.isInPictureInPictureMode)
    }

    // ── UI state ─────────────────────────────────────────────────
    var controlsVisible by remember { mutableStateOf(true) }
    var timelinePreviewMs by remember(episodeId, state.selectedQuality) { mutableStateOf<Long?>(null) }
    // Null for the whole gesture, including time spent holding still.
    var lastTouchEnd by remember { mutableStateOf<Long?>(0L) }
    var showSeekPreview by remember { mutableStateOf(false) }
    var seekPreviewMs by remember { mutableLongStateOf(0L) }
    var seekStartMs by remember { mutableLongStateOf(0L) }
    var zoomToFill by rememberSaveable { mutableStateOf(false) }
    var zoomNotice by remember { mutableStateOf<String?>(null) }
    var qualityMenuOpen by remember { mutableStateOf(false) }
    var resumeAfterComments by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(zoomNotice) {
        if (zoomNotice != null) {
            delay(1000)
            zoomNotice = null
        }
    }

    // ── YouTube-style accumulated seek ───────────────────────────
    var seekAccum by remember(episodeId, state.selectedQuality) { mutableLongStateOf(0L) }

    // ── Auto-next ────────────────────────────────────────────────
    val episodeEnded = currentEpisodeLoaded && playbackState == Player.STATE_ENDED
    var autoNextDismissed by remember(episodeId, episodeEnded) { mutableStateOf(false) }
    var autoNextCountdown by remember(episodeId, episodeEnded) { mutableIntStateOf(5) }

    // ── Speed boost (tap + hold) ────────────────────────────────
    var isSpeedBoosting by remember { mutableStateOf(false) }

    // ── Speed lock (persistent speed without holding) ─────────
    var isSpeedLocked by rememberSaveable { mutableStateOf(false) }
    var lockedSpeed by rememberSaveable { mutableFloatStateOf(2.0f) }
    var showSpeedPopup by remember { mutableStateOf(false) }
    var hintEpisodeId by rememberSaveable { mutableStateOf<String?>(null) }
    val showSpeedHint = hintEpisodeId != null && hintEpisodeId == episodeId
    val baseSpeed = if (isSpeedLocked) lockedSpeed else 1f
    val boostSpeed = (baseSpeed * 2f).coerceAtMost(5f)
    val playbackSpeed = if (isSpeedBoosting) boostSpeed else baseSpeed
    DisposableEffect(player, baseSpeed, playbackSpeed) {
        if (player.playbackParameters.speed != playbackSpeed) player.setPlaybackSpeed(playbackSpeed)
        onDispose {
            if (playbackSpeed != baseSpeed) player.setPlaybackSpeed(baseSpeed)
        }
    }

    val showAutoNext = episodeEnded && state.hasNext && playWhenReady && !autoNextDismissed &&
        !qualityMenuOpen && !showSpeedPopup && !showSpeedHint && !state.isCommentsPanelVisible &&
        timelinePreviewMs == null && !showSeekPreview && seekAccum == 0L

    fun dismissSpeedHint() {
        val pausedEpisode = hintEpisodeId ?: return
        hintEpisodeId = null
        if (player.currentMediaItem?.episodeVideoId() == pausedEpisode && player.playbackState != Player.STATE_ENDED) {
            player.play()
        }
    }

    fun seekToPosition(position: Long) {
        seekAccum = 0L
        val end = player.duration
        if (
            end <= 0 || !player.isCurrentMediaItemSeekable ||
            episodeId == null || player.currentMediaItem?.episodeVideoId() != episodeId
        ) {
            return
        }
        if (episodeEnded) autoNextDismissed = true
        if (state.resumePosition != ResumePosition.Handled) viewModel.onEvent(PlayerEvent.ResumeConsumed(episodeId))
        currentPosition = position.coerceIn(0L, end)
        player.seekTo(currentPosition)
    }

    LaunchedEffect(player, episodeId, state.selectedQuality, seekAccum) {
        val delta = seekAccum
        if (delta != 0L) {
            delay(600)
            if (seekAccum == delta) seekToPosition(player.currentPosition + delta)
        }
    }

    fun saveProgress() {
        val videoId = player.currentMediaItem?.episodeVideoId() ?: return
        val position = player.currentPosition
        val playbackDuration = player.duration
        if (position > 0 && playbackDuration > 0) {
            viewModel.onEvent(PlayerEvent.UpdateProgress(videoId, position, playbackDuration))
        }
    }

    fun changeEpisode(event: PlayerEvent) {
        seekAccum = 0L
        autoNextDismissed = true
        saveProgress()
        viewModel.onEvent(event)
    }

    val stopPlayback = {
        saveProgress()
        PlayerPictureInPicture.clear(activity)
        player.stop()
        player.clearMediaItems()
        PlaybackService.stop(context.applicationContext)
    }
    val closePlayer = {
        seekAccum = 0L
        autoNextDismissed = true
        stopPlayback()
        onBack()
    }

    val closeComments = {
        viewModel.onEvent(PlayerEvent.HideEpisodeComments)
        controlsVisible = true
        if (resumeAfterComments) player.play()
        resumeAfterComments = false
    }

    BackHandler {
        when {
            state.isCommentsPanelVisible -> closeComments()
            showSpeedHint -> dismissSpeedHint()
            showSpeedPopup -> showSpeedPopup = false
            showAutoNext -> autoNextDismissed = true
            else -> closePlayer()
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
            activity.requestedOrientation = origOrientation
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
            return@LaunchedEffect
        }

        val shouldPlay = !state.isCommentsPanelVisible && (currentItem == null || player.playWhenReady)
        if (sameEpisode) {
            player.setMediaItem(mediaItem, player.currentPosition)
        } else {
            player.setMediaItem(mediaItem)
        }
        player.prepare()
        player.playWhenReady = shouldPlay
        if (!sameEpisode) {
            currentPosition = 0L
            duration = 0L
        }
    }

    LaunchedEffect(player, playbackState, playingEpisodeId, episodeId, state.resumePosition) {
        if (episodeId == null || player.currentMediaItem?.episodeVideoId() != episodeId) return@LaunchedEffect
        val ready = player.playbackState == Player.STATE_READY ||
            (player.playbackState == Player.STATE_ENDED && !player.isCurrentMediaItemSeekable)
        val consumed = state.resumePosition.consumeWhenReady(
            isReady = ready,
            canSeek = player.isCurrentMediaItemSeekable,
            seekTo = player::seekTo,
        )
        if (consumed) {
            viewModel.onEvent(PlayerEvent.ResumeConsumed(episodeId))
        }
    }

    // ── Player listener ──────────────────────────────────────────
    DisposableEffect(player) {
        fun updatePlayerState() {
            playbackState = player.playbackState
            playWhenReady = player.playWhenReady
            playingEpisodeId = player.currentMediaItem?.episodeVideoId()
            mediaItemSeekable = player.isCurrentMediaItemSeekable
        }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                updatePlayerState()
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
                currentPosition = newPosition.positionMs.coerceAtLeast(0L)
                viewModel.checkSkipPosition(newPosition.positionMs, player.duration)
            }
        }
        isPlaying = player.isPlaying
        player.addListener(listener)
        updatePlayerState()
        onDispose {
            saveProgress()
            player.removeListener(listener)
        }
    }

    // A headset or notification play action also dismisses the frozen frame.
    LaunchedEffect(playWhenReady, episodeId) {
        if (hintEpisodeId != null && (playWhenReady || hintEpisodeId != episodeId)) hintEpisodeId = null
    }

    // ── Position ticker (every 500 ms) + progress save every 5 s ─
    LaunchedEffect(player) {
        var saveCounter = 0
        while (true) {
            delay(500)
            val position = player.currentPosition
            currentPosition = position
            bufferedPosition = player.bufferedPosition.coerceAtLeast(0L)
            val dur = player.duration
            if (dur > 0) duration = dur
            viewModel.checkSkipPosition(position, dur)
            saveCounter++
            if (saveCounter >= 10) {
                saveCounter = 0
                saveProgress()
            }
        }
    }

    // ── Auto-hide controls (3.5 s) ──────────────────────────────
    val canAutoHide = controlsVisible && isPlaying && !qualityMenuOpen && !showSpeedPopup && !showSpeedHint &&
        !state.isCommentsPanelVisible && !isInPictureInPictureMode && timelinePreviewMs == null && !showAutoNext
    LaunchedEffect(canAutoHide, lastTouchEnd) {
        val releasedAt = lastTouchEnd
        if (canAutoHide && releasedAt != null) {
            delay(3_500)
            if (releasedAt == lastTouchEnd) controlsVisible = false
        }
    }

    // ── Auto-next countdown ──────────────────────────────────────
    val touchHeld = lastTouchEnd == null
    LaunchedEffect(episodeId, showAutoNext, touchHeld) {
        if (!showAutoNext || touchHeld) return@LaunchedEffect
        while (autoNextCountdown > 0) {
            delay(1_000)
            autoNextCountdown--
        }
        if (
            !autoNextDismissed && lastTouchEnd != null && player.playWhenReady &&
            player.playbackState == Player.STATE_ENDED && player.currentMediaItem?.episodeVideoId() == episodeId
        ) {
            changeEpisode(PlayerEvent.NextEpisode)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UI
    // ═══════════════════════════════════════════════════════════════

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black).pointerInput(player) {
            observePlayerTouches { held -> lastTouchEnd = if (held) null else SystemClock.uptimeMillis() }
        },
    ) {

        // Video surface
        if (state.currentVideoUrl != null || player.mediaItemCount > 0) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply { this.player = player; useController = false }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.player = player
                    view.resizeMode = if (zoomToFill && !isInPictureInPictureMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
            )
        }

        if (isInPictureInPictureMode) return@Box

        // ── Gesture layer ────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(player, episodeId, state.selectedQuality, canSeek) {
                    detectPlayerMotion(
                        onPinch = { zoomIn ->
                            seekAccum = 0L
                            if (zoomToFill != zoomIn) {
                                zoomToFill = zoomIn
                                zoomNotice = if (zoomIn) "На весь экран" else "Весь кадр"
                            }
                        },
                        onScrubStart = {
                            if (canSeek) {
                                seekAccum = 0L
                                seekStartMs = player.currentPosition
                                controlsVisible = false
                            }
                        },
                        onScrub = { fraction ->
                            if (canSeek) {
                                seekPreviewMs = (seekStartMs + (fraction * 120_000).toLong()).coerceIn(0L, duration)
                                showSeekPreview = true
                            }
                        },
                        onScrubEnd = { commit ->
                            if (commit && showSeekPreview) seekToPosition(seekPreviewMs)
                            showSeekPreview = false
                        },
                    )
                }
                .pointerInput(player, isSpeedLocked, lockedSpeed, episodeId, state.selectedQuality, canSeek) {
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
                                try {
                                    isSpeedBoosting = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (state.speedHintAvailable && episodeId != null && player.isPlaying &&
                                        player.currentMediaItem?.episodeVideoId() == episodeId
                                    ) {
                                        hintEpisodeId = episodeId
                                        player.pause()
                                        seekAccum = 0L
                                        viewModel.onEvent(PlayerEvent.SpeedHintShown)
                                    }
                                    tryAwaitRelease()
                                } finally {
                                    isSpeedBoosting = false
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
                            if (isSpeedBoosting || !canSeek) return@detectTapGestures
                            suppressNextTap = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (offset.x < size.width / 2) seekAccum -= 10_000L
                            else seekAccum += 10_000L
                        },
                    )
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
                    text = formatTime(seekPreviewMs) + "   " + formatSeekDelta(seekPreviewMs - seekStartMs),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        zoomNotice?.let { notice ->
            Text(
                text = notice,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        val loading = (state.isLoading || playbackState == Player.STATE_BUFFERING) && state.error == null
        if (loading && !controlsVisible && !showSeekPreview && !state.isCommentsPanelVisible && !showSpeedPopup) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center).size(46.dp),
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

        // ── Skip intro/outro button ─────────────────────────────
        run {
            val skip = activeSkip
            val isEd = skip?.type == com.animevost.app.core.domain.model.SkipType.ED
            val showAsNextEpisode = isEd && state.hasNext
            val visible = skip != null && !(isEd && !state.hasNext) && !showAutoNext && timelinePreviewMs == null
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
                                    changeEpisode(PlayerEvent.NextEpisode)
                                } else {
                                    seekToPosition(skip.endMs)
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
            visible = controlsVisible && !showSpeedHint,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize(),
        ) {
            PlayerControls(
                episodeName = state.currentEpisode?.name ?: "",
                videoSources = state.videoSources.map { it.quality },
                selectedQuality = state.selectedQuality,
                playRequested = playRequested,
                loading = loading && controlsVisible && !showSpeedPopup && !state.isCommentsPanelVisible,
                canSeek = canSeek,
                bufferedPosition = bufferedPosition,
                timelinePreview = timelinePreviewMs,
                previewSource = state.currentVideoUrl,
                currentPosition = currentPosition,
                duration = duration,
                hasPrevious = state.hasPrevious,
                hasNext = state.hasNext,
                onBack = closePlayer,
                onOpenComments = {
                    seekAccum = 0L
                    resumeAfterComments = player.playWhenReady && player.playbackState != Player.STATE_ENDED
                    player.pause()
                    controlsVisible = false
                    viewModel.onEvent(PlayerEvent.ShowEpisodeComments)
                },
                onPlayPause = {
                    if (player.playWhenReady && player.playbackState != Player.STATE_ENDED) {
                        player.pause()
                    } else {
                        if (player.playbackState == Player.STATE_ENDED) seekToPosition(0L)
                        player.play()
                    }
                },
                onPrevious = {
                    changeEpisode(PlayerEvent.PreviousEpisode)
                },
                onNext = {
                    changeEpisode(PlayerEvent.NextEpisode)
                },
                onSeekBack = { seekToPosition(player.currentPosition - 10_000L) },
                onSeekForward = { seekToPosition(player.currentPosition + 10_000L) },
                onSeek = ::seekToPosition,
                onTimelinePreview = { preview ->
                    if (preview != null && timelinePreviewMs == null) {
                        seekAccum = 0L
                    }
                    timelinePreviewMs = preview
                },
                onSelectQuality = { viewModel.onEvent(PlayerEvent.SelectQuality(it)) },
                qualityMenuOpen = qualityMenuOpen,
                onQualityMenuOpenChange = { open ->
                    if (open) seekAccum = 0L
                    qualityMenuOpen = open
                },
                skipIntervals = skipIntervals,
            )
        }

        // ── Auto-next card ───────────────────────────────────────
        AnimatedVisibility(
            visible = showAutoNext,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = if (controlsVisible) 90.dp else 24.dp),
        ) {
            AutoNextCard(
                countdown = autoNextCountdown,
                nextName = state.allEpisodes.getOrNull(state.currentEpisodeIndex + 1)?.name ?: "",
                onConfirm = { changeEpisode(PlayerEvent.NextEpisode) },
                onCancel = { autoNextDismissed = true },
            )
        }

        if (showSpeedHint) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.58f)).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Закрыть подсказку",
                    onClick = ::dismissSpeedHint,
                ),
            )
        }
        AnimatedVisibility(
            visible = showSpeedHint || isSpeedBoosting || (isSpeedLocked && controlsVisible),
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp),
        ) {
            SpeedBadge(
                speed = if (isSpeedBoosting || showSpeedHint) boostSpeed else baseSpeed,
                locked = isSpeedLocked,
                showHint = showSpeedHint,
                onClick = {
                    dismissSpeedHint()
                    seekAccum = 0L
                    showSpeedPopup = true
                },
            )
        }

        // ── Speed picker popup (on top of everything) ────────────
        if (showSpeedPopup) {
            SpeedPickerPopup(
                selectedSpeed = lockedSpeed,
                isLocked = isSpeedLocked,
                onSpeedSelect = { speed -> lockedSpeed = speed },
                onLockToggle = {
                    isSpeedLocked = !isSpeedLocked
                    showSpeedPopup = false
                },
                onDismiss = { showSpeedPopup = false },
            )
        }

        if (state.isCommentsPanelVisible) {
            Box(Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Закрыть комментарии",
                onClick = closeComments,
            ))
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
                onClose = closeComments,
                onLoadMore = {
                    viewModel.onEvent(PlayerEvent.LoadMoreEpisodeComments)
                },
                onRetry = {
                    viewModel.onEvent(PlayerEvent.RetryEpisodeComments)
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
