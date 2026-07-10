package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.domain.repository.FavoriteRepository
import javax.inject.Inject

/** Clears the current session — cookies and persisted credentials. */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val favoriteRepository: FavoriteRepository,
) {
    suspend operator fun invoke() {
        authRepository.logout()
        favoriteRepository.clearLocal()
    }
}
