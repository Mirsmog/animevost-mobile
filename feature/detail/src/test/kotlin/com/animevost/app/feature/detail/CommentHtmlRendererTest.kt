package com.animevost.app.feature.detail

import com.animevost.app.core.ui.components.CommentSegment
import com.animevost.app.core.ui.components.parseCommentHtml
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CommentHtmlRendererTest {
    @Test
    fun parsesEmojiImageWhenAltComesBeforeClass() {
        val segments = parseCommentHtml(
            """<div id="comm-id-2049658">Текст <img alt="41" class="emoji" src="https://animevost.org/engine/data/emoticons/41.gif"></div>""",
        )

        assertEquals(
            listOf(
                CommentSegment.TextSegment("Текст "),
                CommentSegment.EmojiSegment(41),
            ),
            segments,
        )
    }

    @Test
    fun parsesSdkCommentEmojiImageWithoutClass() {
        val segments = parseCommentHtml(
            """аниме + игра <img style="vertical-align: middle;border: none;" alt="11" src="/engine/data/emoticons/11.gif"> посмотрел""",
        )

        assertEquals(
            listOf(
                CommentSegment.TextSegment("аниме + игра "),
                CommentSegment.EmojiSegment(11),
                CommentSegment.TextSegment(" посмотрел"),
            ),
            segments,
        )
    }

    @Test
    fun parsesDleSpoilerWithTitleAndBody() {
        val segments = parseCommentHtml(
            """
            <!--dle_spoiler Это аниме состоит из: -->
            <div class="title_spoiler">
                <a href="javascript:ShowOrHide('spe')"><!--spoiler_title-->Это аниме состоит из:<!--spoiler_title_end--></a>
            </div>
            <div id="spe" class="text_spoiler" style="display:none;"><!--spoiler_text-->
                <ol>
                    <li><a href="/tip/tv/307-hack-sign.html">.хак//Знак</a> - ТВ (26 эп.), оригинальный сериал, 2002</li>
                </ol>
            <!--spoiler_text_end--></div><!--/dle_spoiler-->
            """.trimIndent(),
        )

        val spoiler = assertIs<CommentSegment.SpoilerSegment>(segments.single())
        assertEquals("Это аниме состоит из:", spoiler.title)

        val bodyText = parseCommentHtml(spoiler.html)
            .filterIsInstance<CommentSegment.TextSegment>()
            .joinToString(separator = "") { it.text }

        assertTrue(bodyText.contains(".хак//Знак - ТВ (26 эп.), оригинальный сериал, 2002"))
    }

    @Test
    fun parsesSdkCommentSpoilerMarkup() {
        val segments = parseCommentHtml(
            """
            <div class="titlespoiler"><a href="javascript:ShowOrHidec'spaae'"><img id="image-spaae" alt="" src="/templates/AnimeVostNext5/dleimages/spoiler-plus.gif"></a>&nbsp;<a href="javascript:ShowOrHidec'spaae'">Показать / Скрыть текст</a></div>
            <div id="spaae" class="textspoiler" style="display:none;">test</div>
            """.trimIndent(),
        )

        val spoiler = assertIs<CommentSegment.SpoilerSegment>(segments.single())
        assertEquals("", spoiler.title)
        assertEquals("test", spoiler.html)
    }
}
