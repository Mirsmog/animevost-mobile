package com.animevost.app.feature.detail

import kotlin.test.Test
import kotlin.test.assertEquals

class EpisodeRangeTest {
    @Test
    fun `an announcement belongs to the last range of released episodes`() {
        assertEquals(0, lastEpisodeRangeStart(0))
        assertEquals(0, lastEpisodeRangeStart(50))
        assertEquals(50, lastEpisodeRangeStart(51))
        assertEquals(50, lastEpisodeRangeStart(60))
        assertEquals(50, lastEpisodeRangeStart(100))
        assertEquals(100, lastEpisodeRangeStart(101))
    }
}
