package com.animevost.app.core.network.parser

import com.animevost.app.core.domain.model.AnimePreview
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesParser @Inject constructor() {

    fun parse(html: String): List<AnimePreview> {
        val doc = Jsoup.parse(html)
        return doc.select("a[href*=\"/tip/\"]")
            .filter { it.attr("abs:href").contains("animevost.org/tip/") }
            .mapNotNull { element ->
                val href = element.attr("href")
                val id = extractId(href) ?: return@mapNotNull null
                val text = element.text().trim()
                val (title, titleOriginal, episodeInfo) = parseText(text)
                AnimePreview(
                    id = id,
                    title = title,
                    titleOriginal = titleOriginal,
                    posterUrl = "",
                    episodeInfo = episodeInfo,
                    url = href,
                )
            }
            .distinctBy { it.id }
    }

    private fun extractId(url: String): Int? {
        // URL format: /tip/{type}/{id}-{slug}.html
        val segment = url.substringAfterLast("/").substringBefore(".")
        return segment.substringBefore("-").toIntOrNull()
    }

    private fun parseText(text: String): Triple<String, String, String> {
        // Format: "Russian title / Japanese title [episode info]"
        // or: "Title [episode info]"
        val episodeInfo = Regex("\\[([^]]+)]$").find(text)?.groupValues?.get(1) ?: ""
        val withoutEpisode = text.replace(Regex("\\s*\\[[^]]+]$"), "").trim()
        return if (withoutEpisode.contains(" / ")) {
            val parts = withoutEpisode.split(" / ", limit = 2)
            Triple(parts[0].trim(), parts[1].trim(), episodeInfo)
        } else {
            Triple(withoutEpisode, "", episodeInfo)
        }
    }
}
