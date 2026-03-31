package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.SkipSegment
import com.animevost.app.core.domain.repository.SkipRepository
import com.animevost.app.core.domain.util.Result
import javax.inject.Inject

/** Persists a user-defined or fetched [segment] for the given episode. */
class SaveSkipSegmentUseCase @Inject constructor(
    private val repository: SkipRepository,
) {
    suspend operator fun invoke(animeId: Int, episodeName: String, segment: SkipSegment): Result<Unit> =
        repository.saveSkipSegment(animeId, episodeName, segment)
}
