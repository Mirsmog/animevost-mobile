package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.repository.AnimeRepository
import com.animevost.app.core.domain.util.Result
import javax.inject.Inject

/** Submits a user [rating] for [newsId] and returns the updated average rating. */
class RateAnimeUseCase @Inject constructor(
    private val animeRepository: AnimeRepository,
) {
    suspend operator fun invoke(newsId: Int, rating: Int): Result<Double> =
        animeRepository.submitRating(newsId, rating)
}
