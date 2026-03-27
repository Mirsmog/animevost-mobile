package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.NavData
import com.animevost.app.core.domain.repository.AnimeRepository
import javax.inject.Inject

class GetNavDataUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(): NavData = repository.getNavData()
}
