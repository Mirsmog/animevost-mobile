package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.AnimeDetail
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.CatalogFilter
import com.animevost.app.core.domain.model.NavData
import com.animevost.app.core.domain.repository.AnimeRepository
import com.animevost.app.core.domain.util.Result
import com.animevost.app.core.network.AnimeVostApi
import com.animevost.app.core.network.DleEndpoints
import com.animevost.app.core.network.HtmlFetcher
import com.animevost.app.core.network.parser.AnimeDetailParser
import com.animevost.app.core.network.parser.AnimeListParser
import com.animevost.app.core.network.parser.NavigationParser
import com.animevost.app.core.network.parser.SearchParser
import kotlinx.coroutines.CancellationException
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

    override suspend fun getAnimeList(page: Int, filter: CatalogFilter): Result<List<AnimePreview>> {
        return try {
            val path = buildCatalogPath(filter)
            val baseUrl = DleEndpoints.BASE_URL + path
            val html = if (page == 1) {
                // POST on page 1 to set the DLE sort cookie.
                // Main page uses dle_sort_main; category pages (tip/, god/, zhanr/) use dle_sort_cat.
                val isMainPage = filter.genre == null && filter.type == null && filter.year == null && filter.path == null
                val sortCookieKey = if (isMainPage) "dle_sort_main" else "dle_sort_cat"
                val dirCookieKey = if (isMainPage) "dle_direction_main" else "dle_direction_cat"
                val sortParams = mapOf(
                    "dlenewssortby" to filter.sortBy.dleField,
                    "dledirection" to if (filter.sortAscending) "asc" else "desc",
                    "set_new_sort" to sortCookieKey,
                    "set_direction_sort" to dirCookieKey,
                )
                htmlFetcher.fetchPost(baseUrl, sortParams)
            } else {
                // Page 2+ uses GET — DLE cookie (set by page 1 POST) carries the sort
                htmlFetcher.fetch("${baseUrl}page/$page/")
            }
            Result.Success(animeListParser.parse(html).items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun getNavData(): Result<NavData> {
        return try {
            val html = htmlFetcher.fetch(DleEndpoints.BASE_URL)
            Result.Success(navigationParser.parse(html))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun getAnimeDetail(url: String): Result<AnimeDetail> {
        return try {
            val fullUrl = when {
                url.startsWith("http") -> url
                url.startsWith("/") -> DleEndpoints.BASE_URL.trimEnd('/') + url
                else -> DleEndpoints.BASE_URL + url
            }
            val html = htmlFetcher.fetch(fullUrl)
            Result.Success(animeDetailParser.parse(html, fullUrl))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun search(query: String, page: Int): Result<List<AnimePreview>> {
        return try {
            val url = DleEndpoints.BASE_URL + DleEndpoints.SEARCH
            val params = buildMap {
                put("subaction", "search")
                put("story", query)
                if (page > 1) {
                    put("result_from", ((page - 1) * 10 + 1).toString())
                }
            }
            val html = htmlFetcher.fetchPost(url, params)
            Result.Success(searchParser.parse(html).items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun submitRating(newsId: Int, rating: Int): Result<Double> {
        return try {
            val response = api.submitRating(rating, newsId)
            Result.Success(response.get("rating")?.asDouble ?: 0.0)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    private fun buildCatalogPath(filter: CatalogFilter): String {
        filter.path?.let { return it }
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
}
