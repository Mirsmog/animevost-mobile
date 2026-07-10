package com.animevost.app.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.animevost.app.core.domain.model.User
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.sdk.AnimeVostClient
import com.animevost.sdk.error.AnimeVostAuthException
import com.animevost.sdk.error.AnimeVostRegistrationException
import com.animevost.sdk.model.RegistrationRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val client: AnimeVostClient,
    private val dataStore: DataStore<Preferences>,
) : AuthRepository {

    internal companion object {
        val KEY_USERNAME = stringPreferencesKey("auth_username")
        val KEY_USER_ID = intPreferencesKey("auth_user_id")
        val KEY_AVATAR_URL = stringPreferencesKey("auth_avatar_url")
    }

    private val _isLoggedInFlow = MutableStateFlow(client.isLoggedIn())
    override val isLoggedInFlow: Flow<Boolean> = _isLoggedInFlow.asStateFlow()

    override suspend fun login(username: String, password: String): User {
        val session = try {
            client.login(username, password)
        } catch (e: AnimeVostAuthException) {
            throw IllegalArgumentException("Неверный логин или пароль", e)
        }
        val displayName = session.username ?: username
        dataStore.edit { prefs ->
            prefs[KEY_USERNAME] = displayName
            prefs[KEY_USER_ID] = session.userId
            prefs.remove(KEY_AVATAR_URL)
        }
        _isLoggedInFlow.value = true
        return User(id = session.userId, name = displayName, avatarUrl = "", isLoggedIn = true)
    }

    override suspend fun register(username: String, password: String, email: String): User {
        val result = try {
            client.register(
                RegistrationRequest(
                    username = username,
                    password = password,
                    email = email,
                ),
            )
        } catch (e: AnimeVostRegistrationException) {
            val message = e.message
                ?.takeUnless { it == "Registration failed" }
                ?: "Не удалось зарегистрироваться. Проверьте введенные данные"
            throw IllegalArgumentException(message, e)
        }
        val session = result.session
        dataStore.edit { prefs ->
            prefs[KEY_USERNAME] = result.username
            prefs.remove(KEY_AVATAR_URL)
            if (session != null) {
                prefs[KEY_USER_ID] = session.userId
            }
        }
        _isLoggedInFlow.value = session != null
        return User(
            id = session?.userId ?: 0,
            name = result.username,
            avatarUrl = "",
            isLoggedIn = session != null,
        )
    }

    override suspend fun logout() {
        // Stop authenticated background work before the network logout starts.
        _isLoggedInFlow.value = false
        try {
            client.logout()
        } catch (_: Exception) {
        }
        try {
            dataStore.edit { prefs ->
                prefs.remove(KEY_USERNAME)
                prefs.remove(KEY_USER_ID)
                prefs.remove(KEY_AVATAR_URL)
            }
        } catch (_: Exception) { }
    }

    override suspend fun getCurrentUser(): User? {
        val username = dataStore.data
            .map { it[KEY_USERNAME] }
            .firstOrNull() ?: return null
        if (!client.isLoggedIn()) return null
        val userId = dataStore.data
            .map { it[KEY_USER_ID] }
            .firstOrNull() ?: client.currentSession()?.userId ?: 0
        val avatarUrl = dataStore.data
            .map { it[KEY_AVATAR_URL] }
            .firstOrNull() ?: ""
        return User(id = userId, name = username, avatarUrl = avatarUrl, isLoggedIn = true)
    }

    override suspend fun saveAvatarUrl(url: String) {
        dataStore.edit { it[KEY_AVATAR_URL] = url }
    }

    override suspend fun isLoggedIn(): Boolean {
        val loggedIn = getCurrentUser() != null
        _isLoggedInFlow.value = loggedIn
        return loggedIn
    }
}
