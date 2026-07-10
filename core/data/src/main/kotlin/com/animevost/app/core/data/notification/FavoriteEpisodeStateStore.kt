package com.animevost.app.core.data.notification

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class FavoriteEpisodeState(
    val initialized: Boolean,
    val episodesByAnimeId: Map<Int, Int>,
)

@Singleton
class FavoriteEpisodeStateStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun read(): FavoriteEpisodeState {
        val preferences = dataStore.data.first()
        return FavoriteEpisodeState(
            initialized = preferences[KEY_INITIALIZED] ?: false,
            episodesByAnimeId = decode(preferences[KEY_EPISODES].orEmpty()),
        )
    }

    suspend fun replace(episodesByAnimeId: Map<Int, Int>) {
        dataStore.edit { preferences ->
            preferences[KEY_INITIALIZED] = true
            preferences[KEY_EPISODES] = encode(episodesByAnimeId)
        }
    }

    private fun encode(values: Map<Int, Int>): String =
        values.entries
            .asSequence()
            .filter { (animeId, episode) -> animeId > 0 && episode > 0 }
            .sortedBy(Map.Entry<Int, Int>::key)
            .joinToString(separator = ",") { (animeId, episode) -> "$animeId:$episode" }

    private fun decode(value: String): Map<Int, Int> =
        value.splitToSequence(',')
            .mapNotNull { entry ->
                val separator = entry.indexOf(':')
                if (separator <= 0) return@mapNotNull null
                val animeId = entry.substring(0, separator).toIntOrNull() ?: return@mapNotNull null
                val episode = entry.substring(separator + 1).toIntOrNull() ?: return@mapNotNull null
                if (animeId > 0 && episode > 0) animeId to episode else null
            }
            .toMap()

    private companion object {
        val KEY_INITIALIZED = booleanPreferencesKey("favorite_episode_state_initialized")
        val KEY_EPISODES = stringPreferencesKey("favorite_episode_state")
    }
}
