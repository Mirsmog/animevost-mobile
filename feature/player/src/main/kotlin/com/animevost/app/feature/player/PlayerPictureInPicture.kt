package com.animevost.app.feature.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational

object PlayerPictureInPicture {
    private val defaultAspectRatio = Rational(16, 9)

    @Volatile
    private var aspectRatio = defaultAspectRatio

    fun update(
        activity: Activity,
        playbackActive: Boolean,
        videoWidth: Int,
        videoHeight: Int,
    ) {
        aspectRatio = validAspectRatio(videoWidth, videoHeight)
        activity.setPictureInPictureParams(
            buildParams(autoEnterEnabled = playbackActive),
        )
    }

    fun clear(activity: Activity) {
        activity.setPictureInPictureParams(
            buildParams(autoEnterEnabled = false),
        )
    }

    fun onUserLeaveHint(activity: Activity, playbackActive: Boolean) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ||
            !playbackActive ||
            activity.isFinishing ||
            activity.isInPictureInPictureMode
        ) {
            return
        }
        activity.enterPictureInPictureMode(buildParams(autoEnterEnabled = false))
    }

    private fun buildParams(autoEnterEnabled: Boolean): PictureInPictureParams {
        return PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(autoEnterEnabled)
                    setSeamlessResizeEnabled(true)
                }
            }
            .build()
    }

    private fun validAspectRatio(width: Int, height: Int): Rational {
        if (width <= 0 || height <= 0) return defaultAspectRatio
        val ratio = width.toDouble() / height.toDouble()
        return if (ratio in MIN_ASPECT_RATIO..MAX_ASPECT_RATIO) {
            Rational(width, height)
        } else {
            defaultAspectRatio
        }
    }

    private const val MIN_ASPECT_RATIO = 1.0 / 2.39
    private const val MAX_ASPECT_RATIO = 2.39
}
