package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.User

interface AuthRepository {
    suspend fun login(username: String, password: String): User
    suspend fun register(username: String, password: String, email: String): User
    suspend fun logout()
    suspend fun getCurrentUser(): User?
    suspend fun isLoggedIn(): Boolean
}
