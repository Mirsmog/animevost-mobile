package com.animevost.app.feature.player

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.abs
import kotlin.math.roundToInt

private const val FRAME_INTERVAL_MS = 2_000L
private const val FRAME_WIDTH = 256
private const val FRAME_HEIGHT = 144
private val frameDispatcher = Dispatchers.IO.limitedParallelism(1)

private data class TimelineFrame(val bucket: Long, val image: ImageBitmap)

@Composable
internal fun rememberTimelineFrame(url: String?, positionMs: Long?): ImageBitmap? {
    val bucket = positionMs?.div(FRAME_INTERVAL_MS)
    val requested by rememberUpdatedState(bucket)
    val cache = remember(url) { LruCache<Long, ImageBitmap>(16) }
    var frame by remember(url) { mutableStateOf<TimelineFrame?>(null) }
    LaunchedEffect(url, bucket != null) {
        if (bucket == null || url.isNullOrBlank()) return@LaunchedEffect
        // A quick tap needs only the seek, so open the preview stream on a hold or drag.
        delay(150)
        var retriever: MediaMetadataRetriever? = null
        try {
            snapshotFlow { requested }.filterNotNull().conflate().collect { target ->
                val image = cache.get(target) ?: withContext(frameDispatcher) {
                    val source = retriever ?: MediaMetadataRetriever().also {
                        retriever = it
                        it.setDataSource(url, emptyMap())
                    }
                    source.previewAt(target * FRAME_INTERVAL_MS * 1000)
                }?.asImageBitmap()?.also { cache.put(target, it) }
                if (image != null) {
                    frame = TimelineFrame(target, image)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            // The timestamp remains available if the source cannot supply a frame.
        } catch (_: RuntimeException) {
            // Platform extractors do not support every playable stream.
        } finally {
            withContext(NonCancellable + frameDispatcher) { runCatching { retriever?.release() } }
        }
    }
    if (bucket == null) return null
    val recent = frame
    return cache.get(bucket) ?: recent?.takeIf { abs(it.bucket - bucket) <= 1 }?.image
}

private fun MediaMetadataRetriever.previewAt(timeUs: Long): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        return getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, FRAME_WIDTH, FRAME_HEIGHT)
    }
    val full = getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
    val scale = minOf(FRAME_WIDTH.toFloat() / full.width, FRAME_HEIGHT.toFloat() / full.height)
    if (scale >= 1f) return full
    val thumbnail = Bitmap.createScaledBitmap(
        full, (full.width * scale).roundToInt().coerceAtLeast(1), (full.height * scale).roundToInt().coerceAtLeast(1), true,
    )
    if (full !== thumbnail) full.recycle()
    return thumbnail
}
