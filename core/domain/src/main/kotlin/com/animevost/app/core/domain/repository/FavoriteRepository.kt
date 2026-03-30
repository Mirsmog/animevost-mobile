package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.AnimePreview
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun getAllFavorites(): Flow<List<AnimePreview>>
    suspend fun getFavorites(page: Int): List<AnimePreview>
    suspend fun isFavorite(newsId: Int): Boolean
    fun isFavoriteFlow(newsId: Int): Flow<Boolean>
    suspend fun toggleFavorite(newsId: Int, preview: AnimePreview? = null): Boolean
}
