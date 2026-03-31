package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.repository.FavoriteRepository
import javax.inject.Inject

/**
 * Toggles the favourite status for an anime.
 * @return `true` if added to favourites, `false` if removed.
 */
class ToggleFavoriteUseCase @Inject constructor(
    private val repository: FavoriteRepository,
) {
    suspend operator fun invoke(newsId: Int, preview: AnimePreview? = null): Boolean {
        return repository.toggleFavorite(newsId, preview)
    }
}
