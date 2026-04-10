package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.AnimeDetail
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.CatalogFilter
import com.animevost.app.core.domain.model.NavData
import com.animevost.app.core.domain.util.Result

/** Provides anime catalogue, detail, search, rating, and navigation data. */
interface AnimeRepository {
    /** Returns a page of [AnimePreview] items matching [filter], or [Result.Error] on failure. */
    suspend fun getAnimeList(page: Int, filter: CatalogFilter): Result<List<AnimePreview>>

    /** Fetches full [AnimeDetail] for the given [url], or [Result.Error] on failure. */
    suspend fun getAnimeDetail(url: String): Result<AnimeDetail>

    /** Searches for anime matching [query] on the given [page]. */
    suspend fun search(query: String, page: Int): Result<List<AnimePreview>>

    /** Submits a [rating] for the anime with [newsId] and returns the updated average rating. */
    suspend fun submitRating(newsId: Int, rating: Int): Result<Double>

    /** Retrieves genres and years for navigation menus. */
    suspend fun getNavData(): Result<NavData>

    /** Fetches a random anime URL from the site. */
    suspend fun getRandomAnimeUrl(): Result<String>
}
