package com.animevost.app.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.animevost.app.core.domain.model.User
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.network.AnimeVostApi
import com.animevost.app.core.network.DleEndpoints
import com.animevost.app.core.network.HtmlFetcher
import com.animevost.app.core.network.SessionCookieJar
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AnimeVostApi,
    private val htmlFetcher: HtmlFetcher,
    private val cookieJar: SessionCookieJar,
    private val dataStore: DataStore<Preferences>,
) : AuthRepository {

    internal companion object {
        val KEY_USERNAME = stringPreferencesKey("auth_username")
        val KEY_USER_ID = intPreferencesKey("auth_user_id")
    }

    override suspend fun login(username: String, password: String): User {
        val body = api.login(username, password).string()
        if (body.contains("Ошибка авторизации") || body.contains("berrors")) {
            throw IllegalArgumentException("Неверный логин или пароль")
        }
        val userId = cookieJar.getCookieValue("animevost.org", "dle_user_id")
            ?.toIntOrNull() ?: 0
        dataStore.edit { prefs ->
            prefs[KEY_USERNAME] = username
            prefs[KEY_USER_ID] = userId
        }
        return User(id = userId, name = username, avatarUrl = "", isLoggedIn = true)
    }

    override suspend fun register(username: String, password: String, email: String): User {
        val url = DleEndpoints.BASE_URL + "index.php?do=register"
        val params = mapOf(
            "submit_reg" to "submit",
            "login_name" to username,
            "login_password" to password,
            "login_password2" to password,
            "email" to email,
        )
        val html = htmlFetcher.fetchPost(url, params)
        if (html.contains("уже используется") || html.contains("Ошибка")) {
            throw IllegalArgumentException("Ошибка регистрации")
        }
        dataStore.edit { it[KEY_USERNAME] = username }
        return User(id = 0, name = username, avatarUrl = "", isLoggedIn = true)
    }

    override suspend fun logout() {
        try {
            htmlFetcher.fetch(DleEndpoints.BASE_URL + DleEndpoints.LOGOUT)
        } catch (_: Exception) {
            // Server-side logout is best-effort; always clear local session
        }
        cookieJar.clear()
        dataStore.edit { prefs ->
            prefs.remove(KEY_USERNAME)
            prefs.remove(KEY_USER_ID)
        }
    }

    override suspend fun getCurrentUser(): User? {
        val username = dataStore.data
            .map { it[KEY_USERNAME] }
            .firstOrNull() ?: return null
        val cookieUserId = cookieJar.getCookieValue("animevost.org", "dle_user_id")
        if (cookieUserId == null || cookieUserId == "deleted") return null
        val userId = dataStore.data
            .map { it[KEY_USER_ID] }
            .firstOrNull() ?: cookieUserId.toIntOrNull() ?: 0
        return User(id = userId, name = username, avatarUrl = "", isLoggedIn = true)
    }

    override suspend fun isLoggedIn(): Boolean = getCurrentUser() != null
}
