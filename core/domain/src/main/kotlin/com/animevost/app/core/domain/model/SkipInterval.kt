package com.animevost.app.core.domain.model

enum class SkipType { OP, ED }

data class SkipInterval(
    val type: SkipType,
    val startMs: Long,
    val endMs: Long,
)
