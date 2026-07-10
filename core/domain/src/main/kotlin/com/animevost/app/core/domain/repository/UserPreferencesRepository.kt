package com.animevost.app.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** Reactive access to user preferences persisted in DataStore. */
interface UserPreferencesRepository {

    fun preferredVideoQuality(): Flow<String?>

    suspend fun setPreferredVideoQuality(quality: String)

    suspend fun getUserRating(animeId: Int): Int

    suspend fun setUserRating(animeId: Int, rating: Int)
}
