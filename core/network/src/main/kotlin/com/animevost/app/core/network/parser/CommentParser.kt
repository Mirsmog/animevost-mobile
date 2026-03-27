package com.animevost.app.core.network.parser

import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.network.DleEndpoints
import com.google.gson.JsonObject
import org.jsoup.Jsoup
import javax.inject.Inject

class CommentParser @Inject constructor() {

    fun parse(response: JsonObject): List<Comment> {
        val commentsHtml = response.get("comments")?.asString ?: return emptyList()
        return parseCommentsHtml(commentsHtml)
    }

    fun parseCommentsHtml(html: String): List<Comment> {
        val doc = Jsoup.parse(html, DleEndpoints.BASE_URL)

        return doc.select("[id^=comment-id-]")
            .mapNotNull { el ->
                val id = el.attr("id")
                    .removePrefix("comment-id-")
                    .toIntOrNull() ?: 0

                val author = el.selectFirst(".commentFinalAva strong a")
                    ?.text()?.trim()
                    ?: el.selectFirst(".commentFinalAva a")
                        ?.text()?.trim()
                    .orEmpty()

                val date = el.selectFirst(".commentFinalData")
                    ?.ownText()?.trim().orEmpty()

                val text = el.selectFirst("[id^=comm-id-]")
                    ?.text()?.trim()
                    ?: el.selectFirst(".commentFinalText")
                        ?.text()?.trim()
                    .orEmpty()

                val avatar = el.selectFirst(".commentFinalAva img")
                    ?.let { img ->
                        img.absUrl("src").ifEmpty { AnimeListParser.resolveUrl(img.attr("src")) }
                    }.orEmpty()

                if (author.isEmpty() && text.isEmpty()) return@mapNotNull null

                Comment(id = id, author = author, date = date, text = text, avatar = avatar)
            }
    }
}
