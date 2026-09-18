package dev.kmpapp.common

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Stable
sealed class PaginationUiState<T>(
    val page: Int,
    val data: ImmutableList<T>,
    val hasNext: Boolean,
) {
    class Uninitialized<T> : PaginationUiState<T>(page = -1, data = persistentListOf(), hasNext = true) {
        override fun toString(): String = "Uninitialized() ${super.toString()}"
    }

    class Loading<T> internal constructor(
        page: Int,
        data: ImmutableList<T>,
        hasNext: Boolean,
    ) : PaginationUiState<T>(page, data, hasNext) {
        override fun toString(): String = "Loading() ${super.toString()}"
    }

    class Success<T> internal constructor(
        page: Int,
        data: ImmutableList<T>,
        hasNext: Boolean,
    ) : PaginationUiState<T>(page, data, hasNext) {
        override fun toString(): String = "Success() ${super.toString()}"
    }

    class Failed<T> internal constructor(
        page: Int,
        data: ImmutableList<T>,
        hasNext: Boolean,
        val error: ErrorModel,
    ) : PaginationUiState<T>(page, data, hasNext) {
        override fun toString(): String = "Failed(error=$error) ${super.toString()})"
    }

    override fun toString(): String = "PaginationUiState(page=$page, data=$data, hasNext=$hasNext)"

    fun success(newData: List<T>): Success<T> =
        if (newData.isEmpty()) {
            Success(page, data, hasNext = false)
        } else {
            Success(page.inc(), data.plus(newData).toImmutableList(), hasNext = true)
        }

    fun loading(): Loading<T> = Loading(page, data, hasNext)

    fun failed(error: ErrorModel): Failed<T> = Failed(page, data, hasNext, error = error)
}

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalContracts::class)
inline fun <T> PaginationUiState<T>.isSuccess(): Boolean {
    contract { returns(true) implies (this@isSuccess is PaginationUiState.Success<T>) }
    return this is PaginationUiState.Success<T>
}

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalContracts::class)
inline fun <T> PaginationUiState<T>.isLoading(): Boolean {
    contract { returns(true) implies (this@isLoading is PaginationUiState.Loading) }
    return this is PaginationUiState.Loading
}

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalContracts::class)
inline fun <T> PaginationUiState<T>.isFailed(): Boolean {
    contract { returns(true) implies (this@isFailed is PaginationUiState.Failed) }
    return this is PaginationUiState.Failed
}
