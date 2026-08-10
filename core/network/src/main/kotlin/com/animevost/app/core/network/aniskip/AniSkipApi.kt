package com.animevost.app.core.network.aniskip

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AniSkipApi {
    @GET("v2/skip-times/{malId}/{episodeNumber}")
    suspend fun getSkipTimes(
        @Path("malId") malId: Int,
        @Path("episodeNumber") episodeNumber: Int,
        @Query("types[]") types: List<String>,
        @Query("episodeLength") episodeLength: Int = 0,
    ): AniSkipResponse
}

data class AniSkipResponse(
    @SerializedName("found") val found: Boolean = false,
    @SerializedName("results") val results: List<AniSkipResult> = emptyList(),
)

data class AniSkipResult(
    @SerializedName("interval") val interval: AniSkipInterval,
    @SerializedName(value = "skipType", alternate = ["skip_type"])
    val skipType: String,
    @SerializedName(value = "episodeLength", alternate = ["episode_length"])
    val episodeLength: Double? = null,
)

data class AniSkipInterval(
    @SerializedName(value = "startTime", alternate = ["start_time"])
    val startTime: Double,
    @SerializedName(value = "endTime", alternate = ["end_time"])
    val endTime: Double,
)
