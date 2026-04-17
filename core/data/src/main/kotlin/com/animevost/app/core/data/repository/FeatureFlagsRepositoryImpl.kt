package com.animevost.app.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.animevost.app.core.domain.model.BetaFeature
import com.animevost.app.core.domain.repository.FeatureFlagsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FeatureFlagsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : FeatureFlagsRepository {

    override fun isEnabled(feature: BetaFeature): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[booleanPreferencesKey(feature.key)] ?: false }

    override suspend fun setEnabled(feature: BetaFeature, enabled: Boolean) {
        dataStore.edit { prefs -> prefs[booleanPreferencesKey(feature.key)] = enabled }
    }
}
