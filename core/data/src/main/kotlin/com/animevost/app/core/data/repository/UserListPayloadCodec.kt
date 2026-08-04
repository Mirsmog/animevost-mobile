package com.animevost.app.core.data.repository

import com.google.gson.JsonParser
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.CRC32

internal class UserListPayloadException(message: String) : IllegalArgumentException(message)

internal class UserListPayloadTooLargeException(
    val actualCharacters: Int,
    val maximumCharacters: Int,
) : IllegalArgumentException(
    "Profile info is $actualCharacters characters, maximum is $maximumCharacters",
)

internal object UserListPayloadCodec {
    const val MAX_PROFILE_INFO_CHARACTERS = 1_000

    private const val LEGACY_PAYLOAD_VERSION = 1
    private const val COMPACT_PAYLOAD_VERSION = 2
    private const val STATUS_COUNT = 5
    private const val CHECKSUM_SIZE_BYTES = 4
    private const val MAX_DECODED_ENTRIES = 10_000

    private val statusCodes = listOf("w", "d", "x", "p", "h")
    private val markerRegex = Regex(pattern = """\[animevost-list:([^]]*)]""")
    private val markerContentRegex = Regex(pattern = """v(\d+):(.*)""")

    enum class Source {
        NONE,
        LEGACY_RAW_V1,
        MARKER_V1,
        MARKER_V2,
    }

    data class DecodedPayload(
        val statuses: Map<Int, String>,
        val source: Source,
    ) {
        val requiresMigration: Boolean
            get() = source == Source.LEGACY_RAW_V1 || source == Source.MARKER_V1
    }

    fun decode(profileInfo: String): DecodedPayload {
        if (profileInfo.isBlank()) return DecodedPayload(emptyMap(), Source.NONE)

        val markers = markerRegex.findAll(profileInfo).toList()
        if (markers.size > 1) {
            throw UserListPayloadException("Profile contains multiple user-list payloads")
        }
        val marker = markers.singleOrNull()
        if (marker != null) {
            val content = markerContentRegex.matchEntire(marker.groupValues[1])
                ?: throw UserListPayloadException("User-list payload marker is invalid")
            val version = content.groupValues[1].toIntOrNull()
                ?: throw UserListPayloadException("User-list payload version is invalid")
            val encoded = content.groupValues[2]
            return when (version) {
                LEGACY_PAYLOAD_VERSION -> DecodedPayload(
                    statuses = decodeLegacyRequired(encoded),
                    source = Source.MARKER_V1,
                )

                COMPACT_PAYLOAD_VERSION -> DecodedPayload(
                    statuses = decodeCompact(encoded),
                    source = Source.MARKER_V2,
                )

                else -> throw UserListPayloadException(
                    "Unsupported user-list payload version: $version",
                )
            }
        }

        val legacy = decodeLegacyOrNull(profileInfo.trim())
        return if (legacy == null) {
            DecodedPayload(emptyMap(), Source.NONE)
        } else {
            DecodedPayload(legacy, Source.LEGACY_RAW_V1)
        }
    }

    fun merge(profileInfo: String, statuses: Map<Int, String>): String {
        val marker = compactMarker(statuses)
        val existing = decode(profileInfo)
        val withoutExistingPayload = when (existing.source) {
            Source.MARKER_V1,
            Source.MARKER_V2,
            -> markerRegex.replace(profileInfo, "")

            Source.LEGACY_RAW_V1 -> ""
            Source.NONE -> profileInfo
        }.trimEnd()

        val merged = if (withoutExistingPayload.isBlank()) {
            marker
        } else {
            "$withoutExistingPayload\n\n$marker"
        }
        val characterCount = merged.codePointCount(0, merged.length)
        if (characterCount > MAX_PROFILE_INFO_CHARACTERS) {
            throw UserListPayloadTooLargeException(
                actualCharacters = characterCount,
                maximumCharacters = MAX_PROFILE_INFO_CHARACTERS,
            )
        }
        return merged
    }

    private fun compactMarker(statuses: Map<Int, String>): String {
        val data = ByteArrayOutputStream()
        writeVarUInt(data, statuses.size.toLong())

        var previousId = 0
        statuses.toSortedMap().forEach { (newsId, statusCode) ->
            if (newsId <= 0) {
                throw UserListPayloadException("Anime newsId must be positive")
            }
            val statusIndex = statusCodes.indexOf(statusCode)
            if (statusIndex < 0) {
                throw UserListPayloadException("Unsupported anime status: $statusCode")
            }
            val delta = newsId - previousId
            if (delta <= 0) {
                throw UserListPayloadException("Anime newsId values must be unique")
            }
            val packed = delta.toLong() * STATUS_COUNT + statusIndex
            writeVarUInt(data, packed)
            previousId = newsId
        }

        val payload = data.toByteArray()
        val checksum = CRC32().apply { update(payload) }.value
        val withChecksum = ByteArrayOutputStream(payload.size + CHECKSUM_SIZE_BYTES).apply {
            write(payload)
            write((checksum ushr 24).toInt() and 0xFF)
            write((checksum ushr 16).toInt() and 0xFF)
            write((checksum ushr 8).toInt() and 0xFF)
            write(checksum.toInt() and 0xFF)
        }.toByteArray()
        val encoded = Base64.getEncoder().withoutPadding().encodeToString(withChecksum)
        return "[animevost-list:v$COMPACT_PAYLOAD_VERSION:$encoded]"
    }

    private fun decodeCompact(encoded: String): Map<Int, String> {
        val bytes = try {
            Base64.getDecoder().decode(encoded)
        } catch (_: IllegalArgumentException) {
            throw UserListPayloadException("Compact user-list payload is not valid Base64")
        }
        if (bytes.size <= CHECKSUM_SIZE_BYTES) {
            throw UserListPayloadException("Compact user-list payload is truncated")
        }

        val payloadSize = bytes.size - CHECKSUM_SIZE_BYTES
        val expectedChecksum = bytes.readUnsignedInt(payloadSize)
        val actualChecksum = CRC32().apply { update(bytes, 0, payloadSize) }.value
        if (actualChecksum != expectedChecksum) {
            throw UserListPayloadException("Compact user-list payload checksum does not match")
        }

        val reader = VarUIntReader(bytes, payloadSize)
        val entryCount = reader.read()
        if (entryCount > MAX_DECODED_ENTRIES) {
            throw UserListPayloadException("Compact user-list payload has too many entries")
        }

        val statuses = linkedMapOf<Int, String>()
        var previousId = 0L
        repeat(entryCount.toInt()) {
            val packed = reader.read()
            val delta = packed / STATUS_COUNT
            val statusIndex = (packed % STATUS_COUNT).toInt()
            if (delta <= 0) {
                throw UserListPayloadException("Compact user-list payload has a duplicate newsId")
            }
            val newsId = previousId + delta
            if (newsId > Int.MAX_VALUE) {
                throw UserListPayloadException("Compact user-list payload newsId is too large")
            }
            statuses[newsId.toInt()] = statusCodes[statusIndex]
            previousId = newsId
        }
        if (!reader.isAtEnd()) {
            throw UserListPayloadException("Compact user-list payload has trailing data")
        }
        return statuses
    }

    private fun decodeLegacyRequired(encoded: String): Map<Int, String> =
        decodeLegacyOrNull(encoded)
            ?: throw UserListPayloadException("Legacy user-list payload is invalid")

    private fun decodeLegacyOrNull(encoded: String): Map<Int, String>? {
        if (encoded.isBlank()) return null
        return try {
            val json = String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)
            val obj = JsonParser.parseString(json).asJsonObject
            if (obj.get("v")?.asInt != LEGACY_PAYLOAD_VERSION) return null
            val values = obj.getAsJsonObject("s") ?: return null
            val result = linkedMapOf<Int, String>()
            values.entrySet().forEach { (url, element) ->
                val status = element.asString
                if (status !in statusCodes) return null
                val newsId = extractAnimeNewsId(url) ?: return null
                val previous = result.put(newsId, status)
                if (previous != null && previous != status) return null
            }
            result.toSortedMap()
        } catch (_: Exception) {
            null
        }
    }

    private fun writeVarUInt(output: ByteArrayOutputStream, input: Long) {
        if (input < 0) throw UserListPayloadException("VarUInt value must not be negative")
        var value = input
        do {
            val current = (value and 0x7F).toInt()
            value = value ushr 7
            output.write(if (value == 0L) current else current or 0x80)
        } while (value != 0L)
    }

    private fun ByteArray.readUnsignedInt(offset: Int): Long =
        ((this[offset].toLong() and 0xFF) shl 24) or
            ((this[offset + 1].toLong() and 0xFF) shl 16) or
            ((this[offset + 2].toLong() and 0xFF) shl 8) or
            (this[offset + 3].toLong() and 0xFF)

    private class VarUIntReader(
        private val bytes: ByteArray,
        private val limit: Int,
    ) {
        private var index = 0

        fun read(): Long {
            var result = 0L
            var shift = 0
            repeat(9) {
                if (index >= limit) {
                    throw UserListPayloadException("Compact user-list payload is truncated")
                }
                val current = bytes[index++].toInt() and 0xFF
                val value = (current and 0x7F).toLong()
                if (value > (Long.MAX_VALUE ushr shift)) {
                    throw UserListPayloadException("Compact user-list payload VarUInt is too large")
                }
                result = result or (value shl shift)
                if (current and 0x80 == 0) return result
                shift += 7
            }
            throw UserListPayloadException("Compact user-list payload VarUInt is too long")
        }

        fun isAtEnd(): Boolean = index == limit
    }
}
