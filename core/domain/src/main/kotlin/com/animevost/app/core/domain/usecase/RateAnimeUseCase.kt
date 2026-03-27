package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.repository.AnimeRepository
import javax.inject.Inject

class RateAnimeUseCase @Inject constructor(
    private val animeRepository: AnimeRepository,
) {
    suspend operator fun invoke(newsId: Int, rating: Int): Double {
        return animeRepository.submitRating(newsId, rating)
    }
}
