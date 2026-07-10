package com.animevost.app.feature.player

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerUtilsTest {

    @Test
    fun `time formatter handles zero minutes and hours`() {
        assertEquals("0:00", formatTime(0))
        assertEquals("1:05", formatTime(65_000))
        assertEquals("1:01:01", formatTime(3_661_000))
    }

    @Test
    fun `speed formatter omits redundant decimal`() {
        assertEquals("1x", formatSpeed(1f))
        assertEquals("1.5x", formatSpeed(1.5f))
    }
}
