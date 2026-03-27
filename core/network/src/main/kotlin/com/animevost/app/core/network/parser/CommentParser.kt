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

        return doc.select("[id^=comment-], [id^=comm-id-], .comment, .comm-item")
            .mapNotNull { el ->
                val id = el.attr("id")
                    .removePrefix("comment-")
                    .removePrefix("comm-id-")
                    .toIntOrNull() ?: 0

                val author = el.selectFirst(".comm-author a, .comment_author a, .nickname a")
                    ?.text()?.trim()
                    ?: el.selectFirst(".comm-author, .comment_author, .nickname")
                        ?.text()?.trim()
                    .orEmpty()

                val date = el.selectFirst(".comm-date, .comment_date, .date")
                    ?.text()?.trim().orEmpty()

                val text = el.selectFirst(".comm-body, .comment_text, .text, .comm_body")
                    ?.text()?.trim().orEmpty()

                val avatar = el.selectFirst(".comm-avatar img, .comment_avatar img, .avatar img")
                    ?.let { img ->
                        img.absUrl("src").ifEmpty { AnimeListParser.resolveUrl(img.attr("src")) }
                    }.orEmpty()

                if (author.isEmpty() && text.isEmpty()) return@mapNotNull null

                Comment(id = id, author = author, date = date, text = text, avatar = avatar)
            }
    }
}
