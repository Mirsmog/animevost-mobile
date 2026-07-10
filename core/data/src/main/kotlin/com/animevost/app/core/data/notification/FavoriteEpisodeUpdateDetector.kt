package com.animevost.app.core.data.notification

import com.animevost.app.core.data.db.FavoriteEntity
import com.animevost.app.core.network.parser.RssItem
import java.net.URI
import javax.inject.Inject

data class FavoriteEpisodeUpdate(
    val favorite: FavoriteEntity,
    val previousEpisode: Int,
    val currentEpisode: Int,
)

data class FavoriteEpisodeDetection(
    val updates: List<FavoriteEpisodeUpdate>,
    val episodesByAnimeId: Map<Int, Int>,
)

class FavoriteEpisodeUpdateDetector @Inject constructor() {

    fun detect(
        favorites: List<FavoriteEntity>,
        feedItems: List<RssItem>,
        knownEpisodes: Map<Int, Int>,
    ): FavoriteEpisodeDetection {
        val feedByAnimeId = mutableMapOf<Int, Int>()
        feedItems.forEach { item ->
            val animeId = extractAnimeId(item.link) ?: return@forEach
            val episode = extractCurrentEpisode(item.title) ?: return@forEach
            feedByAnimeId[animeId] = maxOf(feedByAnimeId[animeId] ?: 0, episode)
        }

        val favoriteIds = favorites.mapTo(mutableSetOf(), FavoriteEntity::newsId)
        val nextEpisodes = knownEpisodes
            .filterKeys { it in favoriteIds }
            .toMutableMap()
        val updates = mutableListOf<FavoriteEpisodeUpdate>()

        favorites.forEach { favorite ->
            val baseline = nextEpisodes[favorite.newsId]
                ?: extractCurrentEpisode(favorite.episodeInfo)
            if (baseline != null) nextEpisodes[favorite.newsId] = baseline

            val current = feedByAnimeId[favorite.newsId] ?: return@forEach
            if (baseline != null && current > baseline) {
                updates += FavoriteEpisodeUpdate(
                    favorite = favorite,
                    previousEpisode = baseline,
                    currentEpisode = current,
                )
            }
            nextEpisodes[favorite.newsId] = maxOf(baseline ?: current, current)
        }

        return FavoriteEpisodeDetection(
            updates = updates,
            episodesByAnimeId = nextEpisodes,
        )
    }

    internal fun extractCurrentEpisode(value: String): Int? =
        EPISODE_RANGE_REGEX.find(value)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?: EPISODE_SINGLE_REGEX.find(value)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()

    internal fun extractAnimeId(url: String): Int? {
        val path = runCatching { URI(url).path }.getOrNull().orEmpty().ifBlank { url }
        return ANIME_ID_REGEX.find(path)?.groupValues?.get(1)?.toIntOrNull()
    }

    private companion object {
        val EPISODE_RANGE_REGEX = Regex(
            pattern = """(?:\[|^|\s)(?:\d+\s*-\s*)?(\d+)\s+из\s+\d+\+?""",
            option = RegexOption.IGNORE_CASE,
        )
        val EPISODE_SINGLE_REGEX = Regex(
            pattern = """\[(\d+)\s+сери(?:я|и)\]""",
            option = RegexOption.IGNORE_CASE,
        )
        val ANIME_ID_REGEX = Regex("""/(\d+)-[^/]+(?:\.html)?/?$""")
    }
}
