package com.animevost.app.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.animevost.app.core.domain.model.LibraryViewMode
import com.animevost.app.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override fun libraryViewMode(): Flow<LibraryViewMode> =
        dataStore.data.map { prefs ->
            val raw = prefs[KEY_LIBRARY_VIEW_MODE]
            runCatching { LibraryViewMode.valueOf(raw ?: "") }.getOrDefault(LibraryViewMode.GRID)
        }

    override suspend fun setLibraryViewMode(mode: LibraryViewMode) {
        dataStore.edit { prefs -> prefs[KEY_LIBRARY_VIEW_MODE] = mode.name }
    }

    private companion object {
        val KEY_LIBRARY_VIEW_MODE = stringPreferencesKey("library_view_mode")
    }
}
