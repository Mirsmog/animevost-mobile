package com.animevost.app.core.network.parser

import com.animevost.app.core.domain.model.Schedule
import com.animevost.app.core.domain.model.ScheduleItem
import com.animevost.app.core.network.DleEndpoints
import org.jsoup.Jsoup
import javax.inject.Inject

class ScheduleParser @Inject constructor() {

    private val dayDivs = listOf(
        "raspisMon" to "Понедельник",
        "raspisTue" to "Вторник",
        "raspisWed" to "Среда",
        "raspisThu" to "Четверг",
        "raspisFri" to "Пятница",
        "raspisSat" to "Суббота",
        "raspisSun" to "Воскресенье",
    )

    private val timeRegex = Regex("""\((\d{1,2}:\d{2})\)\s*$""")

    fun parse(html: String): List<Schedule> {
        val doc = Jsoup.parse(html, DleEndpoints.BASE_URL)

        return dayDivs.mapNotNull { (divId, dayName) ->
            val div = doc.getElementById(divId) ?: return@mapNotNull null

            val items = div.select("a").mapNotNull { a ->
                val url = a.absUrl("href").ifEmpty {
                    AnimeListParser.resolveUrl(a.attr("href"))
                }
                val text = a.text().trim()
                if (text.isEmpty()) return@mapNotNull null

                val timeMatch = timeRegex.find(text)
                val time = timeMatch?.groupValues?.get(1).orEmpty()
                val title = text.replace(timeRegex, "")
                    .trimEnd()
                    .removeSuffix("~")
                    .trim()

                ScheduleItem(title = title, time = time, url = url)
            }

            if (items.isEmpty()) return@mapNotNull null
            Schedule(dayOfWeek = dayName, items = items)
        }
    }
}
