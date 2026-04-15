package com.animevost.app.core.network.parser

import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses the animevost profile HTML page to extract:
 * - The `info` field value (our Base64-encoded JSON payload)
 * - Form fields required to write back: dle_allow_hash, fullname, land, email, id
 */
@Singleton
class UserInfoParser @Inject constructor() {

    data class Result(
        /** Raw value of the `info` textarea (may be empty or non-base64). */
        val rawInfo: String,
        val userId: String,
        val hash: String,
        val fullname: String,
        val land: String,
        val email: String,
        /** Absolute URL of the user's avatar image, or empty string if not found. */
        val avatarUrl: String = "",
    )

    fun parse(html: String, baseUrl: String = ""): Result {
        val doc = if (baseUrl.isNotBlank()) Jsoup.parse(html, baseUrl) else Jsoup.parse(html)
        val form = doc.selectFirst("form#userinfo")

        val rawInfo = form?.selectFirst("textarea[name=info]")?.text()?.trim() ?: ""
        val userId = form?.selectFirst("input[name=id]")?.`val`()?.trim() ?: ""
        val hash = form?.selectFirst("input[name=dle_allow_hash]")?.`val`()?.trim() ?: ""
        val fullname = form?.selectFirst("input[name=fullname]")?.`val`()?.trim() ?: ""
        val land = form?.selectFirst("input[name=land]")?.`val`()?.trim() ?: ""
        val email = form?.selectFirst("input[name=email]")?.`val`()?.trim() ?: ""

        // Try common DLE avatar selectors; absUrl works because Jsoup has a base URI.
        val avatarUrl = doc
            .select("div.ava img, div.avatar img, div.user-ava img, .userinfo-ava img, div.img_profile img")
            .firstOrNull()?.absUrl("src")
            ?.takeIf { it.isNotBlank() }
            ?: doc.select("img[src*=avatar], img[src*=/ava]")
                .firstOrNull()?.absUrl("src")
                ?.takeIf { it.isNotBlank() }
            ?: ""

        return Result(
            rawInfo = rawInfo,
            userId = userId,
            hash = hash,
            fullname = fullname,
            land = land,
            email = email,
            avatarUrl = avatarUrl,
        )
    }
}
