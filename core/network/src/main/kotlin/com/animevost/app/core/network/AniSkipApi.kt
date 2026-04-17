package com.animevost.app.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AniSkipApi {
    @GET("v2/skip-times/{malId}/{episodeNumber}")
    suspend fun getSkipTimes(
        @Path("malId") malId: Int,
        @Path("episodeNumber") episodeNumber: Int,
        @Query("types[]") types: List<String> = listOf("op", "ed"),
        @Query("episodeLength") episodeLength: Int = 0,
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
