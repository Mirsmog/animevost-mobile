package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.VideoSource
import com.animevost.app.core.domain.repository.VideoRepository
import com.animevost.app.core.domain.util.Result
import com.animevost.app.core.network.DleEndpoints
import com.animevost.app.core.network.HtmlFetcher
import com.animevost.app.core.network.parser.VideoUrlParser
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val htmlFetcher: HtmlFetcher,
    private val videoUrlParser: VideoUrlParser,
) : VideoRepository {

    override suspend fun getVideoUrls(videoId: String): Result<List<VideoSource>> {
        return try {
            val url = "${DleEndpoints.BASE_URL}${DleEndpoints.PLAYER_FRAME}?play=$videoId"
            val html = htmlFetcher.fetch(url)
            Result.Success(videoUrlParser.parse(html))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
}
