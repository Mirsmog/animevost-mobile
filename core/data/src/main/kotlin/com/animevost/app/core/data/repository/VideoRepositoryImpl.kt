package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.VideoSource
import com.animevost.app.core.domain.repository.VideoRepository
import com.animevost.app.core.network.DleEndpoints
import com.animevost.app.core.network.HtmlFetcher
import com.animevost.app.core.network.parser.VideoUrlParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val htmlFetcher: HtmlFetcher,
    private val videoUrlParser: VideoUrlParser,
) : VideoRepository {

    override suspend fun getVideoUrls(videoId: String): List<VideoSource> {
        val url = "${DleEndpoints.BASE_URL}${DleEndpoints.PLAYER_FRAME}?play=$videoId"
        val html = htmlFetcher.fetch(url)
        return videoUrlParser.parse(html)
    }
}
