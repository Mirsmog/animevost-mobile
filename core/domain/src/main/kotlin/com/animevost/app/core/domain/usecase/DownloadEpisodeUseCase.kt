package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.VideoSource
import com.animevost.app.core.domain.repository.VideoRepository
import com.animevost.app.core.domain.util.Result
import javax.inject.Inject

/** Resolves download-capable [VideoSource] options for the given [videoId]. */
class DownloadEpisodeUseCase @Inject constructor(
    private val videoRepository: VideoRepository,
) {
    suspend operator fun invoke(videoId: String): Result<List<VideoSource>> =
        videoRepository.getVideoUrls(videoId)
}
