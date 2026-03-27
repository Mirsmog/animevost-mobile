package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.repository.FavoriteRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: FavoriteRepository,
) {
    suspend operator fun invoke(newsId: Int, preview: AnimePreview? = null): Boolean {
        return repository.toggleFavorite(newsId, preview)
    }
}
