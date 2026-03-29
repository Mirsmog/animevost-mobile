package com.animevost.app.core.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AniSkipApi {

    @GET("v2/skip-times/{mal_id}/{episode}")
    suspend fun getSkipTimes(
        @Path("mal_id") malId: Int,
        @Path("episode") episode: Int,
        @Query("types[]") types: List<String> = listOf("op", "ed"),
    ): AniSkipResponse
}

data class AniSkipResponse(
    val found: Boolean = false,
    val results: List<AniSkipResult> = emptyList(),
)

data class AniSkipResult(
    val interval: AniSkipInterval,
    val skipType: String,
    val episodeLength: Double = 0.0,
)

data class AniSkipInterval(
    val startTime: Double,
    val endTime: Double,
)
