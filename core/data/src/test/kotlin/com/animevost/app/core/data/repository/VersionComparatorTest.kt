package com.animevost.app.core.data.repository

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionComparatorTest {

    @Test
    fun `newer patch version is detected`() {
        assertTrue(isNewerVersion("1.7.13", "1.7.12"))
    }

    @Test
    fun `equal versions are not newer`() {
        assertFalse(isNewerVersion("1.7.12", "1.7.12"))
    }

    @Test
    fun `short equivalent version is not newer`() {
        assertFalse(isNewerVersion("1.7", "1.7.0"))
    }

    @Test
    fun `version prefix and suffix are supported`() {
        assertTrue(isNewerVersion("v2.0.0-beta1", "1.9.9"))
    }

    @Test
    fun `older version is rejected`() {
        assertFalse(isNewerVersion("1.6.99", "1.7.0"))
    }

    @Test
    fun `invalid version is rejected`() {
        assertFalse(isNewerVersion("latest", "1.7.0"))
    }
}
