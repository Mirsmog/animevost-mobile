package com.animevost.app.core.network.animethemes

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.util.Locale

data class AnimeThemeReference(
    val id: Int,
    val type: String,
    val episodes: String,
    val audioUrl: String,
    val sizeBytes: Long,
)

class AnimeThemesClient(
    private val client: OkHttpClient,
) {
    suspend fun findReferences(
        searchQueries: List<String>,
        year: Int?,
        episodeNumber: Int,
    ): List<AnimeThemeReference> = withContext(Dispatchers.IO) {
        var best: AnimeCandidate? = null
        Timber.d(
            "AnimeThemes lookup: year=%s ep=%d queries=%s",
            year,
            episodeNumber,
            searchQueries,
        )
        for (query in searchQueries.distinct().take(MAX_SEARCH_QUERIES)) {
            if (query.isBlank()) continue
            val candidates = search(query)
            if (candidates.isEmpty()) {
                Timber.d("AnimeThemes query returned no anime: '%s'", query)
                continue
            }
            val candidate = candidates.maxByOrNull { scoreCandidate(it, query, year) } ?: continue
            val score = scoreCandidate(candidate, query, year)
            Timber.d(
                "AnimeThemes candidate: query='%s' name='%s' year=%s score=%d",
                query,
                candidate.name,
                candidate.year,
                score,
            )
            if (score > (best?.score ?: Int.MIN_VALUE)) {
                best = candidate.copy(score = score)
            }
            if (score >= EXACT_MATCH_SCORE) break
        }
        val references = best?.let { selectReferences(it.payload, episodeNumber) }.orEmpty()
        Timber.d(
            "AnimeThemes selected: anime='%s' ep=%d refs=%s",
            best?.name,
            episodeNumber,
            references.map { "${it.type}:${it.id}:${it.episodes}" },
        )
        references
    }

    private fun search(query: String): List<AnimeCandidate> {
        val url = API_URL.toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("include", "animethemes.animethemeentries.videos.audio")
            .addQueryParameter("page[size]", PAGE_SIZE.toString())
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("AnimeThemes HTTP ${response.code}")
            }
            val json = response.body?.string().orEmpty()
            val root = JsonParser.parseString(json).asJsonObject
            return root.array("anime").mapNotNull { element ->
                val anime = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                AnimeCandidate(
                    name = anime.string("name"),
                    year = anime.intOrNull("year"),
                    mediaFormat = anime.string("media_format"),
                    payload = anime,
                )
            }
        }
    }

    private fun selectReferences(
        anime: JsonObject,
        episodeNumber: Int,
    ): List<AnimeThemeReference> {
        val candidates = anime.array("animethemes").flatMap { themeElement ->
            val theme = themeElement.takeIf { it.isJsonObject }?.asJsonObject
                ?: return@flatMap emptyList()
            val type = theme.string("type")
            if (type != "OP" && type != "ED") return@flatMap emptyList()
            theme.array("animethemeentries").mapNotNull { entryElement ->
                val entry = entryElement.takeIf { it.isJsonObject }?.asJsonObject
                    ?: return@mapNotNull null
                val episodes = entry.string("episodes")
                if (!episodeMatches(episodes, episodeNumber)) return@mapNotNull null
                val video = entry.array("videos")
                    .mapNotNull { it.takeIf { item -> item.isJsonObject }?.asJsonObject }
                    .filter { it.obj("audio") != null }
                    .maxByOrNull(::scoreVideo)
                    ?: return@mapNotNull null
                val audio = video.obj("audio") ?: return@mapNotNull null
                val id = audio.intOrNull("id") ?: return@mapNotNull null
                val link = audio.string("link")
                val size = audio.longOrNull("size") ?: 0L
                if (!link.startsWith("https://") || size > MAX_AUDIO_BYTES) {
                    return@mapNotNull null
                }
                AnimeThemeReference(
                    id = id,
                    type = type,
                    episodes = episodes,
                    audioUrl = link,
                    sizeBytes = size,
                )
            }
        }
        return candidates
            .distinctBy { it.id }
            .groupBy { it.type }
            .values
            .flatMap { references -> references.take(MAX_REFERENCES_PER_TYPE) }
    }

    private fun scoreCandidate(candidate: AnimeCandidate, query: String, year: Int?): Int {
        val name = normalize(candidate.name)
        val normalizedQuery = normalize(query)
        if (name.isBlank() || normalizedQuery.isBlank()) return Int.MIN_VALUE
        var score = when {
            name == normalizedQuery -> 1_000
            name.contains(normalizedQuery) || normalizedQuery.contains(name) -> 650
            else -> {
                val nameTokens = name.split(' ').filter(String::isNotBlank).toSet()
                val queryTokens = normalizedQuery.split(' ').filter(String::isNotBlank).toSet()
                val common = nameTokens.intersect(queryTokens).size
                if (common == 0) 0 else common * 300 / maxOf(nameTokens.size, queryTokens.size)
            }
        }
        if (year != null && candidate.year != null) {
            score += if (year == candidate.year) 250 else -minOf(250, kotlin.math.abs(year - candidate.year) * 60)
        }
        if (candidate.mediaFormat == "TV") score += 20
        return score
    }

    private fun scoreVideo(video: JsonObject): Int {
        var score = 0
        if (video.boolean("nc")) score += 40
        if (!video.boolean("subbed")) score += 20
        if (!video.boolean("lyrics")) score += 10
        if (video.string("overlap") == "None") score += 10
        return score
    }

    private fun episodeMatches(spec: String, episode: Int): Boolean {
        if (spec.isBlank()) return true
        val normalized = spec.replace(" ", "")
        return normalized.split(',').any { part ->
            when {
                part.matches(Regex("\\d+")) -> part.toIntOrNull() == episode
                part.count { it == '-' } == 1 -> {
                    val start = part.substringBefore('-').toIntOrNull() ?: 1
                    val end = part.substringAfter('-').toIntOrNull() ?: Int.MAX_VALUE
                    episode in start..end
                }
                else -> false
            }
        }
    }

    private fun normalize(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()

    private data class AnimeCandidate(
        val name: String,
        val year: Int?,
        val mediaFormat: String,
        val payload: JsonObject,
        val score: Int = Int.MIN_VALUE,
    )

    private fun JsonObject.array(name: String): JsonArray =
        get(name)?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()

    private fun JsonObject.obj(name: String): JsonObject? =
        get(name)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.string(name: String): String =
        get(name)?.takeIf { !it.isJsonNull }?.asString.orEmpty()

    private fun JsonObject.boolean(name: String): Boolean =
        get(name)?.takeIf { !it.isJsonNull }?.asBoolean ?: false

    private fun JsonObject.intOrNull(name: String): Int? =
        get(name)?.takeIf { !it.isJsonNull }?.runCatching { asInt }?.getOrNull()

    private fun JsonObject.longOrNull(name: String): Long? =
        get(name)?.takeIf { !it.isJsonNull }?.runCatching { asLong }?.getOrNull()

    private companion object {
        const val API_URL = "https://api.animethemes.moe/anime"
        const val PAGE_SIZE = 5
        const val MAX_SEARCH_QUERIES = 4
        const val MAX_REFERENCES_PER_TYPE = 1
        const val MAX_AUDIO_BYTES = 8L * 1024L * 1024L
        const val EXACT_MATCH_SCORE = 1_200
    }
}
