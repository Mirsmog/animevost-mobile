package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.repository.FavoriteRepository
import com.animevost.app.core.domain.util.Result
import javax.inject.Inject

/** Merges local favorites with the remote server on login. */
class SyncFavoritesUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
) {
    suspend operator fun invoke(): Result<Unit> = favoriteRepository.syncOnLogin()
}
