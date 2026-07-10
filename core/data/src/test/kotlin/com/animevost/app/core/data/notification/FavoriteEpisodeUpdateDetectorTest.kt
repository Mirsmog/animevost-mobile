package com.animevost.app.core.data.notification

import com.animevost.app.core.data.db.FavoriteEntity
import com.animevost.app.core.network.parser.RssItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoriteEpisodeUpdateDetectorTest {
    private val detector = FavoriteEpisodeUpdateDetector()

    @Test
    fun `detects a new episode only for a favorite anime`() {
        val favorite = favorite(id = 3802, episodeInfo = "1-14 из 24+")
        val feed = listOf(
            rss(
                id = 3802,
                title = "О моём перерождении в слизь [1-15 из 24+] [16 серия - 17 июля]",
            ),
            rss(id = 9999, title = "Другое аниме [1-8 из 12+]"),
        )

        val result = detector.detect(
            favorites = listOf(favorite),
            feedItems = feed,
            knownEpisodes = mapOf(3802 to 14),
        )

        assertEquals(1, result.updates.size)
        assertEquals(14, result.updates.single().previousEpisode)
        assertEquals(15, result.updates.single().currentEpisode)
        assertEquals(15, result.episodesByAnimeId[3802])
    }

    @Test
    fun `does not notify when a newly added favorite already has current episode`() {
        val result = detector.detect(
            favorites = listOf(favorite(id = 3940, episodeInfo = "1-2 из 12+")),
            feedItems = listOf(rss(id = 3940, title = "Адский режим [1-2 из 12+]")),
            knownEpisodes = emptyMap(),
        )

        assertTrue(result.updates.isEmpty())
        assertEquals(2, result.episodesByAnimeId[3940])
    }

    @Test
    fun `matches favorite across different mirror hosts by news id`() {
        val favorite = favorite(
            id = 3940,
            episodeInfo = "1 из 12+",
            url = "https://animevost.org/tip/tv/3940-hell-mode.html",
        )
        val feed = RssItem(
            title = "Адский режим [1-2 из 12+]",
            link = "https://v13.vost.pw/tip/tv/3940-hell-mode.html",
            pubDate = "",
            description = "",
        )

        val result = detector.detect(
            favorites = listOf(favorite),
            feedItems = listOf(feed),
            knownEpisodes = mapOf(3940 to 1),
        )

        assertEquals(2, result.updates.single().currentEpisode)
    }

    @Test
    fun `ignores announced future episode in title`() {
        val episode = detector.extractCurrentEpisode(
            "Слизь [1-14 из 24+] [15 серия - 17 июля]",
        )

        assertEquals(14, episode)
    }

    @Test
    fun `supports a single released episode title`() {
        assertEquals(7, detector.extractCurrentEpisode("Название аниме [7 серия]"))
    }

    @Test
    fun `removes state for anime no longer in favorites`() {
        val result = detector.detect(
            favorites = listOf(favorite(id = 3802, episodeInfo = "1-14 из 24+")),
            feedItems = emptyList(),
            knownEpisodes = mapOf(3802 to 14, 3940 to 2),
        )

        assertEquals(mapOf(3802 to 14), result.episodesByAnimeId)
    }

    private fun favorite(
        id: Int,
        episodeInfo: String,
        url: String = "tip/tv/$id-title.html",
    ) = FavoriteEntity(
        newsId = id,
        title = "Anime $id",
        titleOriginal = "",
        posterUrl = "",
        episodeInfo = episodeInfo,
        url = url,
    )

    private fun rss(id: Int, title: String) = RssItem(
        title = title,
        link = "https://v13.vost.pw/tip/tv/$id-title.html",
        pubDate = "",
        description = "",
    )
}
