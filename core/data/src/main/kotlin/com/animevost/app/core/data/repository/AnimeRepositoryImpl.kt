package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.AnimeDetail
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.CatalogFilter
import com.animevost.app.core.domain.repository.AnimeRepository
import com.animevost.app.core.network.DleEndpoints
import com.animevost.app.core.network.HtmlFetcher
import com.animevost.app.core.network.parser.AnimeDetailParser
import com.animevost.app.core.network.parser.AnimeListParser
import com.animevost.app.core.network.parser.SearchParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeRepositoryImpl @Inject constructor(
    private val htmlFetcher: HtmlFetcher,
    private val animeListParser: AnimeListParser,
    private val animeDetailParser: AnimeDetailParser,
    private val searchParser: SearchParser,
) : AnimeRepository {

    override suspend fun getAnimeList(page: Int, filter: CatalogFilter): List<AnimePreview> {
        val path = buildCatalogPath(filter)
        val paginatedPath = if (page > 1) "${path}page/$page/" else path
        val url = DleEndpoints.BASE_URL + paginatedPath
        val html = htmlFetcher.fetch(url)
        return animeListParser.parse(html).items
    }

    override suspend fun getAnimeDetail(url: String): AnimeDetail {
        val fullUrl = when {
            url.startsWith("http") -> url
            url.startsWith("/") -> DleEndpoints.BASE_URL.trimEnd('/') + url
            else -> DleEndpoints.BASE_URL + url
        }
        val html = htmlFetcher.fetch(fullUrl)
        return animeDetailParser.parse(html)
    }

    override suspend fun search(query: String, page: Int): List<AnimePreview> {
        val url = DleEndpoints.BASE_URL + DleEndpoints.SEARCH
        val params = buildMap {
            put("do", "search")
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
        filter.type?.let { return "tip/${it.name.lowercase()}/" }
        filter.year?.let { return "god/$it/" }
        return DleEndpoints.MAIN_PAGE
    }
}
