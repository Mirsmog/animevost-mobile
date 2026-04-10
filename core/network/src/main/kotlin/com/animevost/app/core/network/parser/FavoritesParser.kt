package com.animevost.app.core.network.parser

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.network.EndpointResolver
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesParser @Inject constructor(private val resolver: EndpointResolver) {

    fun parse(html: String): List<AnimePreview> {
        val doc = Jsoup.parse(html, resolver.currentBaseUrl)
        // Each user favorite is wrapped in <div class="shortstory"> which contains
        // <a class="shortstoryShare" id="fav-id-{id}"> — use this as the authoritative source
        // to avoid picking up unrelated links (nav menu, "recent" sections, etc.)
        return doc.select("div.shortstory")
            .mapNotNull { block ->
                val shareLink = block.selectFirst("a.shortstoryShare") ?: return@mapNotNull null
                val id = shareLink.id().removePrefix("fav-id-").toIntOrNull()
                    ?: return@mapNotNull null
                val titleLink = block.selectFirst("h2 a") ?: return@mapNotNull null
                val href = titleLink.absUrl("href").ifEmpty { titleLink.attr("href") }
                val text = titleLink.text().trim()
                val (title, titleOriginal, episodeInfo) = parseText(text)
                val posterImg = block.selectFirst("img.imgRadius")
                val posterUrl = posterImg?.absUrl("src")?.ifEmpty { posterImg.attr("src") } ?: ""
                AnimePreview(
                    id = id,
                    title = title,
                    titleOriginal = titleOriginal,
                    posterUrl = posterUrl,
                    episodeInfo = episodeInfo,
                    url = href,
                )
            }
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
