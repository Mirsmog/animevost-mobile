package com.animevost.app.core.network.alloha

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

/** Yummyanime AJAX endpoint that resolves an Alloha iframe URL. */
interface YummyAnimeApi {
    @GET("engine/ajax/controller.php")
    @Headers("X-Requested-With: XMLHttpRequest")
    suspend fun getAllohaIframeUrl(
        @Query("mod") mod: String = "alloha-player",
        @Query("url") url: Int = 1,
        @Query("action") action: String = "iframe",
        @Query("id") yummyAnimeId: Int,
    ): YummyIframeAjaxResponse

    /**
     * One-time warm-up GET on yummyanime root so the server issues a PHPSESSID
     * cookie. Without it the AJAX controller responds `{"success":false}`.
     */
    @GET("/")
    suspend fun warmUp(): retrofit2.Response<okhttp3.ResponseBody>
}

/** Alloha JSON endpoint that returns skipTime + media URLs for a given idFile. */
interface AllohaApi {
    @POST
    @FormUrlEncoded
    suspend fun getMovieData(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Field("token") token: String,
        @Field("av1") av1: Boolean = true,
        @Field("autoplay") autoplay: Int = 0,
        @Field("audio") audio: String = "",
        @Field("subtitle") subtitle: String = "",
    ): retrofit2.Response<AllohaMovieResponse>
}
