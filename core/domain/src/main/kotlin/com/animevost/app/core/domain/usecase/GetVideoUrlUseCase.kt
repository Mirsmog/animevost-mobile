package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.VideoSource
import com.animevost.app.core.domain.repository.VideoRepository
import com.animevost.app.core.domain.util.Result
import javax.inject.Inject

/** Returns available [VideoSource] options for the given [videoId]. */
class GetVideoUrlUseCase @Inject constructor(
    private val repository: VideoRepository,
) {
    suspend operator fun invoke(videoId: String): Result<List<VideoSource>> =
        repository.getVideoUrls(videoId)
}
