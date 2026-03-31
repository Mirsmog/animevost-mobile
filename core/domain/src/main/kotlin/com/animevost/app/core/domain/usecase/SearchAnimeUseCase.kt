package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.repository.AnimeRepository
import com.animevost.app.core.domain.util.Result
import javax.inject.Inject

/** Searches for anime matching [query] on the given [page]. */
class SearchAnimeUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(query: String, page: Int): Result<List<AnimePreview>> =
        repository.search(query, page)
}
