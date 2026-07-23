package com.animevost.app.core.data.skip

import com.animevost.app.core.domain.model.SkipInterval
import com.animevost.app.core.domain.model.SkipType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayDeque
import java.util.Arrays
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal object FingerprintConfig {
    const val ALGORITHM_VERSION = 1
    const val TARGET_SAMPLE_RATE = 11_025
    const val WINDOW_SIZE = 2_048
    const val HOP_SIZE = 1_024
    const val OPENING_SCAN_MS = 7L * 60L * 1_000L
    const val ENDING_SCAN_MS = 7L * 60L * 1_000L

    fun frameToMs(frame: Int): Long =
        frame.toLong() * HOP_SIZE * 1_000L / TARGET_SAMPLE_RATE

    fun msToFrame(positionMs: Long): Int =
        (positionMs * TARGET_SAMPLE_RATE / (HOP_SIZE * 1_000L))
            .coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
            .toInt()
}

internal data class FingerprintResult(
    val landmarks: LongArray,
    val durationMs: Long,
)

internal data class FingerprintReference(
    val id: Int,
    val type: SkipType,
    val durationMs: Long,
    val landmarks: LongArray,
)

internal fun encodeLandmarks(landmarks: LongArray): ByteArray {
    val buffer = ByteBuffer.allocate(landmarks.size * Long.SIZE_BYTES)
        .order(ByteOrder.LITTLE_ENDIAN)
    landmarks.forEach(buffer::putLong)
    return buffer.array()
}

internal fun decodeLandmarks(payload: ByteArray): LongArray {
    if (payload.size % Long.SIZE_BYTES != 0) return LongArray(0)
    val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
    return LongArray(payload.size / Long.SIZE_BYTES) { buffer.long }
}

internal class StreamingAudioFingerprinter(
    private val sampleRateHz: Int,
    private val channelCount: Int,
    private val encoding: Int,
    private val absoluteStartFrame: Int = 0,
    private val onLandmarks: (LongArray) -> Unit,
) {
    private val pcmWindow = FloatArray(FingerprintConfig.WINDOW_SIZE)
    private val fftReal = FloatArray(FingerprintConfig.WINDOW_SIZE)
    private val fftImaginary = FloatArray(FingerprintConfig.WINDOW_SIZE)
    private val magnitudes = FloatArray(FingerprintConfig.WINDOW_SIZE / 2)
    private val peakFrames = ArrayDeque<PeakFrame>()

    private var bufferedSamples = 0
    private var outputSamples = 0L
    private var sourcePhase = 0
    private var sourceAccumulator = 0f
    private var sourceAccumulatorCount = 0
    private var fingerprintFrame = 0
    private var remainder = ByteArray(0)

    fun consume(bytes: ByteArray) {
        if (sampleRateHz <= 0 || channelCount <= 0 || bytes.isEmpty()) return
        val bytesPerSample = bytesPerSample(encoding) ?: return
        val frameSize = bytesPerSample * channelCount
        val input = if (remainder.isEmpty()) {
            bytes
        } else {
            ByteArray(remainder.size + bytes.size).also {
                remainder.copyInto(it)
                bytes.copyInto(it, remainder.size)
            }
        }
        val completeSize = input.size - input.size % frameSize
        if (completeSize < input.size) {
            remainder = input.copyOfRange(completeSize, input.size)
        } else {
            remainder = ByteArray(0)
        }

        var offset = 0
        while (offset < completeSize) {
            val channelsToMix = min(channelCount, 2)
            var mono = 0f
            for (channel in 0 until channelsToMix) {
                mono += readSample(input, offset + channel * bytesPerSample, encoding)
            }
            mono /= channelsToMix
            resample(mono)
            offset += frameSize
        }
    }

    fun finish(): FingerprintResult {
        if (sourceAccumulatorCount > 0) {
            appendResampled(sourceAccumulator / sourceAccumulatorCount)
            sourceAccumulator = 0f
            sourceAccumulatorCount = 0
        }
        return FingerprintResult(
            landmarks = LongArray(0),
            durationMs = outputSamples * 1_000L / FingerprintConfig.TARGET_SAMPLE_RATE,
        )
    }

    private fun resample(sample: Float) {
        sourceAccumulator += sample
        sourceAccumulatorCount++
        sourcePhase += FingerprintConfig.TARGET_SAMPLE_RATE
        if (sourcePhase >= sampleRateHz) {
            appendResampled(sourceAccumulator / sourceAccumulatorCount)
            sourcePhase -= sampleRateHz
            sourceAccumulator = 0f
            sourceAccumulatorCount = 0
        }
    }

    private fun appendResampled(sample: Float) {
        pcmWindow[bufferedSamples++] = sample
        outputSamples++
        if (bufferedSamples < FingerprintConfig.WINDOW_SIZE) return
        processWindow()
        System.arraycopy(
            pcmWindow,
            FingerprintConfig.HOP_SIZE,
            pcmWindow,
            0,
            FingerprintConfig.WINDOW_SIZE - FingerprintConfig.HOP_SIZE,
        )
        bufferedSamples = FingerprintConfig.WINDOW_SIZE - FingerprintConfig.HOP_SIZE
    }

    private fun processWindow() {
        var energy = 0f
        for (index in pcmWindow.indices) {
            val sample = pcmWindow[index]
            energy += sample * sample
            fftReal[index] = sample * HANN_WINDOW[index]
            fftImaginary[index] = 0f
        }
        if (energy / pcmWindow.size < MIN_WINDOW_ENERGY) {
            peakFrames.addLast(PeakFrame(fingerprintFrame, IntArray(0), FloatArray(0), IntArray(0)))
        } else {
            fft(fftReal, fftImaginary)
            for (index in magnitudes.indices) {
                val real = fftReal[index]
                val imaginary = fftImaginary[index]
                magnitudes[index] = real * real + imaginary * imaginary
            }
            peakFrames.addLast(findPeaks(fingerprintFrame))
        }
        fingerprintFrame++
        if (peakFrames.size > MAX_TARGET_DELTA) {
            emitAnchorLandmarks()
            peakFrames.removeFirst()
        }
    }

    private fun findPeaks(frame: Int): PeakFrame {
        val bins = IntArray(FREQUENCY_BANDS.size - 1)
        val strengths = FloatArray(bins.size)
        for (band in bins.indices) {
            val start = FREQUENCY_BANDS[band]
            val end = min(FREQUENCY_BANDS[band + 1], magnitudes.lastIndex)
            var bestBin = start
            var bestStrength = 0f
            for (bin in start..end) {
                val value = magnitudes[bin]
                if (value > bestStrength) {
                    bestStrength = value
                    bestBin = bin
                }
            }
            bins[band] = bestBin
            strengths[band] = bestStrength
        }
        val topTargets = bins.indices
            .sortedByDescending { strengths[it] }
            .take(TARGET_PEAKS_PER_FRAME)
            .toIntArray()
        return PeakFrame(frame, bins, strengths, topTargets)
    }

    private fun emitAnchorLandmarks() {
        val anchor = peakFrames.first()
        if (anchor.bins.isEmpty()) return
        val output = LongArray(anchor.bins.size * TARGET_DELTAS.size * TARGET_PEAKS_PER_FRAME)
        var outputIndex = 0
        TARGET_DELTAS.forEachIndexed { deltaIndex, delta ->
            val target = peakFrames.elementAt(delta)
            for (anchorBin in anchor.bins) {
                for (targetIndex in target.topTargets) {
                    val hash = makeHash(anchorBin, target.bins[targetIndex], deltaIndex)
                    output[outputIndex++] = packLandmark(
                        hash = hash,
                        frame = absoluteStartFrame + anchor.frame,
                    )
                }
            }
        }
        if (outputIndex > 0) {
            onLandmarks(if (outputIndex == output.size) output else output.copyOf(outputIndex))
        }
    }

    private fun makeHash(anchorBin: Int, targetBin: Int, deltaIndex: Int): Int {
        val anchorFrequency = (anchorBin / FREQUENCY_QUANTIZATION).coerceIn(0, 511)
        val targetFrequency = (targetBin / FREQUENCY_QUANTIZATION).coerceIn(0, 511)
        return (anchorFrequency shl 11) or (targetFrequency shl 2) or deltaIndex
    }

    private fun fft(real: FloatArray, imaginary: FloatArray) {
        val size = real.size
        var swapIndex = 0
        for (index in 1 until size) {
            var bit = size shr 1
            while (swapIndex and bit != 0) {
                swapIndex = swapIndex xor bit
                bit = bit shr 1
            }
            swapIndex = swapIndex xor bit
            if (index < swapIndex) {
                val realValue = real[index]
                real[index] = real[swapIndex]
                real[swapIndex] = realValue
                val imaginaryValue = imaginary[index]
                imaginary[index] = imaginary[swapIndex]
                imaginary[swapIndex] = imaginaryValue
            }
        }

        var length = 2
        while (length <= size) {
            val angle = (-2.0 * PI / length)
            val lengthReal = cos(angle).toFloat()
            val lengthImaginary = sin(angle).toFloat()
            var blockStart = 0
            while (blockStart < size) {
                var rotationReal = 1f
                var rotationImaginary = 0f
                for (offset in 0 until length / 2) {
                    val even = blockStart + offset
                    val odd = even + length / 2
                    val oddReal =
                        real[odd] * rotationReal - imaginary[odd] * rotationImaginary
                    val oddImaginary =
                        real[odd] * rotationImaginary + imaginary[odd] * rotationReal
                    val evenReal = real[even]
                    val evenImaginary = imaginary[even]
                    real[even] = evenReal + oddReal
                    imaginary[even] = evenImaginary + oddImaginary
                    real[odd] = evenReal - oddReal
                    imaginary[odd] = evenImaginary - oddImaginary
                    val nextReal =
                        rotationReal * lengthReal - rotationImaginary * lengthImaginary
                    rotationImaginary =
                        rotationReal * lengthImaginary + rotationImaginary * lengthReal
                    rotationReal = nextReal
                }
                blockStart += length
            }
            length = length shl 1
        }
    }

    private data class PeakFrame(
        val frame: Int,
        val bins: IntArray,
        val strengths: FloatArray,
        val topTargets: IntArray,
    )

    companion object {
        const val MIN_WINDOW_ENERGY = 0.0000005f
        const val FREQUENCY_QUANTIZATION = 3
        const val TARGET_PEAKS_PER_FRAME = 2
        const val MAX_TARGET_DELTA = 15

        val TARGET_DELTAS = intArrayOf(3, 7, 11, 15)
        val FREQUENCY_BANDS = intArrayOf(12, 24, 48, 96, 192, 320, 480, 680, 900)
        val HANN_WINDOW = FloatArray(FingerprintConfig.WINDOW_SIZE) { index ->
            (0.5 - 0.5 * cos(2.0 * PI * index / (FingerprintConfig.WINDOW_SIZE - 1))).toFloat()
        }

        fun bytesPerSample(encoding: Int): Int? = when (encoding) {
            ENCODING_PCM_8BIT -> 1
            ENCODING_PCM_16BIT -> 2
            ENCODING_PCM_24BIT -> 3
            ENCODING_PCM_32BIT, ENCODING_PCM_FLOAT -> 4
            else -> null
        }

        fun readSample(bytes: ByteArray, offset: Int, encoding: Int): Float = when (encoding) {
            ENCODING_PCM_8BIT -> ((bytes[offset].toInt() and 0xFF) - 128) / 128f
            ENCODING_PCM_16BIT -> {
                val value =
                    (bytes[offset].toInt() and 0xFF) or (bytes[offset + 1].toInt() shl 8)
                value.toShort().toInt() / 32_768f
            }
            ENCODING_PCM_24BIT -> {
                val value = (bytes[offset].toInt() and 0xFF) or
                    ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                    ((bytes[offset + 2].toInt() and 0xFF) shl 16)
                ((value shl 8) shr 8) / 8_388_608f
            }
            ENCODING_PCM_32BIT -> {
                val value = (bytes[offset].toInt() and 0xFF) or
                    ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                    ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                    (bytes[offset + 3].toInt() shl 24)
                (value / 2_147_483_648.0).toFloat()
            }
            ENCODING_PCM_FLOAT -> {
                val bits = (bytes[offset].toInt() and 0xFF) or
                    ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                    ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                    (bytes[offset + 3].toInt() shl 24)
                Float.fromBits(bits).takeIf(Float::isFinite)?.coerceIn(-1f, 1f) ?: 0f
            }
            else -> 0f
        }

        const val ENCODING_PCM_16BIT = 2
        const val ENCODING_PCM_8BIT = 3
        const val ENCODING_PCM_FLOAT = 4
        const val ENCODING_PCM_24BIT = 0x20000000
        const val ENCODING_PCM_32BIT = 0x30000000
    }
}

internal class FingerprintCollector {
    private var values = LongArray(4_096)
    private var size = 0

    fun add(batch: LongArray) {
        ensureCapacity(size + batch.size)
        batch.copyInto(values, size)
        size += batch.size
    }

    fun toArray(): LongArray = values.copyOf(size)

    private fun ensureCapacity(required: Int) {
        if (required <= values.size) return
        var newSize = values.size
        while (newSize < required) newSize *= 2
        values = values.copyOf(newSize)
    }
}

internal class FingerprintMatcher(
    references: List<FingerprintReference>,
    alreadyResolved: Set<SkipType>,
    private val onMatch: (SkipInterval) -> Unit,
) {
    private val resolvedTypes = alreadyResolved.toMutableSet()
    private val matchers = references.map(::ReferenceMatcher)
    private val recentLandmarks = LongRingBuffer(RECENT_LANDMARK_CAPACITY)
    private var durationMs = 0L
    private var landmarksSinceEvaluation = 0
    private var histogramStartFrame: Int? = null

    fun updateDuration(durationMs: Long) {
        if (durationMs > 0L) this.durationMs = durationMs
    }

    fun consume(batch: LongArray) {
        if (batch.isEmpty()) return
        val latestFrame = landmarkFrame(batch.last())
        val histogramStart = histogramStartFrame
        if (histogramStart == null) {
            histogramStartFrame = latestFrame
        } else if (latestFrame - histogramStart > HISTOGRAM_WINDOW_FRAMES) {
            matchers.forEach(ReferenceMatcher::reset)
            recentLandmarks.clear()
            histogramStartFrame = latestFrame
        }

        for (landmark in batch) {
            recentLandmarks.add(landmark)
            val typeCandidates = matchers.filter { it.reference.type !in resolvedTypes }
            typeCandidates.forEach { it.count(landmark) }
        }
        landmarksSinceEvaluation += batch.size
        if (landmarksSinceEvaluation < EVALUATE_EVERY_LANDMARKS) return
        landmarksSinceEvaluation = 0

        val candidates = matchers
            .asSequence()
            .filter { it.reference.type !in resolvedTypes }
            .mapNotNull { matcher -> matcher.candidate(recentLandmarks, durationMs) }
            .sortedByDescending { it.confidence }
            .toList()
        for (candidate in candidates) {
            if (candidate.interval.type in resolvedTypes) continue
            resolvedTypes += candidate.interval.type
            onMatch(candidate.interval)
        }
    }

    private class ReferenceMatcher(
        val reference: FingerprintReference,
    ) {
        private val sortedLandmarks = reference.landmarks.copyOf().also { Arrays.sort(it) }
        private val histogram = OffsetHistogram()

        fun count(queryLandmark: Long) {
            val hash = landmarkHash(queryLandmark)
            val queryFrame = landmarkFrame(queryLandmark)
            val range = hashRange(sortedLandmarks, hash) ?: return
            if (range.last - range.first + 1 > MAX_HASH_OCCURRENCES) return
            for (index in range) {
                val offset = queryFrame - landmarkFrame(sortedLandmarks[index])
                histogram.increment(Math.floorDiv(offset, OFFSET_BUCKET_FRAMES))
            }
        }

        fun candidate(recent: LongRingBuffer, durationMs: Long): MatchCandidate? {
            val peak = histogram.peak() ?: return null
            if (peak.count < MIN_HISTOGRAM_VOTES) return null
            if (peak.secondCount > 0 && peak.count < peak.secondCount * MIN_PEAK_RATIO) return null

            var matchedFrames = 0
            var lastMatchedFrame = Int.MIN_VALUE
            var firstMatchedFrame = Int.MAX_VALUE
            var finalMatchedFrame = Int.MIN_VALUE
            var verifiedVotes = 0
            recent.forEach { queryLandmark ->
                val queryHash = landmarkHash(queryLandmark)
                val queryFrame = landmarkFrame(queryLandmark)
                val range = hashRange(sortedLandmarks, queryHash) ?: return@forEach
                if (range.last - range.first + 1 > MAX_HASH_OCCURRENCES) return@forEach
                var matchesOffset = false
                for (index in range) {
                    val offset = queryFrame - landmarkFrame(sortedLandmarks[index])
                    if (Math.floorDiv(offset, OFFSET_BUCKET_FRAMES) == peak.key) {
                        verifiedVotes++
                        matchesOffset = true
                        break
                    }
                }
                if (matchesOffset && queryFrame != lastMatchedFrame) {
                    matchedFrames++
                    lastMatchedFrame = queryFrame
                    firstMatchedFrame = min(firstMatchedFrame, queryFrame)
                    finalMatchedFrame = max(finalMatchedFrame, queryFrame)
                }
            }
            if (verifiedVotes < MIN_VERIFIED_VOTES || matchedFrames < MIN_MATCHED_FRAMES) return null
            if (finalMatchedFrame - firstMatchedFrame < MIN_MATCH_SPAN_FRAMES) return null

            val startFrame = peak.key * OFFSET_BUCKET_FRAMES
            var startMs = FingerprintConfig.frameToMs(startFrame)
            if (startMs in -BOUNDARY_TOLERANCE_MS until 0L) startMs = 0L
            if (!isPlausible(reference.type, startMs, durationMs)) return null
            val unclampedEnd = startMs + reference.durationMs
            val endMs = if (durationMs > 0L) min(unclampedEnd, durationMs) else unclampedEnd
            if (endMs - startMs < MIN_INTERVAL_MS) return null
            val confidence = verifiedVotes.toFloat() / max(1, peak.secondCount)
            return MatchCandidate(
                interval = SkipInterval(reference.type, startMs, endMs),
                confidence = confidence,
            )
        }

        fun reset() {
            histogram.clear()
        }

        private fun isPlausible(type: SkipType, startMs: Long, durationMs: Long): Boolean =
            when (type) {
                SkipType.OP -> startMs in 0L..FingerprintConfig.OPENING_SCAN_MS
                SkipType.ED -> {
                    durationMs > 0L &&
                        startMs >= max(0L, durationMs - FingerprintConfig.ENDING_SCAN_MS) &&
                        startMs < durationMs
                }
            }
    }

    private data class MatchCandidate(
        val interval: SkipInterval,
        val confidence: Float,
    )

    private class OffsetHistogram(
        private val capacity: Int = 16_384,
    ) {
        private val keys = IntArray(capacity) { EMPTY_KEY }
        private val counts = IntArray(capacity)
        private val usedSlots = IntArray(capacity)
        private var usedCount = 0

        fun increment(key: Int) {
            if (key == EMPTY_KEY) return
            var slot = mix(key) and (capacity - 1)
            while (true) {
                when (keys[slot]) {
                    EMPTY_KEY -> {
                        if (usedCount == capacity) return
                        keys[slot] = key
                        counts[slot] = 1
                        usedSlots[usedCount++] = slot
                        return
                    }
                    key -> {
                        counts[slot]++
                        return
                    }
                    else -> slot = (slot + 1) and (capacity - 1)
                }
            }
        }

        fun peak(): HistogramPeak? {
            if (usedCount == 0) return null
            var bestKey = 0
            var bestCount = 0
            var secondCount = 0
            for (index in 0 until usedCount) {
                val slot = usedSlots[index]
                val count = counts[slot]
                if (count > bestCount) {
                    secondCount = bestCount
                    bestCount = count
                    bestKey = keys[slot]
                } else if (count > secondCount) {
                    secondCount = count
                }
            }
            return HistogramPeak(bestKey, bestCount, secondCount)
        }

        fun clear() {
            for (index in 0 until usedCount) {
                val slot = usedSlots[index]
                keys[slot] = EMPTY_KEY
                counts[slot] = 0
            }
            usedCount = 0
        }

        private fun mix(value: Int): Int {
            var result = value
            result = result xor (result ushr 16)
            result *= -0x7a143595
            result = result xor (result ushr 13)
            return result
        }

        data class HistogramPeak(
            val key: Int,
            val count: Int,
            val secondCount: Int,
        )

        private companion object {
            const val EMPTY_KEY = Int.MIN_VALUE
        }
    }

    private class LongRingBuffer(
        private val capacity: Int,
    ) {
        private val values = LongArray(capacity)
        private var start = 0
        private var size = 0

        fun add(value: Long) {
            if (size < capacity) {
                values[(start + size) % capacity] = value
                size++
            } else {
                values[start] = value
                start = (start + 1) % capacity
            }
        }

        fun clear() {
            start = 0
            size = 0
        }

        inline fun forEach(block: (Long) -> Unit) {
            for (index in 0 until size) {
                block(values[(start + index) % capacity])
            }
        }
    }

    private companion object {
        const val OFFSET_BUCKET_FRAMES = 2
        const val MAX_HASH_OCCURRENCES = 16
        const val MIN_HISTOGRAM_VOTES = 28
        const val MIN_VERIFIED_VOTES = 24
        const val MIN_MATCHED_FRAMES = 8
        const val MIN_MATCH_SPAN_FRAMES = 32
        const val MIN_PEAK_RATIO = 1.35f
        const val MIN_INTERVAL_MS = 20_000L
        const val BOUNDARY_TOLERANCE_MS = 1_000L
        const val EVALUATE_EVERY_LANDMARKS = 256
        const val RECENT_LANDMARK_CAPACITY = 20_000
        val HISTOGRAM_WINDOW_FRAMES = FingerprintConfig.msToFrame(45_000L)
    }
}

private fun packLandmark(hash: Int, frame: Int): Long =
    (hash.toLong() shl 32) or (frame.toLong() and 0xFFFF_FFFFL)

private fun landmarkHash(landmark: Long): Int = (landmark ushr 32).toInt()

private fun landmarkFrame(landmark: Long): Int = landmark.toInt()

private fun hashRange(sortedLandmarks: LongArray, hash: Int): IntRange? {
    var low = 0
    var high = sortedLandmarks.size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (landmarkHash(sortedLandmarks[mid]) < hash) low = mid + 1 else high = mid
    }
    val first = low
    if (first >= sortedLandmarks.size || landmarkHash(sortedLandmarks[first]) != hash) return null
    low = first
    high = sortedLandmarks.size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (landmarkHash(sortedLandmarks[mid]) <= hash) low = mid + 1 else high = mid
    }
    return first until low
}
