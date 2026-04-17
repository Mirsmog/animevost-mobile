package com.animevost.app.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

interface AniSkipApi {
    @GET("v2/skip-times/{malId}/{episodeNumber}?types[]=op&types[]=ed&episodeLength=0")
    suspend fun getSkipTimes(
        @Path("malId") malId: Int,
        @Path("episodeNumber") episodeNumber: Int,
    ): AniSkipResponse
}

data class AniSkipResponse(val found: Boolean = false, val results: List<AniSkipResult> = emptyList())
data class AniSkipResult(
    val interval: AniSkipInterval,
    @SerializedName("skipType") val skip_type: String,
)
data class AniSkipInterval(
    @SerializedName("startTime") val start_time: Double,
    @SerializedName("endTime") val end_time: Double,
)
