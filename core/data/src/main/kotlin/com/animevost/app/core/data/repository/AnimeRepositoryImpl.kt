package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.AnimeDetail
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.CatalogFilter
import com.animevost.app.core.domain.model.NavData
import com.animevost.app.core.domain.repository.AnimeRepository
import com.animevost.app.core.network.AnimeVostApi
import com.animevost.app.core.network.DleEndpoints
import com.animevost.app.core.network.HtmlFetcher
import com.animevost.app.core.network.parser.AnimeDetailParser
import com.animevost.app.core.network.parser.AnimeListParser
import com.animevost.app.core.network.parser.NavigationParser
import com.animevost.app.core.network.parser.SearchParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeRepositoryImpl @Inject constructor(
    private val htmlFetcher: HtmlFetcher,
    private val animeListParser: AnimeListParser,
    private val animeDetailParser: AnimeDetailParser,
    private val searchParser: SearchParser,
    private val navigationParser: NavigationParser,
    private val api: AnimeVostApi,
) : AnimeRepository {

    override suspend fun getAnimeList(page: Int, filter: CatalogFilter): List<AnimePreview> {
        val path = buildCatalogPath(filter)
        val baseUrl = DleEndpoints.BASE_URL + path
        val html = if (page == 1) {
            // Always POST on page 1 to explicitly set the sort cookie in DLE
            val sortParams = mapOf(
                "dlenewssortby" to filter.sortBy.dleField,
                "dledirection" to if (filter.sortAscending) "asc" else "desc",
                "set_new_sort" to "dle_sort_main",
                "set_direction_sort" to "dle_direction_main",
            )
            htmlFetcher.fetchPost(baseUrl, sortParams)
        } else {
            // Page 2+ uses GET — DLE cookie (set by page 1 POST) carries the sort
            htmlFetcher.fetch("${baseUrl}page/$page/")
        }
        return animeListParser.parse(html).items
    }

    override suspend fun getNavData(): NavData {
        val html = htmlFetcher.fetch(DleEndpoints.BASE_URL)
        return navigationParser.parse(html)
    }

    override suspend fun getAnimeDetail(url: String): AnimeDetail {
        val fullUrl = when {
            url.startsWith("http") -> url
            url.startsWith("/") -> DleEndpoints.BASE_URL.trimEnd('/') + url
            else -> DleEndpoints.BASE_URL + url
        }
        val html = htmlFetcher.fetch(fullUrl)
        return animeDetailParser.parse(html, fullUrl)
    }

    override suspend fun search(query: String, page: Int): List<AnimePreview> {
        val url = DleEndpoints.BASE_URL + DleEndpoints.SEARCH
        val params = buildMap {
            put("subaction", "search")
            put("story", query)
            if (page > 1) {
                put("result_from", ((page - 1) * 10 + 1).toString())
            }
        }
        val html = htmlFetcher.fetchPost(url, params)
        return searchParser.parse(html).items
    }

    private fun buildCatalogPath(filter: CatalogFilter): String {
        filter.genre?.let { genre ->
            val genreUrl = genre.url
            return if (genreUrl.startsWith(DleEndpoints.BASE_URL)) {
                genreUrl.removePrefix(DleEndpoints.BASE_URL)
            } else {
                genreUrl.trimStart('/')
            }
        }
        filter.type?.let { type ->
            if (type.urlSlug.isNotEmpty()) return "tip/${type.urlSlug}/"
        }
        filter.year?.let { return "god/$it/" }
        return DleEndpoints.MAIN_PAGE
    }

    override suspend fun submitRating(newsId: Int, rating: Int): Double {
        val response = api.submitRating(rating, newsId)
        return response.get("rating")?.asDouble ?: 0.0
    }
}
