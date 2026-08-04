package com.animevost.app.core.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class AnimeUrlIdentityTest {

    @Test
    fun `extracts news id from canonical and resolver urls`() {
        assertEquals(3970, extractAnimeNewsId("tip/tv/3970-title.html"))
        assertEquals(3970, extractAnimeNewsId("https://v13.vost.pw/tip/tv/3970-title.html"))
        assertEquals(3970, extractAnimeNewsId("index.php?newsid=3970"))
        assertEquals(3970, extractAnimeNewsId("https://v13.vost.pw/index.php?newsid=3970"))
    }

    @Test
    fun `normalizes resolver url without losing its news id query`() {
        assertEquals(
            "index.php?newsid=3970",
            normalizeAnimePath("https://v13.vost.pw/index.php?newsid=3970"),
        )
    }

    @Test
    fun `normalizes canonical url to mirror independent path`() {
        assertEquals(
            "tip/tv/3970-title.html",
            normalizeAnimePath("https://v13.vost.pw/tip/tv/3970-title.html"),
        )
    }
}
