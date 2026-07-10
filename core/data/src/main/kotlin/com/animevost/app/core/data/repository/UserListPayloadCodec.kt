package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.AnimeStatus
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.Base64

internal object UserListPayloadCodec {
    private const val PAYLOAD_VERSION = 1
    private val markerRegex = Regex(
        pattern = """\[animevost-list:v(\d+):([A-Za-z0-9+/=]+)]""",
    )

    fun decode(profileInfo: String): Map<String, String> {
        if (profileInfo.isBlank()) return emptyMap()
        val marker = markerRegex.find(profileInfo)
        val encoded = marker?.groupValues?.get(2) ?: profileInfo.trim()
        return decodeEncoded(encoded) ?: emptyMap()
    }

    fun merge(profileInfo: String, statuses: Map<String, String>): String {
        val encoded = encode(statuses)
        val marker = "[animevost-list:v$PAYLOAD_VERSION:$encoded]"
        val withoutExistingPayload = when {
            markerRegex.containsMatchIn(profileInfo) -> markerRegex.replace(profileInfo, "")
            decodeEncoded(profileInfo.trim()) != null -> ""
            else -> profileInfo
        }.trimEnd()

        return if (withoutExistingPayload.isBlank()) {
            marker
        } else {
            "$withoutExistingPayload\n\n$marker"
        }
    }

    private fun encode(statuses: Map<String, String>): String {
        val values = JsonObject()
        statuses.forEach { (url, status) -> values.addProperty(url, status) }
        val json = JsonObject().apply {
            addProperty("v", PAYLOAD_VERSION)
            add("s", values)
        }.toString()
        return Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
    }

    private fun decodeEncoded(encoded: String): Map<String, String>? {
        if (encoded.isBlank()) return null
        return try {
            val json = String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)
            val obj = JsonParser.parseString(json).asJsonObject
            if (obj.get("v")?.asInt != PAYLOAD_VERSION) return null
            val values = obj.getAsJsonObject("s") ?: return null
            values.entrySet().mapNotNull { (url, element) ->
                val code = runCatching { element.asString }.getOrNull() ?: return@mapNotNull null
                if (AnimeStatus.fromCode(code) != null) url to code else null
            }.toMap()
        } catch (_: Exception) {
            null
        }
    }
}
