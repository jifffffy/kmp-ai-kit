package dev.kmpapp.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * A user-facing text that is either a raw string or a localized string resource.
 *
 * Use this to carry display text on a `*UiModel` when the value originates in a
 * ViewModel (which cannot call the `@Composable stringResource`). The ViewModel
 * builds a [UiText]; the composable resolves it with [asString].
 *
 * Mirrors [ErrorModel.Resource]. See Rule 12 in `_shared/patterns.md`.
 */
@Stable
sealed interface UiText {
    data class Raw(
        val value: String,
    ) : UiText

    class Resource(
        val resId: StringResource,
        vararg val args: Any,
    ) : UiText {
        override fun toString(): String = "Resource(resId=$resId, args=${args.toList()})"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Resource) return false
            if (resId != other.resId) return false
            return args.contentEquals(other.args)
        }

        override fun hashCode(): Int {
            var result = resId.hashCode()
            result = 31 * result + args.contentHashCode()
            return result
        }
    }

    companion object {
        val Empty: UiText = Raw("")
    }
}

/** Resolve inside a composition. */
@Composable
fun UiText.asString(): String =
    when (this) {
        is UiText.Raw -> value
        is UiText.Resource -> stringResource(resId, *args)
    }

/** Resolve outside a composition (e.g. inside a coroutine / `viewModelScope`). */
suspend fun UiText.resolve(): String =
    when (this) {
        is UiText.Raw -> value
        is UiText.Resource -> getString(resId, *args)
    }
