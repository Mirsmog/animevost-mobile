package com.animevost.app.core.data.repository

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoriteAccountIsolationTest {

    @Test
    fun `anonymous favorites may merge into first account`() {
        assertTrue(canImportGuestFavorites(ownerAccountId = null))
    }

    @Test
    fun `account cache must not resurrect favorites removed on another device`() {
        assertFalse(canImportGuestFavorites(ownerAccountId = 10))
    }
}
