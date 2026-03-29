package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.SkipSegment
import com.animevost.app.core.domain.repository.SkipRepository
import javax.inject.Inject

class SaveSkipSegmentUseCase @Inject constructor(
    private val repository: SkipRepository,
) {
    suspend operator fun invoke(animeId: Int, episodeName: String, segment: SkipSegment) {
        repository.saveSkipSegment(animeId, episodeName, segment)
    }
}
