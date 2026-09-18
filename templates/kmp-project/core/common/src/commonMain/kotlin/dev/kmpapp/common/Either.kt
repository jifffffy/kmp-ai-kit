package dev.kmpapp.common

import dev.kmpapp.common.Either.Failure
import dev.kmpapp.common.Either.Success
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * The use case result class. This class is a sealed class and can be on of the
 * values [Success] or [Failure]. If the result if a use-case is success then the
 * [Success] will be returned with the result [T] value. Otherwise the [Failure]
 * will be returned with the [ErrorModel] field.
 *
 * @param T The result value
 */
sealed class Either<out T> {
    data class Success<T>(
        val data: T,
    ) : Either<T>()

    open class Failure(
        val error: ErrorModel,
    ) : Either<Nothing>() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            other as Failure
            return error == other.error
        }

        override fun hashCode(): Int = error.hashCode()

        override fun toString(): String = "Failure(error=$error)"
    }
}

/**
 * Executes the given [func] if this instance is [Success].
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> Either<T>.onSuccess(func: (result: T) -> Unit): Either<T> {
    contract {
        callsInPlace(func, InvocationKind.AT_MOST_ONCE)
    }
    return also { if (this is Success) func.invoke(data) }
}

/**
 * Executes the given [func] if this instance is [Failure].
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> Either<T>.onFailure(func: (error: ErrorModel) -> Unit): Either<T> {
    contract {
        callsInPlace(func, InvocationKind.AT_MOST_ONCE)
    }
    return also { if (this is Failure) func.invoke(error) }
}

/**
 * A map method that is used for manipulating the original [Either]
 * @return [Failure] when this instance is [Failure]
 */
inline fun <T, E> Either<T>.map(func: (T) -> Either<E>): Either<E> =
    when (this) {
        is Failure -> this
        is Success -> func(data)
    }

/**
 * A map method that is usually used for manipulating value of [Success]
 * @return [Failure] when this instance is [Failure]
 * @see map
 */
inline fun <T, E> Either<T>.mapSuccess(func: (T) -> E): Either<E> =
    when (this) {
        is Failure -> this
        is Success -> Success(func(data))
    }
