package com.animevost.app.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
    }

    override suspend fun login(username: String, password: String): User {
        val response = api.login(username, password)
        val body = response.string()
        if (body.contains("Неверный логин") || body.contains("Неверный пароль")) {
            throw IllegalArgumentException("Invalid credentials")
        }
        dataStore.edit { it[KEY_USERNAME] = username }
        return User(id = 0, name = username, avatarUrl = "", isLoggedIn = true)
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
            throw IllegalArgumentException("Registration failed")
        }
        dataStore.edit { it[KEY_USERNAME] = username }
        return User(id = 0, name = username, avatarUrl = "", isLoggedIn = true)
    }

    override suspend fun logout() {
        cookieJar.clear()
        dataStore.edit { it.remove(KEY_USERNAME) }
    }

    override suspend fun getCurrentUser(): User? {
        val username = dataStore.data
            .map { it[KEY_USERNAME] }
            .firstOrNull()
            ?: return null
        return User(id = 0, name = username, avatarUrl = "", isLoggedIn = true)
    }

    override suspend fun isLoggedIn(): Boolean = getCurrentUser() != null
}
