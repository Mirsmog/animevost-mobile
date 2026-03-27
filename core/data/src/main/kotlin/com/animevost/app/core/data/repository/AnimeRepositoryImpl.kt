package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.AnimeDetail
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.CatalogFilter
import com.animevost.app.core.domain.repository.AnimeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeRepositoryImpl @Inject constructor() : AnimeRepository {

    override suspend fun getAnimeList(page: Int, filter: CatalogFilter): List<AnimePreview> {
        TODO("Implement: fetch HTML, parse with Jsoup")
    }

    override suspend fun getAnimeDetail(url: String): AnimeDetail {
        TODO("Implement: fetch anime detail page, parse with Jsoup")
    }

    override suspend fun search(query: String, page: Int): List<AnimePreview> {
        TODO("Implement: POST search form, parse results")
    }
}
