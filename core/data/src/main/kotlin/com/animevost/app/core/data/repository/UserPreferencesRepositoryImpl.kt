package com.animevost.app.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.animevost.app.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override fun preferredVideoQuality(): Flow<String?> =
        dataStore.data.map { prefs -> prefs[KEY_PREFERRED_VIDEO_QUALITY] }

    override suspend fun setPreferredVideoQuality(quality: String) {
        dataStore.edit { prefs -> prefs[KEY_PREFERRED_VIDEO_QUALITY] = quality }
    }

    override suspend fun getUserRating(animeId: Int): Int {
        if (animeId <= 0) return 0
        return dataStore.data.first()[userRatingKey(animeId)] ?: 0
    }

    override suspend fun setUserRating(animeId: Int, rating: Int) {
        if (animeId <= 0) return
        dataStore.edit { prefs ->
            val key = userRatingKey(animeId)
            if (rating in 1..5) {
                prefs[key] = rating
            } else {
                prefs.remove(key)
            }
        }
    }

    private fun userRatingKey(animeId: Int) =
        intPreferencesKey("anime_rating_$animeId")

    private companion object {
        val KEY_PREFERRED_VIDEO_QUALITY = stringPreferencesKey("preferred_quality")
    }
}
