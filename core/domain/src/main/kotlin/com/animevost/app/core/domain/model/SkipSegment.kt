package com.animevost.app.core.domain.model

data class SkipSegment(
    val type: SkipType,
    val startMs: Long,
    val endMs: Long,
    val source: SkipSource,
)

enum class SkipType { INTRO, OUTRO }

enum class SkipSource { ANISKIP, USER }
