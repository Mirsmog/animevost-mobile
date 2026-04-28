package com.animevost.app.core.network.alloha

import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber

/**
 * Top-level facade used by the data layer to fetch skip intervals from
 * yummyanime.tv's Alloha JSON player. The Alloha API is bot-shielded by the
 * `Borth` header which is bound to a server-issued seed (`<meta viewporti>`)
 * combined with a fixed device fingerprint hash; see [BorthCodec].
 *
 * The translation that AnimeVost itself uploads (`t213`) carries a `null`
 * id_file. We therefore fall back to a sibling translation (AniDUB / AniMaunt
 * etc.) whose timing is usually the same — accepted by the user.
 */
class AllohaSkipClient(
    private val yummyApi: YummyAnimeApi,
    private val allohaApi: AllohaApi,
    private val iframeFetcher: AllohaIframeFetcher,
) {

    /**
     * Loads skip intervals for a given yummyanime id and episode.
     *
     * @param yummyAnimeId yummyanime.tv anime id
     * @param episode 1-based episode number (matches AnimeVost numbering)
     * @return parsed skipTime ranges, empty list if none / unavailable
     */
    suspend fun loadSkipIntervals(
        yummyAnimeId: Int,
        episode: Int,
    ): List<AllohaSkipRange> {
        warmUpIfNeeded()
        val iframeResp = runCatching { yummyApi.getAllohaIframeUrl(yummyAnimeId = yummyAnimeId) }
            .onFailure { Timber.w(it, "Failed to fetch yummyanime iframe URL for id=%d", yummyAnimeId) }
            .getOrNull() ?: return emptyList()
        val iframeUrl = iframeResp.iframeUrl?.takeIf { it.isNotBlank() } ?: return emptyList()
        val iframeData = iframeFetcher.fetch(iframeUrl) ?: return emptyList()

        val translation = pickTranslation(iframeData, episode) ?: run {
            Timber.w(
                "No usable Alloha translation for yummyId=%d ep=%d",
                yummyAnimeId, episode,
            )
            return emptyList()
        }
        val idFile = translation.idFile ?: return emptyList()

        val borth = BorthCodec.buildBorthHeader(iframeData.seed)
        val origin = "https://${iframeUrl.toHttpUrl().host}"
        val postUrl = "$origin/bnsi/movies/$idFile"
        val headers = AllohaHeaders.forMoviePost(
            referer = iframeUrl,
            origin = origin,
            borth = borth,
        )
        val response = runCatching {
            allohaApi.getMovieData(
                url = postUrl,
                headers = headers,
                token = iframeData.token,
            )
        }.onFailure {
            Timber.w(it, "Alloha /bnsi/movies POST failed for idFile=%d", idFile)
        }.getOrNull() ?: return emptyList()

        if (!response.isSuccessful) {
            Timber.w("Alloha /bnsi/movies returned HTTP %d", response.code())
            return emptyList()
        }
        val body = response.body()
        val skipTime = body?.skipTime ?: return emptyList()
        return parseSkipTime(skipTime)
    }

    @Volatile
    private var warmedUp = false

    private suspend fun warmUpIfNeeded() {
        if (warmedUp) return
        runCatching { yummyApi.warmUp() }
            .onFailure { Timber.w(it, "Yummyanime warm-up failed") }
            .onSuccess { warmedUp = true }
    }

    private fun pickTranslation(data: AllohaIframeData, episode: Int): AllohaTranslation? {
        // animevost rarely exposes seasons beyond 1; try season 1 first, then any.
        val seasonOrder = sequenceOf(1, *data.translations.keys.toTypedArray())
            .distinct()
            .toList()
        for (season in seasonOrder) {
            val episodes = data.translations[season] ?: continue
            val translations = episodes[episode] ?: continue
            // Prefer entries that actually have an id_file
            val viable = translations.values.firstOrNull { it.idFile != null }
            if (viable != null) return viable
        }
        return null
    }

    /** Parses Alloha `"0-135,1038-1152"` (seconds) into ms ranges. */
    internal fun parseSkipTime(raw: String): List<AllohaSkipRange> {
        if (raw.isBlank()) return emptyList()
        val segments = raw.split(',')
            .mapNotNull { seg ->
                val parts = seg.split('-')
                if (parts.size != 2) return@mapNotNull null
                val start = parts[0].trim().toLongOrNull() ?: return@mapNotNull null
                val end = parts[1].trim().toLongOrNull() ?: return@mapNotNull null
                if (end <= start) null else start to end
            }
        if (segments.isEmpty()) return emptyList()

        return segments.mapIndexed { idx, (start, end) ->
            // First segment that starts within 2 minutes is treated as opening.
            val type = when {
                idx == 0 && start <= OPENING_MAX_START_S -> AllohaSkipType.OPENING
                else -> AllohaSkipType.ENDING
            }
            AllohaSkipRange(
                type = type,
                startMs = start * 1000,
                endMs = end * 1000,
            )
        }
    }

    companion object {
        private const val OPENING_MAX_START_S = 120L
    }
}

enum class AllohaSkipType { OPENING, ENDING }

data class AllohaSkipRange(
    val type: AllohaSkipType,
    val startMs: Long,
    val endMs: Long,
)
