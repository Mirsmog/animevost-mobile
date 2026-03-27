package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.repository.AnimeRepository
import javax.inject.Inject

class SearchAnimeUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(query: String, page: Int): List<AnimePreview> {
        return repository.search(query, page)
    }
}
