package com.animevost.app.core.network.parser

import com.animevost.app.core.domain.model.AnimeDetail
import com.animevost.app.core.domain.model.AnimeType
import com.animevost.app.core.domain.model.Genre
import com.animevost.app.core.network.DleEndpoints
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import javax.inject.Inject

class AnimeDetailParser @Inject constructor(
    private val episodeParser: EpisodeParser,
) {

    fun parse(html: String): AnimeDetail {
        val doc = Jsoup.parse(html, DleEndpoints.BASE_URL)
        val story = doc.selectFirst("div.shortstory") ?: doc

        val rawTitle = story.selectFirst(".shortstoryHead h1")?.text()?.trim().orEmpty()
        val (title, titleOriginal, _) = AnimeListParser.splitTitle(rawTitle)

        val content = story.selectFirst(".shortstoryContent") ?: story

        val posterUrl = content.selectFirst("img")?.let { img ->
            img.absUrl("src").ifEmpty { AnimeListParser.resolveUrl(img.attr("src")) }
        }.orEmpty()

        val year = extractField(content, "Год выхода")
        val typeText = extractField(content, "Тип")
        val episodeCount = extractField(content, "Количество серий")
        val director = extractField(content, "Режиссёр")
            .ifEmpty { extractField(content, "Режиссер") }
        val description = content.selectFirst("span[itemprop=description]")?.text()?.trim()
            ?: extractField(content, "Описание")

        val genres = parseGenres(content)

        val type = AnimeType.entries.find { it.displayName.equals(typeText, ignoreCase = true) }
            ?: AnimeType.UNKNOWN

        // Rating: <li class="current-rating" style="width:80%;">80</li> → 80% → 4.0/5
        val ratingPercent = content.selectFirst(".current-rating")?.let { el ->
            el.text().toDoubleOrNull()
                ?: el.attr("style").let { style ->
                    Regex("""width:\s*(\d+)%""").find(style)?.groupValues?.get(1)?.toDoubleOrNull()
                }
        } ?: 0.0
        val rating = ratingPercent / 20.0

        // Vote count: <span id="vote-num-id-3774">1084</span>
        val voteCount = doc.select("span[id^=vote-num-id-]").firstOrNull()
            ?.text()?.replace(Regex("[^\\d]"), "")?.toIntOrNull() ?: 0

        // Static info bar
        val staticInfo = story.selectFirst(".staticInfo")
        val publishDate = staticInfo?.selectFirst(".staticInfoLeftData")?.text()?.trim().orEmpty()
        val viewCount = staticInfo?.selectFirst(".staticInfoRightSmotr")?.text()
            ?.replace(Regex("[^\\d]"), "")?.toIntOrNull() ?: 0
        val commentCount = staticInfo?.selectFirst("#dle-comm-link")?.text()
            ?.replace(Regex("[^\\d]"), "")?.toIntOrNull() ?: 0

        // Anime id from vote span or canonical URL
        val id = doc.select("span[id^=vote-num-id-]").firstOrNull()
            ?.attr("id")?.removePrefix("vote-num-id-")?.toIntOrNull()
            ?: extractIdFromDocument(doc)
            ?: 0

        // Footer categories
        val categories = story.selectFirst(".shortstoryFuter")
            ?.select("a")?.map { it.text().trim() }
            .orEmpty()

        val episodes = episodeParser.parse(html)

        return AnimeDetail(
            id = id,
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
            description = description,
            publishDate = publishDate,
            categories = categories,
            relatedAnime = emptyList(),
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
                        url = a.absUrl("href").ifEmpty { AnimeListParser.resolveUrl(a.attr("href")) },
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

    private fun extractIdFromDocument(doc: Document): Int? {
        val canonical = doc.selectFirst("link[rel=canonical]")?.attr("href").orEmpty()
        return AnimeListParser.extractIdFromUrl(canonical)
    }
}
