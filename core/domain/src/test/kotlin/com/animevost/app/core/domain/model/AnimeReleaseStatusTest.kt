package com.animevost.app.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimeReleaseStatusTest {

    @Test
    fun `ongoing category takes priority over generic categories`() {
        assertEquals(
            AnimeReleaseStatus.ONGOING,
            AnimeReleaseStatus.fromCategories(listOf("ТВ", "Онгоинги", "2026")),
        )
    }

    @Test
    fun `generic categories mean completed release`() {
        assertEquals(
            AnimeReleaseStatus.COMPLETED,
            AnimeReleaseStatus.fromCategories(listOf("ТВ", "2020", "Фэнтези")),
        )
    }

    @Test
    fun `missing categories keep safe notification fallback`() {
        val status = AnimeReleaseStatus.fromCategories(emptyList())

        assertEquals(AnimeReleaseStatus.UNKNOWN, status)
        assertTrue(status.supportsEpisodeNotifications)
        assertTrue(status.shouldPollForEpisodes)
    }

    @Test
    fun `completed releases are excluded from polling and notifications`() {
        assertFalse(AnimeReleaseStatus.COMPLETED.supportsEpisodeNotifications)
        assertFalse(AnimeReleaseStatus.COMPLETED.shouldPollForEpisodes)
    }
}
