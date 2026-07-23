package com.animevost.app.core.data.skip

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.animevost.app.core.network.animethemes.AnimeThemeReference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

class ReferenceAudioDecoder @Inject constructor(
    @ApplicationContext context: Context,
    @Named("animethemes") private val client: OkHttpClient,
) {
    private val cacheDirectory = File(context.cacheDir, "skip-theme-audio")

    internal suspend fun fingerprint(reference: AnimeThemeReference): FingerprintResult =
        withContext(Dispatchers.IO) {
            cacheDirectory.mkdirs()
            removeStaleFiles()
            val audioFile = File(cacheDirectory, "${reference.id}.audio")
            try {
                download(reference, audioFile)
                decode(audioFile)
            } finally {
                audioFile.delete()
                File(cacheDirectory, "${reference.id}.part").delete()
            }
        }

    private fun download(reference: AnimeThemeReference, destination: File) {
        val partial = File(cacheDirectory, "${reference.id}.part")
        partial.delete()
        destination.delete()
        val request = Request.Builder()
            .url(reference.audioUrl)
            .header("Accept", "audio/*")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("AnimeThemes audio HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("AnimeThemes audio body is empty")
            val declaredLength = body.contentLength()
            if (declaredLength > MAX_AUDIO_BYTES) {
                throw IOException("AnimeThemes audio is too large: $declaredLength")
            }
            body.byteStream().use { input ->
                partial.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_AUDIO_BYTES) {
                            throw IOException("AnimeThemes audio exceeded size limit")
                        }
                        output.write(buffer, 0, read)
                    }
                }
            }
        }
        if (!partial.renameTo(destination)) {
            throw IOException("Could not finalize AnimeThemes audio cache")
        }
    }

    private fun decode(file: File): FingerprintResult {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(file.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index)
                    .getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            } ?: throw IOException("AnimeThemes file has no audio track")
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IOException("AnimeThemes audio MIME is missing")
            extractor.selectTrack(trackIndex)

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val collector = FingerprintCollector()
            var fingerprinter: StreamingAudioFingerprinter? = null
            var inputEnded = false
            var outputEnded = false
            var outputFormat: MediaFormat? = null
            val bufferInfo = MediaCodec.BufferInfo()

            while (!outputEnded) {
                if (!inputEnded) {
                    val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                            ?: throw IOException("Audio decoder input buffer is missing")
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputEnded = true
                        } else {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime.coerceAtLeast(0L),
                                extractor.sampleFlags,
                            )
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        outputFormat = codec.outputFormat
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (outputIndex >= 0) {
                        val format = outputFormat ?: codec.outputFormat.also { outputFormat = it }
                        val processor = fingerprinter ?: StreamingAudioFingerprinter(
                                sampleRateHz = format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                                encoding = format.getIntegerOrDefault(
                                    MediaFormat.KEY_PCM_ENCODING,
                                    StreamingAudioFingerprinter.ENCODING_PCM_16BIT,
                                ),
                                onLandmarks = collector::add,
                            ).also { fingerprinter = it }
                        if (bufferInfo.size > 0) {
                            val outputBuffer = codec.getOutputBuffer(outputIndex)
                                ?: throw IOException("Audio decoder output buffer is missing")
                            val copy = ByteArray(bufferInfo.size)
                            outputBuffer.duplicate().apply {
                                position(bufferInfo.offset)
                                limit(bufferInfo.offset + bufferInfo.size)
                                get(copy)
                            }
                            processor.consume(copy)
                        }
                        outputEnded =
                            bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0 ||
                            bufferInfo.presentationTimeUs >= MAX_REFERENCE_DURATION_US
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }

            val summary = fingerprinter?.finish()
                ?: throw IOException("AnimeThemes audio produced no PCM")
            val landmarks = collector.toArray()
            if (landmarks.isEmpty() || summary.durationMs < MIN_REFERENCE_DURATION_MS) {
                throw IOException("AnimeThemes audio fingerprint is empty")
            }
            return summary.copy(landmarks = landmarks)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    private fun removeStaleFiles() {
        val cutoff = System.currentTimeMillis() - STALE_FILE_MS
        cacheDirectory.listFiles()
            ?.filter { it.lastModified() < cutoff }
            ?.forEach(File::delete)
    }

    private fun MediaFormat.getIntegerOrDefault(key: String, defaultValue: Int): Int =
        if (containsKey(key)) getInteger(key) else defaultValue

    private companion object {
        const val DOWNLOAD_BUFFER_SIZE = 32 * 1_024
        const val MAX_AUDIO_BYTES = 8L * 1_024L * 1_024L
        const val CODEC_TIMEOUT_US = 10_000L
        const val MAX_REFERENCE_DURATION_US = 210L * 1_000_000L
        const val MIN_REFERENCE_DURATION_MS = 20_000L
        const val STALE_FILE_MS = 24L * 60L * 60L * 1_000L
    }
}
