package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.AnimeDetail
import com.animevost.app.core.domain.repository.AnimeRepository
import javax.inject.Inject

class GetAnimeDetailUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(url: String): AnimeDetail {
        return repository.getAnimeDetail(url)
    }
}
