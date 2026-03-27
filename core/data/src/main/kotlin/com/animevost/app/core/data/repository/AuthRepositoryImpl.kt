package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.User
import com.animevost.app.core.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    override suspend fun login(username: String, password: String): User {
        TODO("Implement: POST login form, parse response, store session")
    }

    override suspend fun register(username: String, password: String, email: String): User {
        TODO("Implement: POST register form, parse response")
    }

    override suspend fun logout() {
        TODO("Implement: clear cookies and stored user data")
    }

    override suspend fun getCurrentUser(): User? {
        TODO("Implement: check stored session/cookies for current user")
    }

    override suspend fun isLoggedIn(): Boolean {
        TODO("Implement: check if valid session exists")
    }
}
