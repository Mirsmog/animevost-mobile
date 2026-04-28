package com.animevost.app.core.network.alloha

import com.google.gson.annotations.SerializedName

/** Response from yummyanime ajax controller (`mod=alloha-player&action=iframe&id=...`). */
data class YummyIframeAjaxResponse(
    @SerializedName("data") val iframeUrl: String? = null,
    @SerializedName("status") val status: String? = null,
)

/** Response of `POST /bnsi/movies/{id}` — only fields we use. */
data class AllohaMovieResponse(
    @SerializedName("skipTime") val skipTime: String? = null,
    @SerializedName("removeTime") val removeTime: String? = null,
)

/** Parsed view of an Alloha iframe HTML page. */
data class AllohaIframeData(
    val token: String,
    val tokenMovie: String,
    val seed: String,
    /** seasons → episodes → translation key (`t213`, `t102`, ...) → translation entry. */
    val translations: Map<Int, Map<Int, Map<String, AllohaTranslation>>>,
)

/** One translation entry inside the Alloha iframe `fileList.all[season][episode][tNNN]`. */
data class AllohaTranslation(
    val id: Long,
    val idFile: Long?,
    val translation: String,
    val idTranslation: Int,
)
