package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.User
import com.animevost.app.core.domain.repository.AuthRepository
import javax.inject.Inject

/** Registers a new account and returns the created [User] on success. */
class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(username: String, password: String, email: String): User =
        repository.register(username, password, email)
}
