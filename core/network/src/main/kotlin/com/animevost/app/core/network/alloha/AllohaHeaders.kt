package com.animevost.app.core.network.alloha

/**
 * Static set of headers required by the Alloha `POST /bnsi/movies/...` endpoint.
 *
 * Empirically the server validates `sec-ch-ua*` along with the standard CORS
 * fetch metadata headers; missing any of them returns a generic 404 with body
 * "контент не найден" — there is no clearer error code from the server.
 */
object AllohaHeaders {
    fun forMoviePost(
        referer: String,
        origin: String,
        borth: String,
    ): Map<String, String> = linkedMapOf(
        "User-Agent" to BorthCodec.userAgent(),
        "Accept" to "*/*",
        "Accept-Language" to "ru,en;q=0.9",
        "Referer" to referer,
        "Origin" to origin,
        "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
        "X-Requested-With" to "XMLHttpRequest",
        "Sec-Fetch-Dest" to "empty",
        "Sec-Fetch-Mode" to "cors",
        "Sec-Fetch-Site" to "same-origin",
        "sec-ch-ua" to "\"Chromium\";v=\"109\", \"Not:A-Brand\";v=\"24\"",
        "sec-ch-ua-mobile" to "?1",
        "sec-ch-ua-platform" to "\"Android\"",
        "Borth" to borth,
    )
}
