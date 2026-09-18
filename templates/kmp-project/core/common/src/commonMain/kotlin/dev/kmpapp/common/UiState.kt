package dev.kmpapp.common

import androidx.compose.runtime.Stable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * This is a data representation in any async situations.
 * It is often used in UI components or view related things.
 *
 * Simplified version of Mavericks Async:
 * https://github.com/airbnb/mavericks/blob/317c92e35e7337f3fbc7180070f8a35e9c9e42e4/mvrx-common/src/main/java/com/airbnb/mvrx/Async.kt
 */
@Stable
sealed interface UiState<out T> {
    data object Uninitialized : UiState<Nothing>

    data object Loading : UiState<Nothing>

    data class Success<T>(
        val value: T,
    ) : UiState<T>

    data class Failed<T>(
        val error: ErrorModel,
    ) : UiState<T>
}

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalContracts::class)
inline fun <T> UiState<T>.isSuccess(): Boolean {
    contract { returns(true) implies (this@isSuccess is UiState.Success<T>) }
    return this is UiState.Success<T>
}

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalContracts::class)
inline fun <T> UiState<T>.isLoading(): Boolean {
    contract { returns(true) implies (this@isLoading is UiState.Loading) }
    return this is UiState.Loading
}

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalContracts::class)
inline fun <T> UiState<T>.isFailed(): Boolean {
    contract { returns(true) implies (this@isFailed is UiState.Failed) }
    return this is UiState.Failed
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> UiState<T>.asSuccess(): UiState.Success<T>? = this as? UiState.Success<T>

@Suppress("NOTHING_TO_INLINE")
inline fun <T> UiState<T>.asFailure(): UiState.Failed<T>? = this as? UiState.Failed<T>

inline fun <T, E> UiState<T>.map(func: (T) -> E): UiState<E> =
    when (this) {
        is UiState.Failed -> UiState.Failed(error)
        UiState.Loading -> UiState.Loading
        is UiState.Success -> UiState.Success(func(this.value))
        UiState.Uninitialized -> UiState.Uninitialized
    }
