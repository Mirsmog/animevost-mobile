package com.animevost.app.feature.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class ResumePositionTest {
    @Test
    fun `a pending resume waits until the episode is ready`() {
        val pending = ResumePosition.Pending(30_000L)
        assertFalse(pending.consumeWhenReady(isReady = false, canSeek = false) { fail("Too early to seek") })
    }

    @Test
    fun `ready streams without seeking can save progress again`() {
        val pending = ResumePosition.Pending(30_000L)
        assertTrue(pending.consumeWhenReady(isReady = true, canSeek = false) { fail("Stream cannot seek") })
    }

    @Test
    fun `normal resume and explicit replay seek to their requested positions`() {
        val seeks = mutableListOf<Long>()
        for (position in listOf(30_000L, 0L)) {
            val pending = ResumePosition.Pending(position)
            assertTrue(pending.consumeWhenReady(isReady = true, canSeek = true, seekTo = seeks::add))
        }
        assertEquals(listOf(30_000L, 0L), seeks)
        assertFalse(ResumePosition.Handled.consumeWhenReady(isReady = true, canSeek = true) { fail("Already handled") })
    }
}
