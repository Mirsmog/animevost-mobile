package com.animevost.app.core.network.parser

import com.animevost.app.core.domain.model.AnimeDetail
import com.animevost.app.core.domain.model.AnimeType
import com.animevost.app.core.domain.model.Genre
import com.animevost.app.core.domain.model.RelatedSeries
import com.animevost.app.core.network.DleEndpoints
import com.animevost.app.core.network.EndpointResolver
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import javax.inject.Inject

class AnimeDetailParser @Inject constructor(
    private val episodeParser: EpisodeParser,
    private val resolver: EndpointResolver,
) {

    companion object {
        // CSS selectors
        private const val SHORT_STORY = "div.shortstory"
        private const val STORY_HEAD_H1 = ".shortstoryHead h1"
        private const val STORY_CONTENT = ".shortstoryContent"
        private const val DESCRIPTION_SPAN = "span[itemprop=description]"
        private const val CURRENT_RATING = ".current-rating"
        private const val VOTE_NUM_SPAN = "span[id^=vote-num-id-]"
        private const val VOTE_NUM_ID_PREFIX = "vote-num-id-"
        private const val STATIC_INFO = ".staticInfo"
        private const val STATIC_INFO_DATE = ".staticInfoLeftData"
        private const val STATIC_INFO_VIEWS = ".staticInfoRightSmotr"
        private const val DLE_COMMENT_LINK = "#dle-comm-link"
        private const val CANONICAL_LINK = "link[rel=canonical]"
        private const val STORY_FOOTER = ".shortstoryFuter"
        private const val TITLE_SPOILER = "div.title_spoiler"
        private const val TEXT_SPOILER_CLASS = "text_spoiler"
    }

    fun parse(html: String, url: String = ""): AnimeDetail {
        val doc = Jsoup.parse(html, resolver.currentBaseUrl)
        val story = doc.selectFirst(SHORT_STORY) ?: doc

        val rawTitle = story.selectFirst(STORY_HEAD_H1)?.text()?.trim().orEmpty()
        val (title, titleOriginal, _) = AnimeListParser.splitTitle(rawTitle)

        val content = story.selectFirst(STORY_CONTENT) ?: story

        val posterUrl = content.selectFirst("img")?.let { img ->
            img.absUrl("src").ifEmpty { AnimeListParser.resolveUrl(img.attr("src"), resolver.currentBaseUrl) }
        }.orEmpty()

        val year = extractField(content, "Год выхода")
        val typeText = extractField(content, "Тип")
        val episodeCount = extractField(content, "Количество серий")
        val director = extractField(content, "Режиссёр")
            .ifEmpty { extractField(content, "Режиссер") }
        val description = content.selectFirst(DESCRIPTION_SPAN)?.text()?.trim()
            ?: extractField(content, "Описание")

        val genres = parseGenres(content)

        val type = AnimeType.entries.find { it.displayName.equals(typeText, ignoreCase = true) }
            ?: AnimeType.UNKNOWN

        // Rating: <li class="current-rating" style="width:80%;">80</li> → 80% → 4.0/5
        val ratingPercent = content.selectFirst(CURRENT_RATING)?.let { el ->
            el.text().toDoubleOrNull()
                ?: el.attr("style").let { style ->
                    Regex("""width:\s*(\d+)%""").find(style)?.groupValues?.get(1)?.toDoubleOrNull()
                }
        } ?: 0.0
        val rating = ratingPercent / 20.0

        // Vote count: <span id="vote-num-id-3774">1084</span>
        val voteCount = doc.select(VOTE_NUM_SPAN).firstOrNull()
            ?.text()?.replace(Regex("[^\\d]"), "")?.toIntOrNull() ?: 0

        // Static info bar
        val staticInfo = story.selectFirst(STATIC_INFO)
        val publishDate = staticInfo?.selectFirst(STATIC_INFO_DATE)?.text()?.trim().orEmpty()
        val viewCount = staticInfo?.selectFirst(STATIC_INFO_VIEWS)?.text()
            ?.replace(Regex("[^\\d]"), "")?.toIntOrNull() ?: 0
        val commentCount = staticInfo?.selectFirst(DLE_COMMENT_LINK)?.text()
            ?.replace(Regex("[^\\d]"), "")?.toIntOrNull() ?: 0

        // Anime id from vote span or canonical URL
        val id = doc.select(VOTE_NUM_SPAN).firstOrNull()
            ?.attr("id")?.removePrefix(VOTE_NUM_ID_PREFIX)?.toIntOrNull()
            ?: extractIdFromDocument(doc)
            ?: 0

        // Footer categories
        val categories = story.selectFirst(STORY_FOOTER)
            ?.select("a")?.map { it.text().trim() }
            .orEmpty()

        // Total comment pages: var total_comments_pages= '100';
        val totalCommentPages = Regex("""total_comments_pages=\s*'(\d+)'""")
            .find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 1

        val episodes = episodeParser.parse(html)

        return AnimeDetail(
            id = id,
            url = url,
            title = title,
            titleOriginal = titleOriginal,
            posterUrl = posterUrl,
            year = year,
            genres = genres,
            type = type,
            episodeCount = episodeCount,
            director = director,
            rating = rating,
            voteCount = voteCount,
            viewCount = viewCount,
            commentCount = commentCount,
            totalCommentPages = totalCommentPages,
            description = description,
            publishDate = publishDate,
            categories = categories,
            relatedAnime = emptyList(),
            relatedSeries = parseRelatedSeries(story),
            episodes = episodes,
        )
    }

    private fun parseGenres(content: Element): List<Genre> {
        for (p in content.select("p")) {
            val strong = p.selectFirst("strong") ?: continue
            if ("Жанр" !in strong.text()) continue

            val links = p.select("a")
            if (links.isNotEmpty()) {
                return links.map { a ->
                    Genre(
                        id = 0,
                        name = a.text().trim(),
                        url = a.absUrl("href").ifEmpty { AnimeListParser.resolveUrl(a.attr("href"), resolver.currentBaseUrl) },
                    )
                }
            }
            // Plain text genres separated by commas
            val text = p.text().substringAfter(strong.text()).trim()
            return text.split(",")
                .map { Genre(id = 0, name = it.trim(), url = "") }
                .filter { it.name.isNotEmpty() }
        }
        return emptyList()
    }

    private fun extractField(content: Element, label: String): String {
        for (p in content.select("p")) {
            val strong = p.selectFirst("strong") ?: continue
            if (label in strong.text()) {
                return p.text().substringAfter(strong.text()).trim()
            }
        }
        for (div in content.select("div")) {
            val strong = div.selectFirst("strong") ?: continue
            if (label in strong.text()) {
                return div.text().substringAfter(strong.text()).trim()
            }
        }
        return ""
    }

    private fun parseRelatedSeries(root: Element): List<RelatedSeries> {
        for (titleDiv in root.select(TITLE_SPOILER)) {
            if ("состоит" !in titleDiv.text()) continue
            val spoilerDiv = titleDiv.nextElementSibling() ?: continue
            if (!spoilerDiv.hasClass(TEXT_SPOILER_CLASS)) continue
            return spoilerDiv.select("li").mapNotNull { li ->
                val link = li.selectFirst("a") ?: return@mapNotNull null
                val title = link.attr("title").ifEmpty { link.text() }.trim()
                val url = link.absUrl("href").ifEmpty { link.attr("href") }
                val description = li.text().substringAfter(link.text()).trim().trimStart('-', ' ')
                RelatedSeries(title = title, url = url, description = description)
            }
        }
        return emptyList()
    }

    private fun extractIdFromDocument(doc: Document): Int? {
        val canonical = doc.selectFirst(CANONICAL_LINK)?.attr("href").orEmpty()
        return AnimeListParser.extractIdFromUrl(canonical)
    }
}
