package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.VideoSource
import com.animevost.app.core.domain.util.Result

/** Provides playback URLs for a given video resource. */
interface VideoRepository {
    /** Returns available [VideoSource] options for [videoId], or [Result.Error] on failure. */
    suspend fun getVideoUrls(videoId: String): Result<List<VideoSource>>
}
