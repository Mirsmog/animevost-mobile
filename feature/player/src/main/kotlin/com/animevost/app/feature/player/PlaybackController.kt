package com.animevost.app.feature.player

import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import timber.log.Timber

@Composable
internal fun rememberPlaybackController(): MediaController? {
    val context = LocalContext.current.applicationContext
    var controller by remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(context) {
        var disposed = false
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java),
        )
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }
                    .onSuccess { if (!disposed) controller = it }
                    .onFailure { error ->
                        if (!disposed) Timber.e(error, "Unable to connect to playback service")
                    }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            disposed = true
            controller = null
            MediaController.releaseFuture(controllerFuture)
        }
    }

    return controller
}
