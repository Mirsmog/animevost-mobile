package com.animevost.app.core.data.mapper

import com.animevost.app.core.data.db.HistoryEntity
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.Episode

internal fun HistoryEntity.toAnimePreview(): AnimePreview = AnimePreview(
    id = animeId,
    title = title,
    titleOriginal = titleOriginal,
    posterUrl = posterUrl,
    episodeInfo = episodeInfo,
    url = url,
)

internal fun AnimePreview.toHistoryEntity(episode: Episode): HistoryEntity = HistoryEntity(
    animeId = id,
    title = title,
    titleOriginal = titleOriginal,
    posterUrl = posterUrl,
    episodeInfo = episodeInfo,
    url = url,
    episodeName = episode.name,
    episodeVideoId = episode.videoId,
)
