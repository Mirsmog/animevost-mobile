package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.SkipInterval
import com.animevost.app.core.domain.model.SkipType
import kotlinx.coroutines.flow.Flow
import java.nio.ByteBuffer

data class LocalSkipRequest(
    val animeId: Int,
    val episodeNumber: Int,
    val title: String,
    val titleOriginal: String,
    val titleAlternative: String,
    val year: Int?,
)

data class LocalSkipPreparation(
    val cachedIntervals: List<SkipInterval>,
    val detectableTypes: Set<SkipType>,
)

sealed interface LocalSkipEvent {
    val animeId: Int
    val episodeNumber: Int

    data class IntervalFound(
        override val animeId: Int,
        override val episodeNumber: Int,
        val interval: SkipInterval,
    ) : LocalSkipEvent
}

interface LocalSkipDetector {
    val events: Flow<LocalSkipEvent>

    suspend fun prepare(request: LocalSkipRequest): LocalSkipPreparation

    fun updatePlaybackPosition(positionMs: Long, durationMs: Long)

    fun onPcmFormat(sampleRateHz: Int, channelCount: Int, encoding: Int)

    fun onPcmBuffer(buffer: ByteBuffer)

    fun stop()
}
