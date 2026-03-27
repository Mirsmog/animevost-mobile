package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.AnimeDetail
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.CatalogFilter
import com.animevost.app.core.domain.model.NavData

interface AnimeRepository {
    suspend fun getAnimeList(page: Int, filter: CatalogFilter): List<AnimePreview>
    suspend fun getAnimeDetail(url: String): AnimeDetail
    suspend fun search(query: String, page: Int): List<AnimePreview>
    suspend fun submitRating(newsId: Int, rating: Int): Double
    suspend fun getNavData(): NavData
}
