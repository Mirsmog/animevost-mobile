package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.User
import com.animevost.app.core.domain.repository.AuthRepository
import javax.inject.Inject

/** Authenticates with [username]/[password] and returns the logged-in [User]. */
class LoginUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(username: String, password: String): User {
        return repository.login(username, password)
    }
}
