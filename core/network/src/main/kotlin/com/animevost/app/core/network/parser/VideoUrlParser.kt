package com.animevost.app.core.network.parser

import com.animevost.app.core.domain.model.VideoSource
import org.jsoup.Jsoup
import javax.inject.Inject

class VideoUrlParser @Inject constructor() {

    private val fileFieldRegex = Regex(""""file"\s*:\s*"([^"]+)"""")
    private val qualitySegmentRegex = Regex("""\[([^\]]+)](https?://[^,\[\s]+)""")

    fun parse(html: String): List<VideoSource> {
        val downloadUrls = parseDownloadUrls(html)

        val sources = mutableListOf<VideoSource>()

        val fileMatch = fileFieldRegex.find(html)
        if (fileMatch != null) {
            val fileValue = fileMatch.groupValues[1]
            qualitySegmentRegex.findAll(fileValue).forEach { match ->
                val quality = match.groupValues[1]
                val streamUrl = match.groupValues[2].trim()
                val downloadUrl = findDownloadUrl(quality, downloadUrls)
                sources.add(VideoSource(quality = quality, url = streamUrl, downloadUrl = downloadUrl))
            }
        }

        // Fallback: if no Playerjs found, use download links
        if (sources.isEmpty()) {
            downloadUrls.forEach { (label, url) ->
                sources.add(
                    VideoSource(
                        quality = label,
                        url = url.replace("&d=1", ""),
                        downloadUrl = url,
                    )
                )
            }
        }

        return sources
    }

    private fun findDownloadUrl(quality: String, downloadUrls: Map<String, String>): String {
        downloadUrls[quality]?.let { return it }
        return downloadUrls.entries.firstOrNull { (key, _) ->
            quality.contains(key, ignoreCase = true) || key.contains(quality, ignoreCase = true)
        }?.value.orEmpty()
    }

    private fun parseDownloadUrls(html: String): Map<String, String> {
        val doc = Jsoup.parse(html)
        val result = mutableMapOf<String, String>()
        doc.select("a.butt[download]").forEach { a ->
            val url = a.attr("href")
            val label = a.text().trim()
            if (url.isNotEmpty()) result[label] = url
        }
        return result
    }
}
