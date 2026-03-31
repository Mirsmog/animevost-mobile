package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.NavData
import com.animevost.app.core.domain.repository.AnimeRepository
import com.animevost.app.core.domain.util.Result
import javax.inject.Inject

/** Retrieves genres and years for navigation menus. */
class GetNavDataUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(): Result<NavData> = repository.getNavData()
}
