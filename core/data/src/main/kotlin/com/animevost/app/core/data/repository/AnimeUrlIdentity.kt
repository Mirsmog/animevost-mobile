package com.animevost.app.core.data.repository

import java.net.URI

private val NEWS_ID_IN_SLUG = Regex(
    pattern = """(?:^|/)(\d+)-[^/?#]+\.html(?:[?#].*)?$""",
    option = RegexOption.IGNORE_CASE,
)
private val NEWS_ID_IN_QUERY = Regex(
    pattern = """(?:[?&])newsid=(\d+)(?:&|$)""",
    option = RegexOption.IGNORE_CASE,
)

internal fun extractAnimeNewsId(value: String): Int? {
    val normalized = value.trim()
    val rawId = NEWS_ID_IN_QUERY.find(normalized)?.groupValues?.get(1)
        ?: NEWS_ID_IN_SLUG.find(normalized)?.groupValues?.get(1)
        ?: return null
    return rawId.toIntOrNull()?.takeIf { it > 0 }
}

internal fun animeResolverPath(newsId: Int): String {
    require(newsId > 0) { "newsId must be positive" }
    return "index.php?newsid=$newsId"
}

internal fun normalizeAnimePath(value: String): String {
    val normalized = value.trim()
    NEWS_ID_IN_QUERY.find(normalized)
        ?.groupValues
        ?.get(1)
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
        ?.let { return animeResolverPath(it) }

    return try {
        URI(normalized).path.trimStart('/').ifBlank { normalized.trimStart('/') }
    } catch (_: Exception) {
        normalized.trimStart('/')
    }
}
