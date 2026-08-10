package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.SkipInterval
import com.animevost.app.core.domain.model.SkipType
import com.animevost.app.core.network.alloha.AllohaSkipRange
import com.animevost.app.core.network.aniskip.AniSkipHint
import com.animevost.app.core.network.aniskip.AniSkipLookup
import com.animevost.app.core.network.aniskip.AniSkipType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal object SkipIntervalClassifier {
    fun classify(
        allohaRanges: List<AllohaSkipRange>,
        aniSkipLookup: AniSkipLookup,
    ): List<SkipInterval> {
        val ranges = allohaRanges
            .filter { it.endMs - it.startMs >= MIN_ACTIONABLE_DURATION_MS }
            .sortedBy { it.startMs }
        if (ranges.isEmpty()) return emptyList()

        val assignments = mutableMapOf<Int, SkipType>()
        val assignedTypes = mutableSetOf<SkipType>()
        val matches = ranges.flatMapIndexed { rangeIndex, range ->
            aniSkipLookup.hints.mapNotNull { hint ->
                buildMatch(rangeIndex, range, hint)
            }
        }.sortedByDescending(Match::score)

        for (match in matches) {
            if (match.rangeIndex in assignments || match.type in assignedTypes) continue
            assignments[match.rangeIndex] = match.type
            assignedTypes += match.type
        }

        val openingWindowEndMs = aniSkipLookup.episodeDurationMs
            ?.let { min(MAX_OPENING_START_MS, it / 2) }
            ?: LEGACY_OPENING_START_MS
        if (SkipType.OP !in assignedTypes) {
            ranges.indices.firstOrNull { index ->
                index !in assignments && ranges[index].startMs <= openingWindowEndMs
            }?.let { index ->
                assignments[index] = SkipType.OP
                assignedTypes += SkipType.OP
            }
        }

        val endingWindowStartMs = aniSkipLookup.episodeDurationMs
            ?.let { it / 2 }
            ?: MIN_ENDING_START_MS
        if (SkipType.ED !in assignedTypes) {
            ranges.indices.lastOrNull { index ->
                index !in assignments && ranges[index].startMs >= endingWindowStartMs
            }?.let { index ->
                assignments[index] = SkipType.ED
            }
        }

        return assignments.map { (index, type) ->
            val range = ranges[index]
            SkipInterval(type = type, startMs = range.startMs, endMs = range.endMs)
        }.sortedBy(SkipInterval::startMs)
    }

    private fun buildMatch(
        rangeIndex: Int,
        range: AllohaSkipRange,
        hint: AniSkipHint,
    ): Match? {
        val overlapMs = (
            min(range.endMs, hint.endMs) - max(range.startMs, hint.startMs)
            ).coerceAtLeast(0L)
        if (overlapMs < MIN_HINT_OVERLAP_MS) return null

        val shortestDurationMs = min(
            range.endMs - range.startMs,
            hint.endMs - hint.startMs,
        )
        if (shortestDurationMs <= 0L) return null
        val coverage = overlapMs.toDouble() / shortestDurationMs.toDouble()
        if (coverage < MIN_HINT_COVERAGE && overlapMs < STRONG_HINT_OVERLAP_MS) return null

        val rangeCenterMs = (range.startMs + range.endMs) / 2
        val hintCenterMs = (hint.startMs + hint.endMs) / 2
        val type = when (hint.type) {
            AniSkipType.OPENING -> SkipType.OP
            AniSkipType.ENDING -> SkipType.ED
        }
        val score = (coverage * COVERAGE_SCORE_SCALE).toLong() +
            overlapMs / 10L -
            abs(rangeCenterMs - hintCenterMs) / 100L
        return Match(rangeIndex = rangeIndex, type = type, score = score)
    }

    private data class Match(
        val rangeIndex: Int,
        val type: SkipType,
        val score: Long,
    )

    private const val MIN_ACTIONABLE_DURATION_MS = 10_000L
    private const val MIN_HINT_OVERLAP_MS = 5_000L
    private const val STRONG_HINT_OVERLAP_MS = 30_000L
    private const val MIN_HINT_COVERAGE = 0.35
    private const val COVERAGE_SCORE_SCALE = 100_000.0
    private const val LEGACY_OPENING_START_MS = 2L * 60L * 1000L
    private const val MAX_OPENING_START_MS = 8L * 60L * 1000L
    private const val MIN_ENDING_START_MS = 8L * 60L * 1000L
}
