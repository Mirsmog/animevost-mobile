package com.animevost.app.feature.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.animevost.app.core.domain.repository.LocalSkipDetector
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import java.nio.ByteBuffer
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var localSkipDetector: LocalSkipDetector

    private val positionHandler = Handler(Looper.getMainLooper())
    private val positionUpdate = object : Runnable {
        override fun run() {
            if (::player.isInitialized) {
                localSkipDetector.updatePlaybackPosition(
                    positionMs = player.currentPosition,
                    durationMs = player.duration,
                )
            }
            positionHandler.postDelayed(this, POSITION_UPDATE_INTERVAL_MS)
        }
    }

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(PLAYBACK_CHANNEL_ID)
                .setChannelName(R.string.playback_channel_name)
                .build(),
        )

        val pcmAudioProcessor = TeeAudioProcessor(
            object : TeeAudioProcessor.AudioBufferSink {
                override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
                    localSkipDetector.onPcmFormat(sampleRateHz, channelCount, encoding)
                }

                override fun handleBuffer(buffer: ByteBuffer) {
                    localSkipDetector.onPcmBuffer(buffer)
                }
            },
        )
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf<AudioProcessor>(pcmAudioProcessor))
                .setEnableFloatOutput(false)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .build()
        }

        player = ExoPlayer.Builder(this, renderersFactory)
            .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true,
                )
                setHandleAudioBecomingNoisy(true)
            }

        val seekBackCommand = SessionCommand(SEEK_BACK_ACTION, Bundle.EMPTY)
        val seekForwardCommand = SessionCommand(SEEK_FORWARD_ACTION, Bundle.EMPTY)
        val seekBackButton = CommandButton.Builder(CommandButton.ICON_SKIP_BACK_10)
            .setSessionCommand(seekBackCommand)
            .setDisplayName("Назад на 10 секунд")
            .setSlots(CommandButton.SLOT_BACK)
            .build()
        val seekForwardButton = CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_10)
            .setSessionCommand(seekForwardCommand)
            .setDisplayName("Вперед на 10 секунд")
            .setSlots(CommandButton.SLOT_FORWARD)
            .build()
        val mediaButtons = listOf(seekBackButton, seekForwardButton)
        val controllerSessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
            .buildUpon()
            .add(seekBackCommand)
            .add(seekForwardCommand)
            .build()
        val controllerCommands = withoutPlaylistNavigation(
            MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS,
        )
        val sessionCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
            ): MediaSession.ConnectionResult {
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(controllerSessionCommands)
                    .setAvailablePlayerCommands(controllerCommands)
                    .setCustomLayout(mediaButtons)
                    .setMediaButtonPreferences(mediaButtons)
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle,
            ): ListenableFuture<SessionResult> {
                val result = when (customCommand.customAction) {
                    SEEK_BACK_ACTION -> {
                        session.player.seekBack()
                        SessionResult.RESULT_SUCCESS
                    }
                    SEEK_FORWARD_ACTION -> {
                        session.player.seekForward()
                        SessionResult.RESULT_SUCCESS
                    }
                    else -> SessionResult.RESULT_ERROR_NOT_SUPPORTED
                }
                return Futures.immediateFuture(SessionResult(result))
            }
        }
        val sessionPlayer = object : ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands =
                withoutPlaylistNavigation(super.getAvailableCommands())

            override fun isCommandAvailable(command: Int): Boolean =
                !isPlaylistNavigationCommand(command) && super.isCommandAvailable(command)
        }
        val sessionBuilder = MediaSession.Builder(this, sessionPlayer)
            .setCallback(sessionCallback)
            .setCustomLayout(mediaButtons)
            .setMediaButtonPreferences(mediaButtons)
        createSessionActivity()?.let(sessionBuilder::setSessionActivity)
        mediaSession = sessionBuilder.build()
        positionHandler.post(positionUpdate)
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        pauseAllPlayersAndStopSelf()
    }

    override fun onDestroy() {
        positionHandler.removeCallbacks(positionUpdate)
        localSkipDetector.stop()
        mediaSession?.release()
        mediaSession = null
        if (::player.isInitialized) player.release()
        super.onDestroy()
    }

    private fun createSessionActivity(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            this,
            SESSION_ACTIVITY_REQUEST_CODE,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun withoutPlaylistNavigation(commands: Player.Commands): Player.Commands =
        commands.buildUpon()
            .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
            .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .remove(Player.COMMAND_SEEK_TO_NEXT)
            .remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .build()

    private fun isPlaylistNavigationCommand(command: Int): Boolean = when (command) {
        Player.COMMAND_SEEK_TO_PREVIOUS,
        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        Player.COMMAND_SEEK_TO_NEXT,
        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
        -> true
        else -> false
    }

    companion object {
        private const val SEEK_INCREMENT_MS = 10_000L
        private const val POSITION_UPDATE_INTERVAL_MS = 500L
        private const val SESSION_ACTIVITY_REQUEST_CODE = 4301
        private const val PLAYBACK_CHANNEL_ID = "playback"
        private const val SEEK_BACK_ACTION = "com.animevost.app.playback.SEEK_BACK"
        private const val SEEK_FORWARD_ACTION = "com.animevost.app.playback.SEEK_FORWARD"

        fun stop(context: Context) {
            context.stopService(Intent(context, PlaybackService::class.java))
        }
    }
}
