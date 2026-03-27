package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.VideoSource
import com.animevost.app.core.domain.repository.VideoRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor() : VideoRepository {

    override suspend fun getVideoUrls(videoId: String): List<VideoSource> {
        TODO("Implement: fetch video player page, extract video URLs")
    }
}
