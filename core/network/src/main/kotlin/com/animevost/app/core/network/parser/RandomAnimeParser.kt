package com.animevost.app.core.network.parser

import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RandomAnimeParser @Inject constructor() {
    /** Parses the HTML fragment from get_random_post.php and returns the relative anime URL. */
    fun parse(html: String): String? =
        Jsoup.parse(html).selectFirst("a")?.attr("href")?.takeIf { it.isNotEmpty() }
}
