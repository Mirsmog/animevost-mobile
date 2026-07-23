package com.animevost.app.core.data.skip

import com.animevost.app.core.data.db.SkipTimeDao
import com.animevost.app.core.data.db.SkipTimeEntity
import com.animevost.app.core.data.db.ThemeFingerprintDao
import com.animevost.app.core.data.db.ThemeFingerprintEntity
import com.animevost.app.core.data.db.ThemeLookupDao
import com.animevost.app.core.data.db.ThemeLookupEntity
import com.animevost.app.core.domain.model.SkipInterval
import com.animevost.app.core.domain.model.SkipType
import com.animevost.app.core.domain.repository.LocalSkipDetector
import com.animevost.app.core.domain.repository.LocalSkipEvent
import com.animevost.app.core.domain.repository.LocalSkipPreparation
import com.animevost.app.core.domain.repository.LocalSkipRequest
import com.animevost.app.core.network.animethemes.AnimeThemeReference
import com.animevost.app.core.network.animethemes.AnimeThemesClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSkipDetectorImpl @Inject constructor(
    private val animeThemesClient: AnimeThemesClient,
    private val referenceAudioDecoder: ReferenceAudioDecoder,
    private val fingerprintDao: ThemeFingerprintDao,
    private val lookupDao: ThemeLookupDao,
    private val skipTimeDao: SkipTimeDao,
) : LocalSkipDetector {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableEvents = MutableSharedFlow<LocalSkipEvent>(extraBufferCapacity = 8)
    override val events: Flow<LocalSkipEvent> = mutableEvents.asSharedFlow()

    @Volatile
    private var activeSession: DetectionSession? = null

    override suspend fun prepare(request: LocalSkipRequest): LocalSkipPreparation {
        stop()
        val cachedIntervals = withContext(Dispatchers.IO) {
            skipTimeDao.getBySource(
                request.animeId,
                request.episodeNumber,
                SkipTimeEntity.SOURCE_LOCAL,
            ).toSkipIntervals()
        }
        val session = DetectionSession(request, cachedIntervals.mapTo(mutableSetOf()) { it.type })
        activeSession = session
        session.worker = scope.launch { processPcm(session) }

        return try {
            val references = loadReferences(request, session.resolvedTypes)
            if (activeSession !== session) {
                LocalSkipPreparation(cachedIntervals, cachedIntervals.mapTo(mutableSetOf()) { it.type })
            } else {
                session.installMatcher(references) { interval ->
                    onIntervalFound(session, interval)
                }
                val detectable = cachedIntervals.mapTo(mutableSetOf()) { it.type }.apply {
                    addAll(references.map { it.type })
                }
                session.detectableTypes.set(detectable)
                Timber.d(
                    "Local skip ready: animeId=%d ep=%d refs=%d cached=%d",
                    request.animeId,
                    request.episodeNumber,
                    references.size,
                    cachedIntervals.size,
                )
                LocalSkipPreparation(cachedIntervals, detectable)
            }
        } catch (error: CancellationException) {
            stopSession(session)
            throw error
        } catch (error: Exception) {
            Timber.w(
                error,
                "Local skip setup failed: animeId=%d ep=%d",
                request.animeId,
                request.episodeNumber,
            )
            session.detectableTypes.set(cachedIntervals.mapTo(mutableSetOf()) { it.type })
            LocalSkipPreparation(
                cachedIntervals = cachedIntervals,
                detectableTypes = cachedIntervals.mapTo(mutableSetOf()) { it.type },
            )
        }
    }

    override fun updatePlaybackPosition(positionMs: Long, durationMs: Long) {
        activeSession?.let { session ->
            session.positionMs.set(positionMs.coerceAtLeast(0L))
            if (durationMs > 0L) {
                session.durationMs.set(durationMs)
                session.withMatcher { it.updateDuration(durationMs) }
            }
        }
    }

    override fun onPcmFormat(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        val session = activeSession ?: return
        if (sampleRateHz <= 0 || channelCount <= 0) return
        session.format.set(PcmFormat(sampleRateHz, channelCount, encoding))
        session.formatGeneration.incrementAndGet()
        Timber.d(
            "Local skip PCM format: animeId=%d ep=%d rate=%d channels=%d encoding=%d",
            session.request.animeId,
            session.request.episodeNumber,
            sampleRateHz,
            channelCount,
            encoding,
        )
    }

    override fun onPcmBuffer(buffer: ByteBuffer) {
        val session = activeSession ?: return
        val format = session.format.get() ?: return
        val positionMs = session.positionMs.get()
        val durationMs = session.durationMs.get()
        if (!session.shouldCapture(positionMs, durationMs)) return
        if (!isSupportedEncoding(format.encoding)) return
        val size = buffer.remaining()
        if (size <= 0 || size > MAX_PCM_CHUNK_BYTES) return
        val copy = ByteArray(size)
        buffer.duplicate().get(copy)
        if (session.firstPcmLogged.compareAndSet(false, true)) {
            Timber.d(
                "Local skip PCM started: animeId=%d ep=%d position=%d duration=%d chunk=%d",
                session.request.animeId,
                session.request.episodeNumber,
                positionMs,
                durationMs,
                size,
            )
        }
        val sequence = session.nextSequence.getAndIncrement()
        session.pcmChannel.trySend(
            PcmChunk(
                sequence = sequence,
                generation = session.formatGeneration.get(),
                format = format,
                positionMs = positionMs,
                durationMs = durationMs,
                data = copy,
            ),
        )
    }

    override fun stop() {
        activeSession?.let(::stopSession)
        activeSession = null
    }

    private fun stopSession(session: DetectionSession) {
        if (activeSession === session) activeSession = null
        session.pcmChannel.close()
        session.worker?.cancel()
    }

    private suspend fun processPcm(session: DetectionSession) {
        var processor: StreamingAudioFingerprinter? = null
        var generation = Int.MIN_VALUE
        var lastSequence = -1L
        for (chunk in session.pcmChannel) {
            if (activeSession !== session) break
            val discontinuity =
                chunk.generation != generation ||
                    (lastSequence >= 0L && chunk.sequence != lastSequence + 1L)
            if (discontinuity) {
                generation = chunk.generation
                processor = StreamingAudioFingerprinter(
                    sampleRateHz = chunk.format.sampleRateHz,
                    channelCount = chunk.format.channelCount,
                    encoding = chunk.format.encoding,
                    absoluteStartFrame = FingerprintConfig.msToFrame(chunk.positionMs),
                    onLandmarks = session::consumeLandmarks,
                )
            }
            lastSequence = chunk.sequence
            session.withMatcher { it.updateDuration(chunk.durationMs) }
            processor?.consume(chunk.data)
        }
    }

    private suspend fun loadReferences(
        request: LocalSkipRequest,
        resolvedTypes: Set<SkipType>,
    ): List<FingerprintReference> = withContext(Dispatchers.IO) {
        fingerprintDao.deleteOutdated(request.animeId, FingerprintConfig.ALGORITHM_VERSION)
        val cachedEntities = fingerprintDao.get(
            request.animeId,
            FingerprintConfig.ALGORITHM_VERSION,
        )
        val applicable = cachedEntities
            .filter { episodeMatches(it.episodes, request.episodeNumber) }
            .mapNotNull { it.toReference() }
            .toMutableList()
        val missingTypes = ALL_SKIP_TYPES - resolvedTypes - applicable.mapTo(mutableSetOf()) { it.type }
        Timber.d(
            "Local skip cache: animeId=%d ep=%d cachedRefs=%d resolved=%s missing=%s",
            request.animeId,
            request.episodeNumber,
            applicable.size,
            resolvedTypes,
            missingTypes,
        )
        if (missingTypes.isEmpty()) return@withContext applicable

        val queryKey = queryKey(request)
        val lookup = lookupDao.get(request.animeId, request.episodeNumber)
        if (lookup?.queryKey == queryKey) {
            Timber.d(
                "Local skip negative lookup cache hit: animeId=%d ep=%d",
                request.animeId,
                request.episodeNumber,
            )
            return@withContext applicable
        }

        val queries = buildSearchQueries(request)
        val remoteReferences = animeThemesClient.findReferences(
            searchQueries = queries,
            year = request.year,
            episodeNumber = request.episodeNumber,
        ).filter { reference ->
            val type = reference.toSkipType()
            type != null &&
                type in missingTypes &&
                cachedEntities.none { it.referenceId == reference.id }
        }
        Timber.d(
            "Local skip remote refs: animeId=%d ep=%d count=%d refs=%s",
            request.animeId,
            request.episodeNumber,
            remoteReferences.size,
            remoteReferences.map { "${it.type}:${it.id}:${it.episodes}" },
        )

        var completed = true
        for (remote in remoteReferences) {
            try {
                Timber.d(
                    "Local skip fingerprinting started: animeId=%d ref=%d type=%s",
                    request.animeId,
                    remote.id,
                    remote.type,
                )
                val result = referenceAudioDecoder.fingerprint(remote)
                val type = remote.toSkipType() ?: continue
                val entity = ThemeFingerprintEntity(
                    animeId = request.animeId,
                    referenceId = remote.id,
                    type = type.name,
                    episodes = remote.episodes,
                    durationMs = result.durationMs,
                    algorithmVersion = FingerprintConfig.ALGORITHM_VERSION,
                    landmarks = encodeLandmarks(result.landmarks),
                )
                fingerprintDao.insert(entity)
                entity.toReference()?.let(applicable::add)
                Timber.d(
                    "Cached local fingerprint: animeId=%d ref=%d type=%s points=%d",
                    request.animeId,
                    remote.id,
                    type,
                    result.landmarks.size,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                completed = false
                Timber.w(error, "Could not fingerprint AnimeThemes ref=%d", remote.id)
            }
        }
        if (completed) {
            lookupDao.insert(
                ThemeLookupEntity(
                    animeId = request.animeId,
                    episode = request.episodeNumber,
                    queryKey = queryKey,
                ),
            )
        }
        applicable.distinctBy { it.id }
    }

    private fun onIntervalFound(session: DetectionSession, interval: SkipInterval) {
        if (activeSession !== session || !session.resolvedTypes.add(interval.type)) return
        val request = session.request
        scope.launch(Dispatchers.IO) {
            skipTimeDao.insertAll(
                listOf(
                    SkipTimeEntity(
                        animeId = request.animeId,
                        episode = request.episodeNumber,
                        type = interval.type.name,
                        startMs = interval.startMs,
                        endMs = interval.endMs,
                        source = SkipTimeEntity.SOURCE_LOCAL,
                    ),
                ),
            )
        }
        mutableEvents.tryEmit(
            LocalSkipEvent.IntervalFound(
                animeId = request.animeId,
                episodeNumber = request.episodeNumber,
                interval = interval,
            ),
        )
        Timber.i(
            "Local skip detected: animeId=%d ep=%d type=%s start=%d end=%d",
            request.animeId,
            request.episodeNumber,
            interval.type,
            interval.startMs,
            interval.endMs,
        )
    }

    private fun buildSearchQueries(request: LocalSkipRequest): List<String> {
        val values = LinkedHashSet<String>()
        request.titleOriginal
            .split(',')
            .map(String::trim)
            .filter(String::isNotBlank)
            .forEach(values::add)
        listOf(request.titleAlternative, request.title).forEach { title ->
            title.trim().takeIf(String::isNotBlank)?.let(values::add)
        }
        return values.toList()
    }

    private fun queryKey(request: LocalSkipRequest): String =
        "$LOOKUP_RULE_VERSION|" + buildSearchQueries(request)
            .joinToString("|")
            .lowercase(Locale.ROOT) + "|${request.year.orEmpty()}"

    private fun Int?.orEmpty(): String = this?.toString().orEmpty()

    private fun episodeMatches(spec: String, episode: Int): Boolean {
        if (spec.isBlank()) return true
        return spec.replace(" ", "").split(',').any { part ->
            when {
                part.matches(Regex("\\d+")) -> part.toIntOrNull() == episode
                part.count { it == '-' } == 1 -> {
                    val start = part.substringBefore('-').toIntOrNull() ?: 1
                    val end = part.substringAfter('-').toIntOrNull() ?: Int.MAX_VALUE
                    episode in start..end
                }
                else -> false
            }
        }
    }

    private fun ThemeFingerprintEntity.toReference(): FingerprintReference? {
        val skipType = runCatching { SkipType.valueOf(type) }.getOrNull() ?: return null
        val decoded = decodeLandmarks(landmarks)
        if (decoded.isEmpty()) return null
        return FingerprintReference(
            id = referenceId,
            type = skipType,
            durationMs = durationMs,
            landmarks = decoded,
        )
    }

    private fun AnimeThemeReference.toSkipType(): SkipType? = when (type) {
        "OP" -> SkipType.OP
        "ED" -> SkipType.ED
        else -> null
    }

    private fun List<SkipTimeEntity>.toSkipIntervals(): List<SkipInterval> = mapNotNull { entity ->
        val type = runCatching { SkipType.valueOf(entity.type) }.getOrNull()
            ?: return@mapNotNull null
        SkipInterval(type, entity.startMs, entity.endMs)
    }

    private fun isSupportedEncoding(encoding: Int): Boolean = encoding in SUPPORTED_ENCODINGS

    private class DetectionSession(
        val request: LocalSkipRequest,
        initialResolvedTypes: Set<SkipType>,
    ) {
        val resolvedTypes: MutableSet<SkipType> =
            ConcurrentHashMap.newKeySet<SkipType>().apply { addAll(initialResolvedTypes) }
        val pcmChannel = Channel<PcmChunk>(capacity = PCM_QUEUE_CAPACITY)
        val positionMs = AtomicLong(0L)
        val durationMs = AtomicLong(0L)
        val format = AtomicReference<PcmFormat?>()
        val formatGeneration = AtomicInteger(0)
        val nextSequence = AtomicLong(0L)
        val detectableTypes = AtomicReference<Set<SkipType>>(emptySet())
        val firstPcmLogged = AtomicBoolean(false)
        private val firstLandmarksLogged = AtomicBoolean(false)
        var worker: Job? = null

        private val matchLock = Any()
        @Volatile
        private var matcher: FingerprintMatcher? = null
        private val pendingLandmarks = ArrayDeque<LongArray>()

        fun installMatcher(
            references: List<FingerprintReference>,
            onMatch: (SkipInterval) -> Unit,
        ) {
            synchronized(matchLock) {
                val newMatcher = FingerprintMatcher(references, resolvedTypes.toSet(), onMatch)
                matcher = newMatcher
                newMatcher.updateDuration(durationMs.get())
                pendingLandmarks.forEach(newMatcher::consume)
                pendingLandmarks.clear()
            }
        }

        fun consumeLandmarks(batch: LongArray) {
            if (firstLandmarksLogged.compareAndSet(false, true)) {
                Timber.d(
                    "Local skip fingerprint stream started: animeId=%d ep=%d batch=%d",
                    request.animeId,
                    request.episodeNumber,
                    batch.size,
                )
            }
            synchronized(matchLock) {
                val activeMatcher = matcher
                if (activeMatcher != null) {
                    activeMatcher.consume(batch)
                } else {
                    if (pendingLandmarks.size >= MAX_PENDING_BATCHES) {
                        pendingLandmarks.removeFirst()
                    }
                    pendingLandmarks.addLast(batch)
                }
            }
        }

        inline fun withMatcher(block: (FingerprintMatcher) -> Unit) {
            synchronized(matchLock) {
                matcher?.let(block)
            }
        }

        fun shouldCapture(positionMs: Long, durationMs: Long): Boolean {
            val types = detectableTypes.get()
            val preparing = matcher == null
            val scanOpening =
                SkipType.OP !in resolvedTypes &&
                    (preparing || SkipType.OP in types) &&
                    positionMs <= FingerprintConfig.OPENING_SCAN_MS
            val scanEnding =
                SkipType.ED !in resolvedTypes &&
                    (preparing || SkipType.ED in types) &&
                    durationMs > 0L &&
                    positionMs >= durationMs - FingerprintConfig.ENDING_SCAN_MS
            return scanOpening || scanEnding
        }
    }

    private data class PcmFormat(
        val sampleRateHz: Int,
        val channelCount: Int,
        val encoding: Int,
    )

    private data class PcmChunk(
        val sequence: Long,
        val generation: Int,
        val format: PcmFormat,
        val positionMs: Long,
        val durationMs: Long,
        val data: ByteArray,
    )

    private companion object {
        val ALL_SKIP_TYPES = setOf(SkipType.OP, SkipType.ED)
        val SUPPORTED_ENCODINGS = setOf(
            StreamingAudioFingerprinter.ENCODING_PCM_8BIT,
            StreamingAudioFingerprinter.ENCODING_PCM_16BIT,
            StreamingAudioFingerprinter.ENCODING_PCM_FLOAT,
            StreamingAudioFingerprinter.ENCODING_PCM_24BIT,
            StreamingAudioFingerprinter.ENCODING_PCM_32BIT,
        )
        const val PCM_QUEUE_CAPACITY = 64
        const val MAX_PENDING_BATCHES = 400
        const val MAX_PCM_CHUNK_BYTES = 512 * 1_024
        const val LOOKUP_RULE_VERSION = 2
    }
}
