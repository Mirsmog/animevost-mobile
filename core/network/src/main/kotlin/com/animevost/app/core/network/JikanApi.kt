package com.animevost.app.core.network

import retrofit2.http.GET
import retrofit2.http.Query

interface JikanApi {

    @GET("v4/anime")
    suspend fun searchAnime(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
    ): JikanSearchResponse
}

data class JikanSearchResponse(
    val data: List<JikanAnime> = emptyList(),
)

data class JikanAnime(
    val mal_id: Int,
    val title: String = "",
    val title_japanese: String? = null,
    val type: String? = null,
    val year: Int? = null,
)
