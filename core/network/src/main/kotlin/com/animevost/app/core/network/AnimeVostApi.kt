package com.animevost.app.core.network

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AnimeVostApi {

    @GET(DleEndpoints.AJAX_RATING)
    suspend fun submitRating(
        @Query("go_rate") rating: Int,
        @Query("news_id") newsId: Int,
        @Query("skin") skin: String = DleEndpoints.DEFAULT_SKIN,
    ): JsonObject

    @GET(DleEndpoints.AJAX_COMMENTS)
    suspend fun getComments(
        @Query("cstart") page: Int,
        @Query("news_id") newsId: Int,
        @Query("skin") skin: String = DleEndpoints.DEFAULT_SKIN,
    ): JsonObject

    @FormUrlEncoded
    @POST(DleEndpoints.AJAX_ADD_COMMENT)
    suspend fun addComment(
        @Field("post_id") postId: Int,
        @Field("comments") text: String,
        @Field("name") name: String,
        @Field("mail") mail: String = "",
        @Field("editor_mode") editorMode: String = "",
        @Field("skin") skin: String = DleEndpoints.DEFAULT_SKIN,
        @Field("sec_code") secCode: String = "",
        @Field("question_answer") questionAnswer: String = "",
        @Field("recaptcha_response_field") recaptchaResponse: String = "",
        @Field("recaptcha_challenge_field") recaptchaChallenge: String = "",
        @Field("allow_subscribe") allowSubscribe: String = "0",
    ): ResponseBody

    @GET(DleEndpoints.AJAX_FAVORITES)
    suspend fun toggleFavorite(
        @Query("fav_id") favId: Int,
        @Query("action") action: String,
        @Query("skin") skin: String = DleEndpoints.DEFAULT_SKIN,
    ): JsonObject

    @FormUrlEncoded
    @POST(DleEndpoints.LOGIN)
    suspend fun login(
        @Field("login_name") username: String,
        @Field("login_password") password: String,
        @Field("login") submit: String = "submit",
    ): ResponseBody
}
