package com.animevost.app.feature.player

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.media3.exoplayer.ExoPlayer
import com.animevost.app.core.domain.model.SegmentType
import com.animevost.app.core.domain.model.SkipSegment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SkipDetector(
    private val exoPlayer: ExoPlayer,
    private val surfaceView: SurfaceView,
    var onSegmentDetected: ((SegmentType, Long) -> Unit)? = null,
) {
    companion object {
        private const val HAMMING_THRESHOLD = 10
        private const val MAX_SCAN_MS = 15L * 60 * 1000
        private const val SCAN_INTERVAL_MS = 1000L
        private const val SCAN_START_MS = 10_000L
    }

    private var job: Job? = null
    private val skippedTypes = mutableSetOf<SegmentType>()

    fun start(segments: List<SkipSegment>, scope: CoroutineScope) {
        if (segments.isEmpty()) return
        stop()
        skippedTypes.clear()
        job = scope.launch {
            // Wait until 10s into playback
            while (isActive && exoPlayer.currentPosition < SCAN_START_MS) {
                delay(SCAN_INTERVAL_MS)
            }
            while (isActive && exoPlayer.currentPosition < MAX_SCAN_MS) {
                if (skippedTypes.size >= segments.size) break
                val frame = captureFrame()
                if (frame == null) {
                    delay(SCAN_INTERVAL_MS)
                    continue
                }
                val hash = PHashUtils.compute(frame)
                frame.recycle()
                for (segment in segments) {
                    if (segment.type in skippedTypes) continue
                    val matchOffset = matchSegment(hash, segment) ?: continue
                    val pos = exoPlayer.currentPosition
                    val seekTo = pos + (segment.durationMs - matchOffset)
                    skippedTypes.add(segment.type)
                    onSegmentDetected?.invoke(segment.type, seekTo)
                    break
                }
                delay(SCAN_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun resetSkipped() {
        skippedTypes.clear()
    }

    private fun matchSegment(hash: Long, segment: SkipSegment): Long? {
        if (PHashUtils.hammingDistance(hash, segment.hash0) <= HAMMING_THRESHOLD) return 0L
        if (PHashUtils.hammingDistance(hash, segment.hash3) <= HAMMING_THRESHOLD) return 3000L
        if (PHashUtils.hammingDistance(hash, segment.hash6) <= HAMMING_THRESHOLD) return 6000L
        return null
    }

    private suspend fun captureFrame(): Bitmap? {
        if (!surfaceView.isAttachedToWindow || surfaceView.width <= 0 || surfaceView.height <= 0) {
            return null
        }
        return suspendCancellableCoroutine { cont ->
            val bitmap = Bitmap.createBitmap(
                surfaceView.width,
                surfaceView.height,
                Bitmap.Config.ARGB_8888,
            )
            try {
                PixelCopy.request(surfaceView, bitmap, { result ->
                    if (result == PixelCopy.SUCCESS) {
                        cont.resume(bitmap)
                    } else {
                        bitmap.recycle()
                        cont.resume(null)
                    }
                }, Handler(Looper.getMainLooper()))
            } catch (_: Exception) {
                bitmap.recycle()
                cont.resume(null)
            }
        }
    }
}
