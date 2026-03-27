package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.AnimePreview

interface FavoriteRepository {
    suspend fun getFavorites(page: Int): List<AnimePreview>
    suspend fun toggleFavorite(newsId: Int, preview: AnimePreview? = null): Boolean
}
