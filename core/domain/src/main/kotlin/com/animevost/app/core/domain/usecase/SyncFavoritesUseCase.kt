package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.repository.FavoriteRepository
import com.animevost.app.core.domain.util.Result
import javax.inject.Inject

/** Synchronises local favourites with the remote animevost server. */
class SyncFavoritesUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
) {
    suspend operator fun invoke(): Result<Unit> = favoriteRepository.syncWithRemote()
}
