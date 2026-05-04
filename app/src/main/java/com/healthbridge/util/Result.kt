package com.healthbridge.util

/**
 * Sealed class for type-safe result handling
 * Similar to Kotlin's Result but more explicit for our use case
 */
sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure<T>(val exception: Exception) : Result<T>()

    inline fun onSuccess(block: (T) -> Unit): Result<T> {
        if (this is Success) block(data)
        return this
    }

    inline fun onFailure(block: (Exception) -> Unit): Result<T> {
        if (this is Failure) block(exception)
        return this
    }

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun exceptionOrNull(): Exception? = when (this) {
        is Success -> null
        is Failure -> exception
    }

    /**
     * Transform the success value while preserving failure state
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> Failure(exception)
    }

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun <T> failure(exception: Exception): Result<T> = Failure(exception)
    }
}

