package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.VideoSource

interface VideoRepository {
    suspend fun getVideoUrls(videoId: String): List<VideoSource>
}
