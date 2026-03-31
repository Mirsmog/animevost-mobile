package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.CatalogFilter
import com.animevost.app.core.domain.repository.AnimeRepository
import com.animevost.app.core.domain.util.Result
import javax.inject.Inject

/** Returns a page of [AnimePreview] items for the given [filter]. */
class GetAnimeListUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(page: Int, filter: CatalogFilter): Result<List<AnimePreview>> =
        repository.getAnimeList(page, filter)
}
