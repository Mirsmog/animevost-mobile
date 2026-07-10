package com.animevost.app.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** User-controlled preferences for favorite episode notifications. */
interface NotificationPreferencesRepository {
    val favoriteNotificationsEnabled: Flow<Boolean>
    val mutedFavoriteIds: Flow<Set<Int>>

    suspend fun setFavoriteNotificationsEnabled(enabled: Boolean)

    suspend fun setFavoriteMuted(animeId: Int, muted: Boolean)
}
