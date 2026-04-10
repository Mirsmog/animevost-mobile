package com.animevost.app.core.network.parser

import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.network.EndpointResolver
import com.google.gson.JsonObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import javax.inject.Inject

class CommentParser @Inject constructor(private val resolver: EndpointResolver) {

    fun parse(response: JsonObject): List<Comment> {
        val commentsHtml = response.get("comments")?.asString ?: return emptyList()
        return parseCommentsHtml(commentsHtml)
    }

    fun parseCommentsHtml(html: String): List<Comment> {
        val doc = Jsoup.parse(html, resolver.currentBaseUrl)

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

                val avatar = el.selectFirst(".commentFinalAva img")
                    ?.let { img ->
                        img.absUrl("src").ifEmpty { AnimeListParser.resolveUrl(img.attr("src"), resolver.currentBaseUrl) }
                    }.orEmpty()

                val (quotedAuthor, quotedText, commentText) = parseTextContent(el)

                if (author.isEmpty() && commentText.isEmpty()) return@mapNotNull null

                Comment(
                    id = id,
                    author = author,
                    date = date,
                    text = commentText,
                    avatar = avatar,
                    quotedAuthor = quotedAuthor,
                    quotedText = quotedText,
                )
            }
    }

    /**
     * Parses the text content element extracting:
     * - quotedAuthor: who was cited (from div.titlequote "Цитата: Username")
     * - quotedText: the cited text (from div.quote)
     * - remaining comment text after stripping the quote block
     *
     * DLE quote HTML:
     * <!--QuoteBegin USERNAME-->
     * <div class="titlequote">Цитата: USERNAME</div>
     * <div class="quote"><!--QuoteEBegin-->QUOTED TEXT<!--QuoteEnd--></div>
     * <!--QuoteEEnd-->
     * ACTUAL REPLY TEXT
     */
    private fun parseTextContent(el: Element): Triple<String, String, String> {
        val textEl = el.selectFirst("[id^=comm-id-]")
            ?: el.selectFirst(".commentFinalText")
            ?: return Triple("", "", "")

        // Extract quote block
        val titleQuote = textEl.selectFirst(".titlequote")
        val quotedEl = textEl.selectFirst(".quote")

        val quotedAuthor = titleQuote?.text()
            ?.replace("Цитата:", "")
            ?.replace("Quote:", "")
            ?.trim().orEmpty()

        val quotedText = quotedEl?.text()?.trim().orEmpty()

        // Remove quote divs then get remaining text
        titleQuote?.remove()
        quotedEl?.remove()

        // Remove quote divs then get remaining HTML (preserving formatting tags)
        val rawText = textEl.html().trim()
            .replace(Regex("<!--QuoteE?Begin.*?-->", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<!--Quote.*?-->", RegexOption.DOT_MATCHES_ALL), "")
            .trim()

        return Triple(quotedAuthor, quotedText, rawText)
    }
}
