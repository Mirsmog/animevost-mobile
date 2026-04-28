package com.animevost.app.core.network.alloha

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import timber.log.Timber

/**
 * Loads an Alloha iframe page (https://absciss.thealloha.club/?token=…&token_movie=…)
 * and extracts the meta `viewporti` seed plus the JSON `fileList` describing
 * available translations per (season, episode).
 */
class AllohaIframeFetcher(
    private val client: OkHttpClient,
    private val gson: Gson = Gson(),
) {

    suspend fun fetch(iframeUrl: String): AllohaIframeData? {
        val request = Request.Builder()
            .url(iframeUrl)
            .header("User-Agent", BorthCodec.userAgent())
            .header("Referer", "https://yummyanime.tv/")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "ru,en;q=0.9")
            .build()
        val body = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                Timber.w("Alloha iframe fetch failed: HTTP %d", resp.code)
                return null
            }
            resp.body?.string() ?: return null
        }
        return parse(iframeUrl, body)
    }

    private fun parse(iframeUrl: String, html: String): AllohaIframeData? {
        val doc = Jsoup.parse(html)
        val seed = doc.select("meta[name=viewporti]").attr("content").takeIf { it.isNotBlank() }
            ?: run {
                Timber.w("Alloha iframe missing meta viewporti seed")
                return null
            }
        val token = TOKEN_REGEX.find(iframeUrl)?.groupValues?.get(1) ?: return null
        val tokenMovie = TOKEN_MOVIE_REGEX.find(iframeUrl)?.groupValues?.get(1) ?: return null
        val fileListJson = FILELIST_REGEX.find(html)?.groupValues?.get(1)
            ?: run {
                Timber.w("Alloha iframe missing fileList JSON")
                return null
            }
        val unescaped = unescapeJsString(fileListJson)
        val translations = parseTranslations(unescaped)
        return AllohaIframeData(
            token = token,
            tokenMovie = tokenMovie,
            seed = seed,
            translations = translations,
        )
    }

    private fun parseTranslations(json: String): Map<Int, Map<Int, Map<String, AllohaTranslation>>> {
        return runCatching {
            val root = gson.fromJson(json, JsonObject::class.java)
            val all = root.getAsJsonObject("all") ?: return emptyMap()
            buildMap {
                for ((seasonKey, seasonJson) in all.entrySet()) {
                    val seasonNum = seasonKey.toIntOrNull() ?: continue
                    val episodes = seasonJson.asJsonObject ?: continue
                    val episodeMap = buildMap<Int, Map<String, AllohaTranslation>> {
                        for ((episodeKey, episodeJson) in episodes.entrySet()) {
                            val epNum = episodeKey.toIntOrNull() ?: continue
                            val translationsObj = episodeJson.asJsonObject ?: continue
                            val translationMap = buildMap<String, AllohaTranslation> {
                                for ((tKey, tJson) in translationsObj.entrySet()) {
                                    parseTranslation(tJson)?.let { put(tKey, it) }
                                }
                            }
                            put(epNum, translationMap)
                        }
                    }
                    put(seasonNum, episodeMap)
                }
            }
        }.getOrElse {
            Timber.w(it, "Failed to parse Alloha fileList JSON")
            emptyMap()
        }
    }

    private fun parseTranslation(node: JsonElement): AllohaTranslation? = runCatching {
        val obj = node.asJsonObject
        AllohaTranslation(
            id = obj["id"].asLong,
            idFile = obj["id_file"]?.takeUnless { it.isJsonNull }?.asLong,
            translation = obj["translation"]?.asString.orEmpty(),
            idTranslation = obj["id_translation"]?.asInt ?: 0,
        )
    }.getOrNull()

    /** The fileList JSON is wrapped in JSON.parse('…'), so we need to unescape JS string escapes. */
    private fun unescapeJsString(s: String): String = buildString(s.length) {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val next = s[i + 1]) {
                    '\'', '\"', '\\', '/' -> append(next)
                    'n' -> append('\n')
                    'r' -> append('\r')
                    't' -> append('\t')
                    'b' -> append('\b')
                    'f' -> append('\u000C')
                    'u' -> if (i + 5 < s.length) {
                        append(s.substring(i + 2, i + 6).toInt(16).toChar())
                        i += 4
                    } else append(next)
                    else -> { append(c); append(next) }
                }
                i += 2
            } else {
                append(c)
                i++
            }
        }
    }

    companion object {
        private val TOKEN_REGEX = Regex("[?&]token=([0-9a-fA-F]+)")
        private val TOKEN_MOVIE_REGEX = Regex("[?&]token_movie=([0-9a-fA-F]+)")
        private val FILELIST_REGEX = Regex("""const\s+fileList\s*=\s*JSON\.parse\(\s*'(.+?)'\s*\)\s*;""")
    }
}
