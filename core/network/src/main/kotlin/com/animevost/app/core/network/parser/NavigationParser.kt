package com.animevost.app.core.network.parser

import com.animevost.app.core.domain.model.Genre
import com.animevost.app.core.domain.model.NavData
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationParser @Inject constructor() {

    fun parse(html: String): NavData {
        val doc = Jsoup.parse(html)
        val nav = doc.selectFirst("div.menu ul#topnav") ?: return NavData()

        val genres = nav.select("a[href]")
            .filter { it.attr("href").matches(Regex("^/zhanr/[a-z].*")) }
            .mapIndexed { index, el ->
                Genre(index + 1, el.text().trim(), el.attr("href"))
            }

        val years = nav.select("a[href]")
            .filter { it.attr("href").matches(Regex("^/god/\\d{4}/$")) }
            .map { it.text().trim() }
            .distinct()
            .sortedDescending()

        return NavData(genres = genres, years = years)
    }
}
