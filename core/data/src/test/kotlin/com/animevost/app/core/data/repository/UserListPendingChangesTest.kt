package com.animevost.app.core.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserListPendingChangesTest {
    @Test
    fun `local changes preserve unrelated desktop changes and apply removals`() {
        val remote = mapOf(1 to "d", 2 to "w", 3 to "p")
        val local = mapOf(2 to "h", 3 to null, 4 to "x")
        assertEquals(mapOf(1 to "d", 2 to "h", 4 to "x"), applyUserListChanges(remote, local))
    }

    @Test
    fun `pending removals survive persistence and repeated delivery`() {
        val changes = mapOf(3970 to null, 1 to "d")
        val restored = decodePendingUserListChanges(encodePendingUserListChanges(changes))
        assertEquals(changes, restored)
        val once = applyUserListChanges(mapOf(3970 to "w", 2 to "p"), restored)
        assertEquals(mapOf(1 to "d", 2 to "p"), once)
        assertEquals(once, applyUserListChanges(once, restored))
    }

    @Test
    fun `invalid persisted changes fail without replacing the remote list`() {
        assertFailsWith<UserListPayloadException> { decodePendingUserListChanges("""{"0":"w"}""") }
        assertFailsWith<UserListPayloadException> { decodePendingUserListChanges("""{"1":"unknown"}""") }
    }
}
