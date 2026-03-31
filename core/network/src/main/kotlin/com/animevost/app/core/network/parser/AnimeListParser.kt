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
        val items = doc.select(SHORT_STORY).mapNotNull { block -> parseBlock(block) }

        val currentPage = doc.select(ACTIVE_PAGE_SELECTOR)
            .firstOrNull()?.text()?.toIntOrNull() ?: 1

        val paginationPages = doc.select(PAGINATION_LINK_SELECTOR)
            .mapNotNull { it.text().toIntOrNull() }

        val totalPages = paginationPages.maxOrNull() ?: currentPage

        return AnimeListResult(
            items = items,
            currentPage = currentPage,
            totalPages = maxOf(totalPages, currentPage),
        )
    }

    fun parseItems(html: String): List<AnimePreview> = parse(html).items

    private fun parseBlock(block: Element): AnimePreview? {
        val headLink = block.selectFirst(STORY_HEAD_H2_LINK)
            ?: block.selectFirst(STORY_HEAD_H1_LINK)
            ?: return null

        val fullUrl = headLink.absUrl("href").ifEmpty { return null }
        val id = extractIdFromUrl(fullUrl) ?: return null
        val rawTitle = headLink.text().trim()

        val (title, titleOriginal, episodeInfo) = splitTitle(rawTitle)

        val img = block.selectFirst(STORY_CONTENT_IMG)
        val posterUrl = img?.let {
            it.absUrl("src").ifEmpty { resolveUrl(it.attr("src")) }
        }.orEmpty()

        // Best-effort extraction of rating / views / comments from shortstory block
        val rating = parseRating(block)
        val viewCount = parseIntFromSelectors(block,
            STATIC_INFO_VIEWS, "span.views-num", ".views-count")
        val commentCount = parseIntFromSelectors(block,
            DLE_COMMENT_LINK, COMS_NUM, COMMENTS_NUM, COMM_NUM)

        return AnimePreview(
            id = id,
            title = title,
            titleOriginal = titleOriginal,
            posterUrl = posterUrl,
            episodeInfo = episodeInfo,
            url = fullUrl,
            type = extractTypeFromUrl(fullUrl),
            rating = rating,
            viewCount = viewCount,
            commentCount = commentCount,
        )
    }

    private fun parseRating(block: Element): Double {
        val el = block.selectFirst(CURRENT_RATING)
            ?: block.selectFirst(RATING)
            ?: return 0.0
        // Try extracting from style="width:XX%"
        val fromStyle = RATING_WIDTH.find(el.attr("style"))
            ?.groupValues?.get(1)?.toDoubleOrNull()
        if (fromStyle != null) return fromStyle / 20.0 // percent → 5-star scale
        // Fallback: text might contain raw value
        val text = el.text().trim().toDoubleOrNull() ?: return 0.0
        return if (text > 5.0) text / 20.0 else text
    }

    private fun parseIntFromSelectors(block: Element, vararg selectors: String): Int {
        for (sel in selectors) {
            val text = block.selectFirst(sel)?.text() ?: continue
            val num = DIGITS_ONLY.replace(text, "").toIntOrNull()
            if (num != null) return num
        }
        return 0
    }

    companion object {
        // CSS selectors
        private const val SHORT_STORY = "div.shortstory"
        private const val STORY_HEAD_H2_LINK = ".shortstoryHead h2 a"
        private const val STORY_HEAD_H1_LINK = ".shortstoryHead h1 a"
        private const val STORY_CONTENT_IMG = ".shortstoryContent img"
        private const val ACTIVE_PAGE_SELECTOR =
            ".pnext span.active, .dle-pagination span.active, .navigation span.dle_active"
        private const val PAGINATION_LINK_SELECTOR = ".pnext a, .dle-pagination a, .navigation a"
        private const val CURRENT_RATING = ".current-rating"
        private const val RATING = ".rating"
        private const val STATIC_INFO_VIEWS = ".staticInfoRightSmotr"
        private const val DLE_COMMENT_LINK = "#dle-comm-link"
        private const val COMS_NUM = "a.coms-num"
        private const val COMMENTS_NUM = ".comments-num"
        private const val COMM_NUM = "span.comm-num"

        // Regex patterns
        private val ID_FROM_URL = Regex("""/(\d+)-[^/]+\.html""")
        private val EPISODE_BRACKET = Regex("""\[([^\]]+)]\s*$""")
        private val TYPE_FROM_URL = Regex("""/tip/([^/]+)/""")
        private val RATING_WIDTH = Regex("""width:\s*(\d+)%""")
        private val DIGITS_ONLY = Regex("""[^\d]""")

        private val TYPE_SLUG_MAP = mapOf(
            "tv"                     to "ТВ",
            "tv-speshl"              to "ТВ-спэшл",
            "ova"                    to "OVA",
            "ona"                    to "ONA",
            "polnometrazhnyy-film"   to "Фильм",
            "korotkometrazhnyy-film" to "К/М фильм",
            "dunkhua"                to "Дунхуа",
        )

        fun extractIdFromUrl(url: String): Int? =
            ID_FROM_URL.find(url)?.groupValues?.get(1)?.toIntOrNull()

        fun extractTypeFromUrl(url: String): String =
            TYPE_FROM_URL.find(url)?.groupValues?.get(1)?.lowercase()
                ?.let { TYPE_SLUG_MAP[it] } ?: ""

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
