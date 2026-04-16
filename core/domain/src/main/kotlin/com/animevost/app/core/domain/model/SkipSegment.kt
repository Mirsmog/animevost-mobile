package com.animevost.app.core.domain.model

data class SkipSegment(
    val animeId: Int,
    val type: SegmentType,
    val hash0: Long,
    val hash3: Long,
    val hash6: Long,
    val durationMs: Long,
    val referenceTimeMs: Long,
)
