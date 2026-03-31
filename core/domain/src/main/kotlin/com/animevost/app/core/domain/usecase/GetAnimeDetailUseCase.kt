package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.AnimeDetail
import com.animevost.app.core.domain.repository.AnimeRepository
import com.animevost.app.core.domain.util.Result
import javax.inject.Inject

/** Fetches full [AnimeDetail] for the given anime [url]. */
class GetAnimeDetailUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(url: String): Result<AnimeDetail> =
        repository.getAnimeDetail(url)
}
