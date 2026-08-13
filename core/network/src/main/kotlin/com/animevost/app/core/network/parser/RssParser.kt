package com.animevost.app.core.network.parser

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import javax.inject.Inject

data class RssItem(
    val title: String,
    val link: String,
    val pubDate: String,
    val description: String,
    val categories: List<String> = emptyList(),
)

class RssParser @Inject constructor() {

    fun parse(xml: String): List<RssItem> {
        val items = mutableListOf<RssItem>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var insideItem = false
        var currentTag = ""
        var title = ""
        var link = ""
        var pubDate = ""
        var description = ""
        var category = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "item") {
                        insideItem = true
                        title = ""
                        link = ""
                        pubDate = ""
                        description = ""
                        category = ""
                    }
                }

                XmlPullParser.TEXT -> {
                    if (insideItem) {
                        val text = parser.text?.trim().orEmpty()
                        when (currentTag) {
                            "title" -> title += text
                            "link" -> link += text
                            "pubDate" -> pubDate += text
                            "description" -> description += text
                            "category" -> category += text
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && insideItem) {
                        insideItem = false
                        items += RssItem(
                            title = title,
                            link = link,
                            pubDate = pubDate,
                            description = description,
                            categories = category
                                .split(',')
                                .map { it.trim() }
                                .filter { it.isNotEmpty() },
                        )
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        return items
    }
}
