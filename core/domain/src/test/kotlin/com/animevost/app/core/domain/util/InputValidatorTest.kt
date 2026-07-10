package com.animevost.app.core.domain.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InputValidatorTest {

    @Test
    fun `valid credentials pass validation`() {
        assertTrue(InputValidator.validateUsername("user123").isValid)
        assertTrue(InputValidator.validatePassword("secret123").isValid)
        assertTrue(InputValidator.validateEmail("user@example.org").isValid)
    }

    @Test
    fun `short credentials fail validation`() {
        assertFalse(InputValidator.validateUsername("ab").isValid)
        assertFalse(InputValidator.validatePassword("12345").isValid)
    }

    @Test
    fun `malformed email fails validation`() {
        assertFalse(InputValidator.validateEmail("user-at-example").isValid)
    }

    @Test
    fun `matching password confirmation passes validation`() {
        assertTrue(InputValidator.validatePasswordConfirmation("secret123", "secret123").isValid)
    }

    @Test
    fun `different password confirmation fails validation`() {
        assertFalse(InputValidator.validatePasswordConfirmation("secret123", "secret321").isValid)
        assertFalse(InputValidator.validatePasswordConfirmation("secret123", "").isValid)
    }
}
