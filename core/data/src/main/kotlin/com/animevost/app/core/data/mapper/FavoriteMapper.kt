package com.animevost.app.core.data.mapper

import com.animevost.app.core.data.db.FavoriteEntity
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeReleaseStatus

internal fun FavoriteEntity.toAnimePreview(): AnimePreview = AnimePreview(
    id = newsId,
    title = title,
    titleOriginal = titleOriginal,
    posterUrl = posterUrl,
    episodeInfo = episodeInfo,
    url = url,
    releaseStatus = AnimeReleaseStatus.fromStorage(releaseStatus),
)

internal fun AnimePreview.toFavoriteEntity(): FavoriteEntity = FavoriteEntity(
    newsId = id,
    title = title,
    titleOriginal = titleOriginal,
    posterUrl = posterUrl,
    episodeInfo = episodeInfo,
    url = url,
    releaseStatus = releaseStatus.name,
)
