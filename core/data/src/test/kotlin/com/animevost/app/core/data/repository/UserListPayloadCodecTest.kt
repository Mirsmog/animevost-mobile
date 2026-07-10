package com.animevost.app.core.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserListPayloadCodecTest {

    @Test
    fun `merge preserves profile text and round trips statuses`() {
        val statuses = mapOf(
            "tip/tv/1-first.html" to "w",
            "tip/ova/2-second.html" to "p",
        )

        val merged = UserListPayloadCodec.merge("Обычный текст профиля", statuses)

        assertTrue(merged.startsWith("Обычный текст профиля\n\n"))
        assertEquals(statuses, UserListPayloadCodec.decode(merged))
    }

    @Test
    fun `merge replaces existing marker without duplicating it`() {
        val first = UserListPayloadCodec.merge("Текст", mapOf("first" to "w"))
        val second = UserListPayloadCodec.merge(first, mapOf("second" to "d"))

        assertEquals(1, "[animevost-list:".toRegex(RegexOption.LITERAL).findAll(second).count())
        assertEquals(mapOf("second" to "d"), UserListPayloadCodec.decode(second))
    }

    @Test
    fun `legacy raw payload is migrated without exposing duplicate payload`() {
        val legacy = UserListPayloadCodec.merge("", mapOf("first" to "w"))
            .substringAfterLast(':')
            .removeSuffix("]")

        val migrated = UserListPayloadCodec.merge(legacy, mapOf("second" to "p"))

        assertFalse(migrated.startsWith(legacy))
        assertEquals(mapOf("second" to "p"), UserListPayloadCodec.decode(migrated))
    }
}
