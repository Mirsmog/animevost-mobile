package com.animevost.app.core.network

import retrofit2.http.GET
import retrofit2.http.Query

interface JikanApi {
    @GET("v4/anime")
    suspend fun searchAnime(
        @Query("q") query: String,
        @Query("limit") limit: Int = 3,
    ): JikanSearchResponse
}

data class JikanSearchResponse(val data: List<JikanAnimeData> = emptyList())
data class JikanAnimeData(val mal_id: Int, val title: String = "", val title_english: String? = null)
