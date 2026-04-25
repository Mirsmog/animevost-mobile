package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.LibraryViewMode
import kotlinx.coroutines.flow.Flow

/** Reactive access to user preferences persisted in DataStore. */
interface UserPreferencesRepository {

    /** Display mode used for the "All" section of the library screen. */
    fun libraryViewMode(): Flow<LibraryViewMode>

    suspend fun setLibraryViewMode(mode: LibraryViewMode)
}
