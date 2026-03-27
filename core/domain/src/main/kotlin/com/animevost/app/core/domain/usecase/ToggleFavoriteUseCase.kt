package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.repository.FavoriteRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: FavoriteRepository,
) {
    suspend operator fun invoke(newsId: Int): Boolean {
        return repository.toggleFavorite(newsId)
    }
}
