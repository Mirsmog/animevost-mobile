package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.VideoSource
import com.animevost.app.core.domain.repository.VideoRepository
import javax.inject.Inject

class DownloadEpisodeUseCase @Inject constructor(
    private val videoRepository: VideoRepository,
) {
    suspend operator fun invoke(videoId: String): List<VideoSource> {
        return videoRepository.getVideoUrls(videoId)
    }
}
