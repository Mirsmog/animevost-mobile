package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.VideoSource
import com.animevost.app.core.domain.repository.VideoRepository
import com.animevost.app.core.domain.util.Result
import com.animevost.app.core.data.sdk.toDomain
import com.animevost.sdk.AnimeVostClient
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val client: AnimeVostClient,
) : VideoRepository {

    override suspend fun getVideoUrls(videoId: String): Result<List<VideoSource>> {
        return try {
            Result.Success(client.getVideoSources(videoId).map { it.toDomain() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
}
