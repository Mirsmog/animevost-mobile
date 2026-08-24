package com.animevost.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

private const val EMOJI_BASE = "https://animevost.org/engine/data/emoticons/"
private const val EMOJI_PLACEHOLDER_START = '\uE000'
private const val EMOJI_PLACEHOLDER_END = '\uE001'
private val RAW_EMOJI_TOKEN_REGEX = Regex("""(?<!\d):(\d{1,3}):""")
private val IMG_TAG_REGEX = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
private val IMG_CLASS_REGEX = Regex("""\bclass\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
private val IMG_ALT_ID_REGEX = Regex("""\balt\s*=\s*["'](\d{1,3})["']""", RegexOption.IGNORE_CASE)
private val IMG_EMOJI_SRC_ID_REGEX = Regex("""/emoticons/(\d{1,3})\.gif""", RegexOption.IGNORE_CASE)
private val DLE_SPOILER_REGEX = Regex(
    """(?:<!--dle_?spoiler.*?-->\s*)?<div[^>]*class\s*=\s*["'][^"']*\btitle_?spoiler\b[^"']*["'][^>]*>(.*?)</div>\s*<div[^>]*class\s*=\s*["'][^"']*\btext_?spoiler\b[^"']*["'][^>]*>(.*?)</div>(?:\s*<!--/dle_?spoiler-->)?""",
    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
)
private val TITLE_SPOILER_REGEX = Regex(
    """<div[^>]*class\s*=\s*["'][^"']*\btitle_?spoiler\b[^"']*["'][^>]*>.*?</div>""",
    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
)
private val TEXT_SPOILER_REGEX = Regex(
    """<div[^>]*class\s*=\s*["'][^"']*\btext_?spoiler\b[^"']*["'][^>]*>(.*?)</div>""",
    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
)
private val BBCODE_SPOILER_REGEX = Regex(
    """\[spoiler(?:=([^\]]+))?](.*?)\[/spoiler]""",
    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
)

/** Sealed hierarchy representing parsed comment segments */
sealed class CommentSegment {
    data class TextSegment(
        val text: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val strikethrough: Boolean = false,
    ) : CommentSegment()
    data class EmojiSegment(val id: Int) : CommentSegment()
    data class SpoilerSegment(val title: String, val html: String) : CommentSegment()
    object LineBreak : CommentSegment()
}

private data class SpoilerContent(val title: String, val html: String)

/** Parse raw HTML (from server) into a list of CommentSegments. Regex-only, no Jsoup. */
fun parseCommentHtml(html: String): List<CommentSegment> {
    val segments = mutableListOf<CommentSegment>()

    // Normalize <br> and <br/> to newlines
    var text = html
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")

    // Replace emoji markers: <!--smile:N-->...<img ...>...<!--/smile--> → placeholder
    text = text.replace(
        Regex("""<!--smile:(\d+)-->.*?<!--/smile-->""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    ) { mr -> "$EMOJI_PLACEHOLDER_START${mr.groupValues[1]}$EMOJI_PLACEHOLDER_END" }

    text = text.replace(RAW_EMOJI_TOKEN_REGEX) { mr ->
        val id = mr.groupValues[1].toIntOrNull()
        if (id != null && id.isAnimeVostEmojiId()) {
            "$EMOJI_PLACEHOLDER_START$id$EMOJI_PLACEHOLDER_END"
        } else {
            mr.value
        }
    }

    text = text.replace(IMG_TAG_REGEX) { mr ->
        val id = mr.value.extractAnimeVostEmojiId()
        if (id != null) "$EMOJI_PLACEHOLDER_START$id$EMOJI_PLACEHOLDER_END" else mr.value
    }

    val spoilerContents = mutableListOf<SpoilerContent>()

    fun addSpoiler(title: String, bodyHtml: String): String {
        spoilerContents.add(
            SpoilerContent(
                title = title.htmlToPlainText(),
                html = bodyHtml.cleanSpoilerHtml(),
            ),
        )
        return "\u0002SPOILER${spoilerContents.size - 1}\u0003"
    }

    text = text.replace(DLE_SPOILER_REGEX) { mr ->
        addSpoiler(
            title = mr.groupValues[1].extractSpoilerTitle(),
            bodyHtml = mr.groupValues[2],
        )
    }

    text = text.replace(TITLE_SPOILER_REGEX, "")

    text = text.replace(TEXT_SPOILER_REGEX) { mr ->
        addSpoiler(title = "", bodyHtml = mr.groupValues[1])
    }

    text = text.replace(BBCODE_SPOILER_REGEX) { mr ->
        addSpoiler(title = mr.groupValues[1], bodyHtml = mr.groupValues[2])
    }

    text = text.normalizeBlockTags()

    parseInlineHtml(text, spoilerContents, segments)

    return segments.filter { seg ->
        when (seg) {
            is CommentSegment.TextSegment -> seg.text.isNotEmpty()
            else -> true
        }
    }.withoutExcessLineBreaks()
}

private fun Int.isAnimeVostEmojiId(): Boolean =
    this in 1..100 || this == 102

private fun String.extractAnimeVostEmojiId(): Int? {
    val classValue = IMG_CLASS_REGEX.find(this)?.groupValues?.getOrNull(1).orEmpty()
    val hasEmojiClass = classValue.split(Regex("""\s+""")).any { it.equals("emoji", ignoreCase = true) }
    val srcEmojiId = IMG_EMOJI_SRC_ID_REGEX.find(this)?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (!hasEmojiClass && srcEmojiId == null) return null

    return IMG_ALT_ID_REGEX.find(this)?.groupValues?.getOrNull(1)?.toIntOrNull()
        ?.takeIf { it.isAnimeVostEmojiId() }
        ?: srcEmojiId?.takeIf { it.isAnimeVostEmojiId() }
}

private fun String.extractSpoilerTitle(): String {
    val markerTitle = Regex(
        """<!--spoiler_?title-->(.*?)<!--spoiler_?title_?end-->""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    ).find(this)?.groupValues?.getOrNull(1)

    return (markerTitle ?: this)
        .htmlToPlainText()
        .takeUnless { it.equals("Показать / Скрыть текст", ignoreCase = true) }
        .orEmpty()
}

private fun String.cleanSpoilerHtml(): String =
    replace(Regex("""<!--spoiler_?text-->""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""<!--spoiler_?text_?end-->""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""<!--/?dle_?spoiler.*?-->""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
        .trim()

private fun String.normalizeBlockTags(): String =
    replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("""<li\b[^>]*>""", RegexOption.IGNORE_CASE), "\n- ")
        .replace(Regex("""</li\s*>""", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("""</p\s*>""", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("""</tr\s*>""", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("""</div\s*>""", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("""</ol\s*>|</ul\s*>""", RegexOption.IGNORE_CASE), "\n")

private fun String.htmlToPlainText(): String =
    normalizeBlockTags()
        .replace(Regex("""<!--.*?-->""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
        .replace(Regex("""<[^>]+>"""), "")
        .decodeHtmlEntities()
        .replace(Regex("""[ \t\u00A0]+"""), " ")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()

private fun String.decodeHtmlEntities(): String =
    replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&nbsp;", " ")
        .replace("&#39;", "'")

private fun List<CommentSegment>.withoutExcessLineBreaks(): List<CommentSegment> {
    val result = mutableListOf<CommentSegment>()
    var pendingLineBreak = false

    for (segment in this) {
        when (segment) {
            is CommentSegment.LineBreak -> {
                if (result.isNotEmpty() && !pendingLineBreak) {
                    result += segment
                }
                pendingLineBreak = true
            }
            is CommentSegment.TextSegment -> {
                val text = segment.text.replace(Regex("""[ \t\u00A0]+"""), " ")
                if (text.isNotBlank()) {
                    result += segment.copy(text = text)
                    pendingLineBreak = false
                }
            }
            else -> {
                result += segment
                pendingLineBreak = false
            }
        }
    }

    while (result.lastOrNull() is CommentSegment.LineBreak) {
        result.removeAt(result.lastIndex)
    }
    return result
}

private fun parseInlineHtml(
    html: String,
    spoilerContents: List<SpoilerContent>,
    out: MutableList<CommentSegment>,
) {
    val tokenPattern = Regex(
        """(<b>|</b>|<strong>|</strong>|<i>|</i>|<em>|</em>|<s>|</s>|<del>|</del>|""" +
        """\uE000(\d+)\uE001|\u0002SPOILER(\d+)\u0003|\n|<[^>]+>)""",
        RegexOption.IGNORE_CASE,
    )

    var bold = false
    var italic = false
    var strike = false
    var lastEnd = 0

    fun flushText(rawText: String) {
        if (rawText.isNotEmpty()) {
            val unescaped = rawText.decodeHtmlEntities()
            out.add(CommentSegment.TextSegment(unescaped, bold, italic, strike))
        }
    }

    for (match in tokenPattern.findAll(html)) {
        val before = html.substring(lastEnd, match.range.first)
        flushText(before)
        lastEnd = match.range.last + 1

        val token = match.value
        val emojiId = match.groupValues[2]
        val spoilerIdx = match.groupValues[3]

        when {
            emojiId.isNotEmpty() -> out.add(CommentSegment.EmojiSegment(emojiId.toInt()))
            spoilerIdx.isNotEmpty() -> {
                val idx = spoilerIdx.toInt()
                if (idx < spoilerContents.size) {
                    val spoiler = spoilerContents[idx]
                    out.add(CommentSegment.SpoilerSegment(spoiler.title, spoiler.html))
                }
            }
            token == "\n" -> out.add(CommentSegment.LineBreak)
            token.equals("<b>", ignoreCase = true) || token.equals("<strong>", ignoreCase = true) -> bold = true
            token.equals("</b>", ignoreCase = true) || token.equals("</strong>", ignoreCase = true) -> bold = false
            token.equals("<i>", ignoreCase = true) || token.equals("<em>", ignoreCase = true) -> italic = true
            token.equals("</i>", ignoreCase = true) || token.equals("</em>", ignoreCase = true) -> italic = false
            token.equals("<s>", ignoreCase = true) || token.equals("<del>", ignoreCase = true) -> strike = true
            token.equals("</s>", ignoreCase = true) || token.equals("</del>", ignoreCase = true) -> strike = false
            // skip other HTML tags
        }
    }
    flushText(html.substring(lastEnd))
}

@Composable
fun CommentHtmlRenderer(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    val segments = remember(html) { parseCommentHtml(html) }

    val hasSpecial = segments.any { it is CommentSegment.EmojiSegment || it is CommentSegment.SpoilerSegment }

    if (!hasSpecial) {
        val annotated = buildAnnotatedString {
            for (seg in segments) {
                when (seg) {
                    is CommentSegment.TextSegment -> {
                        withStyle(SpanStyle(
                            fontWeight = if (seg.bold) FontWeight.Bold else null,
                            fontStyle = if (seg.italic) FontStyle.Italic else null,
                            textDecoration = if (seg.strikethrough) TextDecoration.LineThrough else null,
                        )) { append(seg.text) }
                    }
                    is CommentSegment.LineBreak -> append("\n")
                    else -> {}
                }
            }
        }
        Text(text = annotated, style = style, modifier = modifier)
    } else {
        Column(modifier = modifier) {
            RenderSegments(segments, style)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RenderSegments(segments: List<CommentSegment>, style: TextStyle) {
    val lines = mutableListOf<MutableList<CommentSegment>>()
    var line = mutableListOf<CommentSegment>()
    for (item in segments) {
        if (item is CommentSegment.LineBreak) {
            lines.add(line)
            line = mutableListOf()
        } else {
            line.add(item)
        }
    }
    if (line.isNotEmpty()) lines.add(line)

    for (lineItems in lines) {
        RenderCommentLine(lineItems, style)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RenderCommentLine(lineItems: List<CommentSegment>, style: TextStyle) {
    val needsInlineLayout = lineItems.any {
        it is CommentSegment.EmojiSegment || it is CommentSegment.SpoilerSegment
    }

    if (needsInlineLayout) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            var textAcc = buildAnnotatedString {}
            for (item in lineItems) {
                when (item) {
                    is CommentSegment.TextSegment -> {
                        textAcc = buildAnnotatedString {
                            append(textAcc)
                            appendTextSegment(item)
                        }
                    }
                    is CommentSegment.EmojiSegment -> {
                        if (textAcc.isNotEmpty()) {
                            val text = textAcc
                            Text(text = text, style = style)
                            textAcc = buildAnnotatedString {}
                        }
                        AsyncImage(
                            model = "$EMOJI_BASE${item.id}.gif",
                            contentDescription = "emoji ${item.id}",
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    is CommentSegment.SpoilerSegment -> {
                        if (textAcc.isNotEmpty()) {
                            val text = textAcc
                            Text(text = text, style = style)
                            textAcc = buildAnnotatedString {}
                        }
                        InlineSpoiler(title = item.title, html = item.html, style = style)
                    }
                    is CommentSegment.LineBreak -> Unit
                }
            }
            if (textAcc.isNotEmpty()) {
                Text(text = textAcc, style = style)
            }
        }
    } else {
        val annotated = buildAnnotatedString {
            for (item in lineItems) {
                if (item is CommentSegment.TextSegment) {
                    appendTextSegment(item)
                }
            }
        }
        if (annotated.isNotEmpty()) {
            Text(text = annotated, style = style)
        }
    }
}

private fun AnnotatedString.Builder.appendTextSegment(segment: CommentSegment.TextSegment) {
    withStyle(
        SpanStyle(
            fontWeight = if (segment.bold) FontWeight.Bold else null,
            fontStyle = if (segment.italic) FontStyle.Italic else null,
            textDecoration = if (segment.strikethrough) TextDecoration.LineThrough else null,
        ),
    ) {
        append(segment.text)
    }
}

@Composable
private fun InlineSpoiler(title: String, html: String, style: TextStyle) {
    var revealed by remember(html) { mutableStateOf(false) }
    val text = remember(title, html) {
        html.htmlToPlainText()
            .replace(Regex("""\s+"""), " ")
            .trim()
            .ifBlank { title.trim() }
            .ifBlank { "Спойлер" }
    }
    val shape = RoundedCornerShape(4.dp)

    if (revealed) {
        Text(
            text = text,
            style = style,
            modifier = Modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .clickable { revealed = false }
                .padding(horizontal = 3.dp, vertical = 1.dp),
        )
    } else {
        val maskColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
        val dotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
        Box(
            modifier = Modifier
                .clip(shape)
                .background(maskColor)
                .drawBehind {
                    val step = 5.dp.toPx()
                    val radius = 0.9.dp.toPx()
                    var y = radius
                    var row = 0
                    while (y < size.height) {
                        var x = radius + if (row % 2 == 0) 0f else step / 2f
                        while (x < size.width) {
                            drawCircle(dotColor, radius, Offset(x, y))
                            x += step
                        }
                        y += step
                        row++
                    }
                }
                .clickable { revealed = true }
                .padding(horizontal = 3.dp, vertical = 1.dp),
        ) {
            Text(
                text = text,
                style = style.copy(color = Color.Transparent),
            )
        }
    }
}
