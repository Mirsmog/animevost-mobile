package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.User

/** Handles authentication operations (login, register, logout, session). */
interface AuthRepository {
    /** Authenticates with [username]/[password] and returns the logged-in [User]. */
    suspend fun login(username: String, password: String): User

    /** Registers a new account and returns the created [User]. */
    suspend fun register(username: String, password: String, email: String): User

    /** Clears session cookies and persisted credentials. */
    suspend fun logout()

    /** Returns the currently logged-in [User], or `null` if not authenticated. */
    suspend fun getCurrentUser(): User?

    /** Returns `true` if a valid session exists. */
    suspend fun isLoggedIn(): Boolean
}
