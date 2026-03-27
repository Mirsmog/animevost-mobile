package com.animevost.app.core.network.parser

import com.animevost.app.core.domain.model.AnimeListResult
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.network.DleEndpoints
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import javax.inject.Inject

class AnimeListParser @Inject constructor() {

    fun parse(html: String): AnimeListResult {
        val doc = Jsoup.parse(html, DleEndpoints.BASE_URL)
        val items = doc.select("div.shortstory").mapNotNull { block -> parseBlock(block) }

        val currentPage = doc.select(
            ".pnext span.active, .dle-pagination span.active, .navigation span.dle_active"
        ).firstOrNull()?.text()?.toIntOrNull() ?: 1

        val paginationPages = doc.select(
            ".pnext a, .dle-pagination a, .navigation a"
        ).mapNotNull { it.text().toIntOrNull() }

        val totalPages = paginationPages.maxOrNull() ?: currentPage

        return AnimeListResult(
            items = items,
            currentPage = currentPage,
            totalPages = maxOf(totalPages, currentPage),
        )
    }

    fun parseItems(html: String): List<AnimePreview> = parse(html).items

    private fun parseBlock(block: Element): AnimePreview? {
        val headLink = block.selectFirst(".shortstoryHead h2 a")
            ?: block.selectFirst(".shortstoryHead h1 a")
            ?: return null

        val fullUrl = headLink.absUrl("href").ifEmpty { return null }
        val id = extractIdFromUrl(fullUrl) ?: return null
        val rawTitle = headLink.text().trim()

        val (title, titleOriginal, episodeInfo) = splitTitle(rawTitle)

        val img = block.selectFirst(".shortstoryContent img")
        val posterUrl = img?.let {
            it.absUrl("src").ifEmpty { resolveUrl(it.attr("src")) }
        }.orEmpty()

        return AnimePreview(
            id = id,
            title = title,
            titleOriginal = titleOriginal,
            posterUrl = posterUrl,
            episodeInfo = episodeInfo,
            url = fullUrl,
        )
    }

    companion object {
        private val ID_FROM_URL = Regex("""/(\d+)-[^/]+\.html""")
        private val EPISODE_BRACKET = Regex("""\[([^\]]+)]\s*$""")

        fun extractIdFromUrl(url: String): Int? =
            ID_FROM_URL.find(url)?.groupValues?.get(1)?.toIntOrNull()

        fun splitTitle(raw: String): Triple<String, String, String> {
            val episodeMatch = EPISODE_BRACKET.find(raw)
            val episodeInfo = episodeMatch?.groupValues?.get(1).orEmpty()
            val withoutEpisodes = raw.replace(EPISODE_BRACKET, "").trim()

            val parts = withoutEpisodes.split(" / ", limit = 2)
            val title = parts[0].trim()
            val titleOriginal = if (parts.size > 1) parts[1].trim() else ""

            return Triple(title, titleOriginal, episodeInfo)
        }

        fun resolveUrl(path: String): String =
            if (path.startsWith("/")) "${DleEndpoints.BASE_URL.trimEnd('/')}$path" else path
    }
}
