package com.animevost.app.core.domain.util

/**
 * Represents the outcome of an asynchronous operation.
 *
 * @param T the type of the successful value.
 */
sealed class Result<out T> {
    /** The operation completed successfully; [data] holds the result. */
    data class Success<T>(val data: T) : Result<T>()

    /** The operation failed; [exception] and/or [message] describe the cause. */
    data class Error(val exception: Throwable? = null, val message: String? = null) : Result<Nothing>()

}

/** Executes [action] with the success value when this result is [Result.Success]. */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

/** Executes [action] with the exception and message when this result is [Result.Error]. */
inline fun <T> Result<T>.onError(action: (Throwable?, String?) -> Unit): Result<T> {
    if (this is Result.Error) action(exception, message)
    return this
}

/** Transforms a [Result.Success] value with [transform], leaving [Result.Error] unchanged. */
inline fun <T, R> Result<T>.mapSuccess(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
}
