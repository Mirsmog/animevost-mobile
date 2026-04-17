package com.animevost.app.feature.player

import android.media.MediaMetadataRetriever
import androidx.media3.exoplayer.ExoPlayer
import com.animevost.app.core.domain.model.SegmentType
import com.animevost.app.core.domain.model.SkipSegment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Scans video frames via [MediaMetadataRetriever] to find the pHash-matching
 * position for each [SkipSegment], then polls [ExoPlayer.currentPosition] and
 * fires [onSegmentDetected] 1 s before the match point.
 *
 * Falls back to [SkipSegment.startMs] when no pHash match is found.
 */
class SkipDetector(
    private val exoPlayer: ExoPlayer,
    private val videoUrl: String,
    var onSegmentDetected: ((SegmentType, Long) -> Unit)? = null,
) {
    companion object {
        private const val SCAN_STEP_MS = 3000L
        private const val POLL_INTERVAL_MS = 500L
        private const val HAMMING_THRESHOLD = 10
        private const val TRIGGER_LEAD_MS = 1000L
    }

    private val jobs = mutableListOf<Job>()
    private val skippedTypes = mutableSetOf<SegmentType>()

    fun start(segments: List<SkipSegment>, scope: CoroutineScope) {
        if (segments.isEmpty()) return
        stop()
        skippedTypes.clear()
        for (segment in segments) {
            jobs += scope.launch {
                val matchPositionMs = scanForMatch(segment)
                pollAndTrigger(segment, matchPositionMs)
            }
        }
    }

    fun stop() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    fun resetSkipped() {
        stop()
        skippedTypes.clear()
    }

    private suspend fun scanForMatch(segment: SkipSegment): Long {
        val hasHashes = segment.hash0 != 0L || segment.hash3 != 0L || segment.hash6 != 0L
        if (!hasHashes) return segment.startMs

        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(videoUrl, emptyMap())
                val scanStart = maxOf(0L, segment.startMs - segment.windowMs)
                val scanEnd = segment.startMs + segment.windowMs
                var t = scanStart
                while (t <= scanEnd) {
                    val bitmap = retriever.getFrameAtTime(
                        t * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    )
                    if (bitmap != null) {
                        val hash = PHashUtils.compute(bitmap)
                        bitmap.recycle()

                        if (segment.hash0 != 0L &&
                            PHashUtils.hammingDistance(hash, segment.hash0) <= HAMMING_THRESHOLD
                        ) {
                            return@withContext maxOf(0L, t)
                        }
                        if (segment.hash3 != 0L &&
                            PHashUtils.hammingDistance(hash, segment.hash3) <= HAMMING_THRESHOLD
                        ) {
                            return@withContext maxOf(0L, t - 3000L)
                        }
                        if (segment.hash6 != 0L &&
                            PHashUtils.hammingDistance(hash, segment.hash6) <= HAMMING_THRESHOLD
                        ) {
                            return@withContext maxOf(0L, t - 6000L)
                        }
                    }
                    t += SCAN_STEP_MS
                }
                segment.startMs
            } catch (_: Exception) {
                segment.startMs
            } finally {
                retriever.release()
            }
        }
    }

    private suspend fun pollAndTrigger(segment: SkipSegment, matchPositionMs: Long) {
        val triggerAt = matchPositionMs - TRIGGER_LEAD_MS
        while (true) {
            if (segment.type in skippedTypes) return
            val pos = exoPlayer.currentPosition
            if (pos >= triggerAt) {
                skippedTypes.add(segment.type)
                onSegmentDetected?.invoke(segment.type, matchPositionMs + segment.durationMs)
                return
            }
            delay(POLL_INTERVAL_MS)
        }
    }
}
