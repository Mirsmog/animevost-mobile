package com.animevost.app.core.data.repository

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserListPayloadCodecTest {

    @Test
    fun `merge preserves profile text and round trips compact statuses`() {
        val statuses = mapOf(
            1 to "w",
            2 to "p",
        )

        val merged = UserListPayloadCodec.merge("Обычный текст профиля", statuses)
        val decoded = UserListPayloadCodec.decode(merged)

        assertTrue(merged.startsWith("Обычный текст профиля\n\n"))
        assertTrue(merged.contains("[animevost-list:v2:"))
        assertEquals(statuses, decoded.statuses)
        assertEquals(UserListPayloadCodec.Source.MARKER_V2, decoded.source)
    }

    @Test
    fun `merge replaces existing marker without duplicating it`() {
        val first = UserListPayloadCodec.merge("Текст", mapOf(1 to "w"))
        val second = UserListPayloadCodec.merge(first, mapOf(2 to "d"))

        assertEquals(1, "[animevost-list:".toRegex(RegexOption.LITERAL).findAll(second).count())
        assertEquals(mapOf(2 to "d"), UserListPayloadCodec.decode(second).statuses)
    }

    @Test
    fun `legacy raw payload is decoded and migrated to compact marker`() {
        val legacy = legacyPayload(
            "tip/tv/1-first.html" to "w",
            "tip/ova/2-second.html" to "p",
        )

        val decoded = UserListPayloadCodec.decode(legacy)
        val migrated = UserListPayloadCodec.merge(legacy, decoded.statuses)

        assertEquals(mapOf(1 to "w", 2 to "p"), decoded.statuses)
        assertTrue(decoded.requiresMigration)
        assertFalse(migrated.startsWith(legacy))
        assertTrue(migrated.startsWith("[animevost-list:v2:"))
        assertEquals(decoded.statuses, UserListPayloadCodec.decode(migrated).statuses)
    }

    @Test
    fun `legacy marker is decoded by news id`() {
        val encoded = legacyPayload("tip/tv/3970-title.html" to "h")
        val decoded = UserListPayloadCodec.decode("Bio\n\n[animevost-list:v1:$encoded]")

        assertEquals(mapOf(3970 to "h"), decoded.statuses)
        assertEquals(UserListPayloadCodec.Source.MARKER_V1, decoded.source)
        assertTrue(decoded.requiresMigration)
    }

    @Test
    fun `compact payload fits seven hundred nearby anime ids`() {
        val statuses = (1..700).associate { newsId ->
            newsId * 5 to statusCodes[newsId % statusCodes.size]
        }

        val merged = UserListPayloadCodec.merge("", statuses)

        assertTrue(merged.length <= UserListPayloadCodec.MAX_PROFILE_INFO_CHARACTERS)
        assertEquals(statuses, UserListPayloadCodec.decode(merged).statuses)
    }

    @Test
    fun `corrupted compact payload is rejected instead of becoming empty`() {
        val merged = UserListPayloadCodec.merge("", mapOf(3970 to "w"))
        val payloadStart = merged.indexOf(':', merged.indexOf("v2")) + 1
        val replacement = if (merged[payloadStart] == 'A') "B" else "A"
        val corrupted = merged.replaceRange(payloadStart, payloadStart + 1, replacement)

        assertFailsWith<UserListPayloadException> {
            UserListPayloadCodec.decode(corrupted)
        }
    }

    @Test
    fun `merge refuses to overwrite corrupted compact payload`() {
        val existing = UserListPayloadCodec.merge("", mapOf(3970 to "w"))
        val payloadStart = existing.indexOf(':', existing.indexOf("v2")) + 1
        val replacement = if (existing[payloadStart] == 'A') "B" else "A"
        val corrupted = existing.replaceRange(payloadStart, payloadStart + 1, replacement)

        assertFailsWith<UserListPayloadException> {
            UserListPayloadCodec.merge(corrupted, mapOf(4000 to "p"))
        }
    }

    @Test
    fun `merge refuses to replace multiple payload markers`() {
        val first = UserListPayloadCodec.merge("", mapOf(1 to "w"))
        val second = UserListPayloadCodec.merge("", mapOf(2 to "d"))

        assertFailsWith<UserListPayloadException> {
            UserListPayloadCodec.merge("$first\n$second", mapOf(3 to "p"))
        }
    }

    @Test
    fun `profile info over server limit is rejected before upload`() {
        assertFailsWith<UserListPayloadTooLargeException> {
            UserListPayloadCodec.merge("я".repeat(990), mapOf(3970 to "w"))
        }
    }

    @Test
    fun `invalid legacy marker is rejected instead of becoming empty`() {
        assertFailsWith<UserListPayloadException> {
            UserListPayloadCodec.decode("[animevost-list:v1:not_base64]")
        }
    }

    @Test
    fun `malformed marker is rejected instead of becoming ordinary profile text`() {
        assertFailsWith<UserListPayloadException> {
            UserListPayloadCodec.decode("[animevost-list:broken]")
        }
    }

    @Test
    fun `ordinary profile text has no remote list`() {
        val decoded = UserListPayloadCodec.decode("Обычный текст профиля")

        assertEquals(emptyMap(), decoded.statuses)
        assertEquals(UserListPayloadCodec.Source.NONE, decoded.source)
    }

    private fun legacyPayload(vararg statuses: Pair<String, String>): String {
        val values = statuses.joinToString(",") { (url, status) ->
            "\"$url\":\"$status\""
        }
        val json = """{"v":1,"s":{$values}}"""
        return Base64.getEncoder().encodeToString(json.toByteArray())
    }

    private companion object {
        val statusCodes = listOf("w", "d", "x", "p", "h")
    }
}
