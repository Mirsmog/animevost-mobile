package com.animevost.app.core.network.aniskip

import kotlin.math.roundToLong

class AniSkipClient(
    private val api: AniSkipApi,
) {
    suspend fun loadSkipHints(malId: Int, episodeNumber: Int): AniSkipLookup {
        val response = api.getSkipTimes(
            malId = malId,
            episodeNumber = episodeNumber,
            types = SUPPORTED_TYPES,
        )
        if (!response.found) return AniSkipLookup.EMPTY

        val hints = response.results.mapNotNull { result ->
            val type = when (result.skipType.lowercase()) {
                "op", "mixed-op" -> AniSkipType.OPENING
                "ed", "mixed-ed" -> AniSkipType.ENDING
                else -> return@mapNotNull null
            }
            val startMs = result.interval.startTime.secondsToMs()
            val endMs = result.interval.endTime.secondsToMs()
            if (startMs < 0L || endMs <= startMs) return@mapNotNull null
            AniSkipHint(type = type, startMs = startMs, endMs = endMs)
        }
        val episodeDurationMs = response.results
            .mapNotNull { it.episodeLength }
            .filter { it > 0.0 }
            .maxOrNull()
            ?.secondsToMs()

        return AniSkipLookup(
            hints = hints,
            episodeDurationMs = episodeDurationMs,
        )
    }

    private fun Double.secondsToMs(): Long = (this * 1000.0).roundToLong()

    private companion object {
        val SUPPORTED_TYPES = listOf("op", "ed", "mixed-op", "mixed-ed")
    }
}

enum class AniSkipType { OPENING, ENDING }

data class AniSkipHint(
    val type: AniSkipType,
    val startMs: Long,
    val endMs: Long,
)

data class AniSkipLookup(
    val hints: List<AniSkipHint>,
    val episodeDurationMs: Long?,
) {
    companion object {
        val EMPTY = AniSkipLookup(emptyList(), null)
    }
}
