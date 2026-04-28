package com.animevost.app.core.network.alloha

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import timber.log.Timber

/**
 * Resolves an animevost title to a yummyanime.tv anime id by querying the
 * site's DLE search. Yummyanime URLs use the form `/{id}-slug.html`, so we
 * extract the leading numeric segment from the first matching `<a>` tag.
 */
class YummyAnimeSearchClient(
    private val client: OkHttpClient,
) {

    suspend fun searchFirstId(candidates: List<String>): Int? {
        for (raw in candidates) {
            val query = raw.trim()
            if (query.isEmpty()) continue
            try {
                val id = searchOnce(query)
                if (id != null) {
                    Timber.d("YummyAnime resolved query='%s' → id=%d", query, id)
                    return id
                }
            } catch (e: Exception) {
                Timber.w(e, "YummyAnime search failed for '%s'", query)
            }
        }
        return null
    }

    private fun searchOnce(query: String): Int? {
        val body = FormBody.Builder()
            .add("do", "search")
            .add("subaction", "search")
            .add("story", query)
            .build()
        val req = Request.Builder()
            .url("https://yummyanime.tv/index.php?do=search")
            .post(body)
            .header("User-Agent", BorthCodec.userAgent())
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "ru,en;q=0.9")
            .header("Referer", "https://yummyanime.tv/")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val html = resp.body?.string() ?: return null
            val doc = Jsoup.parse(html, "https://yummyanime.tv/")
            val link = doc.select("a.movie-item__link[href]").firstOrNull() ?: return null
            val href = link.absUrl("href").ifEmpty { link.attr("href") }
            val match = ID_REGEX.find(href) ?: return null
            return match.groupValues[1].toIntOrNull()
        }
    }

    companion object {
        private val ID_REGEX = Regex("""yummyanime\.tv/(\d+)-""")
    }
}
