package com.animevost.app.core.data.mapper

import com.animevost.app.core.data.db.SkipSegmentEntity
import com.animevost.app.core.domain.model.SkipSegment
import com.animevost.app.core.domain.model.SkipSource
import com.animevost.app.core.domain.model.SkipType

internal fun SkipSegmentEntity.toDomain(): SkipSegment = SkipSegment(
    type = when (type) {
        "INTRO" -> SkipType.INTRO
        else -> SkipType.OUTRO
    },
    startMs = startMs,
    endMs = endMs,
    source = when (source) {
        "USER" -> SkipSource.USER
        else -> SkipSource.ANISKIP
    },
)

internal fun SkipSegment.toEntity(animeId: Int, episodeName: String): SkipSegmentEntity =
    SkipSegmentEntity(
        animeId = animeId,
        episodeName = episodeName,
        type = type.name,
        startMs = startMs,
        endMs = endMs,
        source = source.name,
    )
