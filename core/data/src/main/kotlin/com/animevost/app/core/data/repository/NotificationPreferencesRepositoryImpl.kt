package com.animevost.app.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.animevost.app.core.domain.repository.NotificationPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : NotificationPreferencesRepository {

    override val favoriteNotificationsEnabled: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[KEY_FAVORITE_NOTIFICATIONS_ENABLED] ?: true
        }

    override val mutedFavoriteIds: Flow<Set<Int>> =
        dataStore.data.map { preferences ->
            preferences[KEY_MUTED_FAVORITE_IDS]
                .orEmpty()
                .mapNotNull(String::toIntOrNull)
                .toSet()
        }

    override suspend fun setFavoriteNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_FAVORITE_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    override suspend fun setFavoriteMuted(animeId: Int, muted: Boolean) {
        if (animeId <= 0) return
        dataStore.edit { preferences ->
            val ids = preferences[KEY_MUTED_FAVORITE_IDS].orEmpty().toMutableSet()
            if (muted) ids += animeId.toString() else ids -= animeId.toString()
            preferences[KEY_MUTED_FAVORITE_IDS] = ids
        }
    }

    private companion object {
        val KEY_FAVORITE_NOTIFICATIONS_ENABLED =
            booleanPreferencesKey("favorite_notifications_enabled")
        val KEY_MUTED_FAVORITE_IDS =
            stringSetPreferencesKey("muted_favorite_notification_ids")
    }
}
