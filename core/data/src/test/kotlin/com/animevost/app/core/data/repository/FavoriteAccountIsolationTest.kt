package com.animevost.app.core.data.repository

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoriteAccountIsolationTest {

    @Test
    fun `anonymous favorites may merge into first account`() {
        assertTrue(canMergeLocalFavorites(ownerAccountId = null, currentAccountId = 10))
    }

    @Test
    fun `same account favorites may merge`() {
        assertTrue(canMergeLocalFavorites(ownerAccountId = 10, currentAccountId = 10))
    }

    @Test
    fun `different account favorites must not merge`() {
        assertFalse(canMergeLocalFavorites(ownerAccountId = 10, currentAccountId = 20))
    }
}
