package com.animevost.app.feature.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/** Sealed hierarchy representing parsed comment segments */
sealed class CommentSegment {
    data class TextSegment(
        val text: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val strikethrough: Boolean = false,
    ) : CommentSegment()
    data class EmojiSegment(val id: Int) : CommentSegment()
    data class SpoilerSegment(val text: String) : CommentSegment()
    object LineBreak : CommentSegment()
}

/** Parse raw HTML (from server) into a list of CommentSegments. Regex-only, no Jsoup. */
fun parseCommentHtml(html: String): List<CommentSegment> {
    val segments = mutableListOf<CommentSegment>()

    // Normalize <br> and <br/> to newlines
    var text = html
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")

    // Replace emoji markers: <!--smile:N-->...<img ...>...<!--/smile--> → placeholder
    text = text.replace(
        Regex("""<!--smile:(\d+)-->.*?<!--/smile-->""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    ) { mr -> "$EMOJI_PLACEHOLDER_START${mr.groupValues[1]}$EMOJI_PLACEHOLDER_END" }

    // Also handle orphaned <img class="emoji" alt="N" ...>
    text = text.replace(
        Regex("""<img[^>]+class="emoji"[^>]+alt="(\d+)"[^>]*/?>""", RegexOption.IGNORE_CASE)
    ) { mr -> "$EMOJI_PLACEHOLDER_START${mr.groupValues[1]}$EMOJI_PLACEHOLDER_END" }

    // Extract spoiler divs
    val spoilerContents = mutableListOf<String>()
    text = text.replace(
        Regex("""<div[^>]*class="text_spoiler"[^>]*>(.*?)</div>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    ) { mr ->
        val inner = mr.groupValues[1].replace(Regex("<[^>]+>"), "").trim()
        spoilerContents.add(inner)
        "\u0002SPOILER${spoilerContents.size - 1}\u0003"
    }

    // Handle BBCode spoiler
    text = text.replace(
        Regex("""\[spoiler](.*?)\[/spoiler]""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    ) { mr ->
        val inner = mr.groupValues[1]
        spoilerContents.add(inner)
        "\u0002SPOILER${spoilerContents.size - 1}\u0003"
    }

    parseInlineHtml(text, spoilerContents, segments)

    return segments.filter { seg ->
        when (seg) {
            is CommentSegment.TextSegment -> seg.text.isNotEmpty()
            else -> true
        }
    }
}

private fun parseInlineHtml(
    html: String,
    spoilerContents: List<String>,
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
            val unescaped = rawText
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&nbsp;", " ")
                .replace("&#39;", "'")
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
                    out.add(CommentSegment.SpoilerSegment(spoilerContents[idx]))
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
            renderSegments(segments, style)
        }
    }
}

@Composable
private fun renderSegments(segments: List<CommentSegment>, style: TextStyle) {
    data class Run(val items: List<CommentSegment>)

    val runs = mutableListOf<Run>()
    var current = mutableListOf<CommentSegment>()
    for (seg in segments) {
        if (seg is CommentSegment.SpoilerSegment) {
            if (current.isNotEmpty()) { runs.add(Run(current)); current = mutableListOf() }
            runs.add(Run(listOf(seg)))
        } else {
            current.add(seg)
        }
    }
    if (current.isNotEmpty()) runs.add(Run(current))

    for (run in runs) {
        val first = run.items.firstOrNull()
        if (first is CommentSegment.SpoilerSegment) {
            SpoilerBlock(text = first.text)
        } else {
            val lines = mutableListOf<MutableList<CommentSegment>>()
            var line = mutableListOf<CommentSegment>()
            for (item in run.items) {
                if (item is CommentSegment.LineBreak) {
                    lines.add(line); line = mutableListOf()
                } else {
                    line.add(item)
                }
            }
            if (line.isNotEmpty()) lines.add(line)

            for (lineItems in lines) {
                if (lineItems.any { it is CommentSegment.EmojiSegment }) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        var textAcc = buildAnnotatedString {}
                        for (item in lineItems) {
                            when (item) {
                                is CommentSegment.TextSegment -> {
                                    textAcc = buildAnnotatedString {
                                        append(textAcc)
                                        withStyle(SpanStyle(
                                            fontWeight = if (item.bold) FontWeight.Bold else null,
                                            fontStyle = if (item.italic) FontStyle.Italic else null,
                                            textDecoration = if (item.strikethrough) TextDecoration.LineThrough else null,
                                        )) { append(item.text) }
                                    }
                                }
                                is CommentSegment.EmojiSegment -> {
                                    if (textAcc.isNotEmpty()) {
                                        val t = textAcc
                                        Text(text = t, style = style)
                                        textAcc = buildAnnotatedString {}
                                    }
                                    AsyncImage(
                                        model = "$EMOJI_BASE${item.id}.gif",
                                        contentDescription = "emoji ${item.id}",
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                else -> {}
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
                                withStyle(SpanStyle(
                                    fontWeight = if (item.bold) FontWeight.Bold else null,
                                    fontStyle = if (item.italic) FontStyle.Italic else null,
                                    textDecoration = if (item.strikethrough) TextDecoration.LineThrough else null,
                                )) { append(item.text) }
                            }
                        }
                    }
                    if (annotated.isNotEmpty()) Text(text = annotated, style = style)
                }
            }
        }
    }
}

@Composable
private fun SpoilerBlock(text: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "⚠ Спойлер",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Скрыть" else "Показать")
                }
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
