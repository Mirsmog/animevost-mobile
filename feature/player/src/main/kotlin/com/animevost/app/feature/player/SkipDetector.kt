package com.animevost.app.feature.player

import androidx.media3.exoplayer.ExoPlayer
import com.animevost.app.core.domain.model.SegmentType
import com.animevost.app.core.domain.model.SkipSegment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Polls the player position every second and triggers a seek when the current
 * position falls inside a segment's detection window [startMs - windowMs, startMs + windowMs].
 * The skip target is always startMs + durationMs (end of the segment).
 */
class SkipDetector(
    private val exoPlayer: ExoPlayer,
    var onSegmentDetected: ((SegmentType, Long) -> Unit)? = null,
) {
    companion object {
        private const val POLL_INTERVAL_MS = 1000L
    }

    private var job: Job? = null
    private val skippedTypes = mutableSetOf<SegmentType>()

    fun start(segments: List<SkipSegment>, scope: CoroutineScope) {
        if (segments.isEmpty()) return
        stop()
        skippedTypes.clear()
        job = scope.launch {
            while (isActive) {
                if (skippedTypes.size >= segments.size) break
                val pos = exoPlayer.currentPosition
                for (segment in segments) {
                    if (segment.type in skippedTypes) continue
                    val lo = segment.startMs - segment.windowMs
                    val hi = segment.startMs + segment.windowMs
                    if (pos in lo..hi) {
                        skippedTypes.add(segment.type)
                        onSegmentDetected?.invoke(segment.type, segment.startMs + segment.durationMs)
                        break
                    }
                }
                delay(POLL_INTERVAL_MS)
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
}
