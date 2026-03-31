package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.SkipSegment
import com.animevost.app.core.domain.repository.SkipRepository
import com.animevost.app.core.domain.util.Result
import javax.inject.Inject

/** Returns skip segments for [episodeName] of [animeId]. */
class GetSkipSegmentsUseCase @Inject constructor(
    private val repository: SkipRepository,
) {
    suspend operator fun invoke(animeId: Int, episodeName: String): Result<List<SkipSegment>> =
        repository.getSkipSegments(animeId, episodeName)
}
