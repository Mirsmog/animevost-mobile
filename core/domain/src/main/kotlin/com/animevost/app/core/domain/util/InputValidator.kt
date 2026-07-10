package com.animevost.app.core.domain.util

/** Result of a single field validation. */
sealed class ValidationResult {
    /** The value passed all validation rules. */
    object Valid : ValidationResult()

    /** The value failed validation; [reason] describes why. */
    data class Invalid(val reason: String) : ValidationResult()
}

/** Returns `true` when this result is [ValidationResult.Valid]. */
val ValidationResult.isValid: Boolean get() = this is ValidationResult.Valid

/**
 * Stateless input validation helpers for common auth fields.
 * All methods return [ValidationResult] — never throw.
 */
object InputValidator {

    private const val MIN_USERNAME_LENGTH = 3
    private const val MAX_USERNAME_LENGTH = 32
    private const val MIN_PASSWORD_LENGTH = 6

    // RFC 5322 simplified email regex
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /** Validates that [username] is non-blank and within the allowed length range. */
    fun validateUsername(username: String): ValidationResult = when {
        username.isBlank() ->
            ValidationResult.Invalid("Введите имя пользователя")
        username.length < MIN_USERNAME_LENGTH ->
            ValidationResult.Invalid("Имя должно содержать не менее $MIN_USERNAME_LENGTH символов")
        username.length > MAX_USERNAME_LENGTH ->
            ValidationResult.Invalid("Имя должно содержать не более $MAX_USERNAME_LENGTH символов")
        else -> ValidationResult.Valid
    }

    /** Validates that [password] is non-blank and meets the minimum length requirement. */
    fun validatePassword(password: String): ValidationResult = when {
        password.isBlank() ->
            ValidationResult.Invalid("Введите пароль")
        password.length < MIN_PASSWORD_LENGTH ->
            ValidationResult.Invalid("Пароль должен содержать не менее $MIN_PASSWORD_LENGTH символов")
        else -> ValidationResult.Valid
    }

    /** Validates that the repeated password matches the original password. */
    fun validatePasswordConfirmation(password: String, confirmation: String): ValidationResult = when {
        confirmation.isBlank() ->
            ValidationResult.Invalid("Повторите пароль")
        password != confirmation ->
            ValidationResult.Invalid("Пароли не совпадают")
        else -> ValidationResult.Valid
    }

    /** Validates that [email] is non-blank and matches the expected email format. */
    fun validateEmail(email: String): ValidationResult = when {
        email.isBlank() ->
            ValidationResult.Invalid("Введите адрес электронной почты")
        !EMAIL_REGEX.matches(email) ->
            ValidationResult.Invalid("Введите корректный адрес электронной почты")
        else -> ValidationResult.Valid
    }
}
