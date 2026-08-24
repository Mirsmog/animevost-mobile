package com.animevost.app.core.data.sdk

import com.animevost.app.core.domain.model.AnimeDetail
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeReleaseStatus
import com.animevost.app.core.domain.model.AnimeType
import com.animevost.app.core.domain.model.CatalogFilter
import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.domain.model.CommentScope
import com.animevost.app.core.domain.model.CommentPage
import com.animevost.app.core.domain.model.Episode
import com.animevost.app.core.domain.model.Genre
import com.animevost.app.core.domain.model.NavData
import com.animevost.app.core.domain.model.RelatedSeries
import com.animevost.app.core.domain.model.Schedule
import com.animevost.app.core.domain.model.ScheduleItem
import com.animevost.app.core.domain.model.SortOption
import com.animevost.app.core.domain.model.VideoSource
import com.animevost.sdk.model.AnimeCategory
import com.animevost.sdk.model.AnimeComment
import com.animevost.sdk.model.AnimeDetails
import com.animevost.sdk.model.AnimeEpisode
import com.animevost.sdk.model.CatalogSort
import com.animevost.sdk.model.CommentAction
import com.animevost.sdk.model.NavigationData
import com.animevost.sdk.model.ScheduleDay
import java.net.URI
import com.animevost.sdk.model.AnimePreview as SdkAnimePreview
import com.animevost.sdk.model.CatalogFilter as SdkCatalogFilter
import com.animevost.sdk.model.CommentPage as SdkCommentPage
import com.animevost.sdk.model.RelatedSeries as SdkRelatedSeries
import com.animevost.sdk.model.VideoSource as SdkVideoSource

internal fun SdkAnimePreview.toDomain(): AnimePreview =
    AnimePreview(
        id = id,
        title = title,
        titleOriginal = originalTitle.orEmpty(),
        posterUrl = posterUrl.orEmpty(),
        episodeInfo = episodeInfo.orEmpty(),
        url = url,
        type = url.toAnimeTypeLabel(),
        rating = rating ?: 0.0,
        viewCount = viewCount ?: 0,
        commentCount = commentCount ?: 0,
        releaseStatus = AnimeReleaseStatus.fromCategories(categories.map { it.title }),
    )

internal fun AnimeDetails.toDomain(totalCommentPages: Int): AnimeDetail {
    val categoryUrls = categories.associateByNormalizedTitle()
    return AnimeDetail(
        id = id,
        url = url,
        title = title,
        titleOriginal = originalTitle.orEmpty(),
        titleAlternative = alternativeTitle.orEmpty(),
        posterUrl = posterUrl.orEmpty(),
        year = year.orEmpty(),
        genres = genres.map { name ->
            Genre(
                id = 0,
                name = name,
                url = categoryUrls[name.normalizedTitle()].orEmpty(),
            )
        },
        type = type.toAnimeType(),
        episodeCount = episodeCount.orEmpty(),
        director = director.orEmpty(),
        rating = rating ?: 0.0,
        voteCount = voteCount ?: 0,
        viewCount = viewCount ?: 0,
        commentCount = commentCount ?: 0,
        totalCommentPages = totalCommentPages.coerceAtLeast(1),
        description = description.orEmpty(),
        publishDate = publishedDate.orEmpty(),
        categories = categories.map { it.title },
        relatedAnime = emptyList(),
        relatedSeries = relatedSeries.map { it.toDomain() },
        episodes = episodes.map { it.toDomain() },
        releaseStatus = AnimeReleaseStatus.fromCategories(categories.map { it.title }),
        episodeInfo = episodeInfo.orEmpty(),
    )
}

internal fun NavigationData.toDomain(): NavData =
    NavData(
        genres = genres.mapIndexed { index, link ->
            Genre(
                id = index,
                name = link.title,
                url = link.path,
            )
        },
        years = years.map { it.title },
    )

internal fun ScheduleDay.toDomainOrNull(): Schedule? {
    if (entries.isEmpty()) return null
    return Schedule(
        dayOfWeek = weekday.displayName,
        items = entries.map { entry ->
            ScheduleItem(
                title = entry.title,
                time = entry.timeLabel.orEmpty(),
                url = entry.url,
            )
        },
    )
}

internal fun AnimeComment.toDomain(): Comment {
    val quote = quotes.firstOrNull()
    return Comment(
        id = id,
        author = author.name,
        date = createdAtLabel.orEmpty(),
        text = bodyHtml,
        avatar = avatarUrl.orEmpty(),
        quotedAuthor = quote?.authorName.orEmpty(),
        quotedText = quote?.bodyText.orEmpty(),
        depth = depth,
        ordinal = ordinal,
        authorCommentCount = authorCommentCount,
        canReply = CommentAction.REPLY in actions,
        canReport = CommentAction.REPORT in actions,
        canDelete = CommentAction.DELETE in actions,
        scope = episodeNumber?.let { CommentScope.Episode(it) } ?: CommentScope.Anime,
    )
}

internal fun SdkCommentPage.toDomain(): CommentPage =
    CommentPage(
        comments = comments.map { it.toDomain() },
        currentPage = currentPage,
        totalPages = totalPages?.coerceAtLeast(currentPage) ?: currentPage,
    )

internal fun SdkVideoSource.toDomain(): VideoSource =
    VideoSource(
        quality = quality,
        url = url,
        downloadUrl = downloadUrl.orEmpty(),
    )

internal fun CatalogFilter.toSdk(): SdkCatalogFilter =
    SdkCatalogFilter(
        path = toSdkPath(),
        sortBy = sortBy.toSdk(),
        sortAscending = sortAscending,
    )

private fun CatalogFilter.toSdkPath(): String? {
    path?.trim()?.takeIf { it.isNotBlank() }?.let { return it.toRelativePath() }
    genre?.url?.trim()?.takeIf { it.isNotBlank() }?.let { return it.toRelativePath() }
    type?.urlSlug?.takeIf { it.isNotBlank() }?.let { return "tip/$it/" }
    year?.trim()?.takeIf { it.isNotBlank() }?.let { return "god/$it/" }
    return null
}

private fun SortOption.toSdk(): CatalogSort =
    when (this) {
        SortOption.DATE -> CatalogSort.DATE
        SortOption.RATING -> CatalogSort.RATING
        SortOption.VIEWS -> CatalogSort.VIEWS
        SortOption.COMMENTS -> CatalogSort.COMMENTS
        SortOption.TITLE -> CatalogSort.TITLE
    }

private fun AnimeEpisode.toDomain(): Episode =
    Episode(
        name = name,
        videoId = videoId,
        thumbnailUrl = thumbnailUrl.orEmpty(),
        number = number,
    )

private fun SdkRelatedSeries.toDomain(): RelatedSeries =
    RelatedSeries(
        title = title,
        url = url,
        description = description.orEmpty(),
    )

private fun String?.toAnimeType(): AnimeType {
    val value = this?.trim().orEmpty()
    return AnimeType.entries.firstOrNull { it.displayName.equals(value, ignoreCase = true) }
        ?: AnimeType.UNKNOWN
}

private fun String.toAnimeTypeLabel(): String {
    val slug = Regex("""/tip/([^/]+)/""").find(this)?.groupValues?.get(1)?.lowercase().orEmpty()
    return AnimeType.entries.firstOrNull { it.urlSlug == slug }?.displayName.orEmpty()
}

private fun String.toRelativePath(): String =
    runCatching {
        URI(this).rawPath.trimStart('/')
    }.getOrDefault(trimStart('/'))

private fun List<AnimeCategory>.associateByNormalizedTitle(): Map<String, String> =
    associate { it.title.normalizedTitle() to it.url }

private fun String.normalizedTitle(): String =
    trim().lowercase()
