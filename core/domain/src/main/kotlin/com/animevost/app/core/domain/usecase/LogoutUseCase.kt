package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.repository.AuthRepository
import javax.inject.Inject

/** Clears the current session — cookies and persisted credentials. */
class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke() {
        repository.logout()
    }
}
