package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.SkipSegment

interface SkipRepository {
    suspend fun getSkipSegments(animeId: Int, episodeName: String): List<SkipSegment>
    suspend fun saveSkipSegment(animeId: Int, episodeName: String, segment: SkipSegment)
}
