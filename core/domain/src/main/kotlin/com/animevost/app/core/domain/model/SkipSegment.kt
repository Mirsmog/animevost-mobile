package com.animevost.app.core.domain.model

data class SkipSegment(
    val animeId: Int,
    val type: SegmentType,
    /** User-set reference position where the segment starts (in ms). */
    val startMs: Long,
    /** Length of the segment (in ms). */
    val durationMs: Long,
    /**
     * Tolerance window on each side of [startMs] (in ms).
     * Accounts for variable cold-open lengths across episodes.
     * Default: 90 seconds.
     */
    val windowMs: Long = 90_000L,
    /** pHash at startMs. */
    val hash0: Long = 0L,
    /** pHash at startMs + 3 s. */
    val hash3: Long = 0L,
    /** pHash at startMs + 6 s. */
    val hash6: Long = 0L,
)
