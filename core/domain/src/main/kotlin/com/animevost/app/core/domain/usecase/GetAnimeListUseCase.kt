package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.CatalogFilter
import com.animevost.app.core.domain.repository.AnimeRepository
import javax.inject.Inject

class GetAnimeListUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(page: Int, filter: CatalogFilter): List<AnimePreview> {
        return repository.getAnimeList(page, filter)
    }
}
