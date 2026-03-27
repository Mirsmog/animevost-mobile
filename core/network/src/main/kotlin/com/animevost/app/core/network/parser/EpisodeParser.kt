package com.animevost.app.core.network.parser

import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.network.DleEndpoints
import javax.inject.Inject

class EpisodeParser @Inject constructor() {

    private val dataBlockRegex = Regex("""var\s+data\s*=\s*\{([^}]+)}""")
    private val entryRegex = Regex(""""([^"]+)"\s*:\s*"(\d+)"""")

    fun parse(html: String): List<Episode> {
        val dataMatch = dataBlockRegex.find(html) ?: return emptyList()
        val dataContent = dataMatch.groupValues[1]

        return entryRegex.findAll(dataContent).map { match ->
            val name = match.groupValues[1]
            val videoId = match.groupValues[2]
            Episode(
                name = name,
                videoId = videoId,
                thumbnailUrl = "${DleEndpoints.MEDIA_THUMBNAIL_BASE}$videoId.jpg",
            )
        }.toList()
    }
}
