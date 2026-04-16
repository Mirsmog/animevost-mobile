package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.SegmentType
import com.animevost.app.core.domain.model.SkipSegment

interface SkipSegmentRepository {
    suspend fun getSegments(animeId: Int): List<SkipSegment>
    suspend fun saveSegment(segment: SkipSegment)
    suspend fun deleteSegment(animeId: Int, type: SegmentType)
}
