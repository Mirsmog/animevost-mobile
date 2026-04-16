package com.animevost.app.core.data.repository

import com.animevost.app.core.data.db.SkipSegmentDao
import com.animevost.app.core.data.db.SkipSegmentEntity
import com.animevost.app.core.domain.model.SegmentType
import com.animevost.app.core.domain.model.SkipSegment
import com.animevost.app.core.domain.repository.SkipSegmentRepository
import javax.inject.Inject

class SkipSegmentRepositoryImpl @Inject constructor(
    private val dao: SkipSegmentDao,
) : SkipSegmentRepository {

    override suspend fun getSegments(animeId: Int): List<SkipSegment> =
        dao.getByAnimeId(animeId).map { it.toDomain() }

    override suspend fun saveSegment(segment: SkipSegment) {
        dao.upsert(segment.toEntity())
    }

    override suspend fun deleteSegment(animeId: Int, type: SegmentType) {
        dao.delete(animeId, type.name)
    }

    private fun SkipSegment.toEntity() = SkipSegmentEntity(
        animeId = animeId,
        type = type.name,
        hash0 = hash0,
        hash3 = hash3,
        hash6 = hash6,
        durationMs = durationMs,
        referenceTimeMs = referenceTimeMs,
    )

    private fun SkipSegmentEntity.toDomain() = SkipSegment(
        animeId = animeId,
        type = SegmentType.valueOf(type),
        hash0 = hash0,
        hash3 = hash3,
        hash6 = hash6,
        durationMs = durationMs,
        referenceTimeMs = referenceTimeMs,
    )
}
