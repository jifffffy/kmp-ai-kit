package dev.kmpapp.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * This class represent different types of error or unsuccessful data.
 * It can be used in any layer or class (UI, Data,...)
 */
@Stable
sealed interface ErrorModel {
    data class Exception(
        val exception: Throwable,
    ) : ErrorModel

    data class Message(
        val message: String,
    ) : ErrorModel

    open class MessageCode(
        val message: String,
        val code: Int,
    ) : ErrorModel {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            other as MessageCode

            if (message != other.message) return false
            return code == other.code
        }

        override fun hashCode(): Int {
            var result = message.hashCode()
            result = 31 * result + code
            return result
        }

        override fun toString(): String = "MessageCode(message='$message', code=$code)"
    }

    class Resource(
        val messageResId: StringResource,
        vararg val args: Any = emptyArray(),
    ) : ErrorModel {
        override fun toString(): String = "Resource(messageResId=$messageResId, args=${args.toList()})"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            other as Resource

            if (messageResId != other.messageResId) return false
            return args.contentEquals(other.args)
        }

        override fun hashCode(): Int = messageResId.hashCode()
    }
}

@Composable
fun ErrorModel.asStringOrNull(): String? = internalAsString()

@Composable
fun ErrorModel.asString(): String = requireNotNull(internalAsString())

@Composable
private fun ErrorModel.internalAsString(): String? =
    when (this) {
        is ErrorModel.Exception -> exception.message.orEmpty()
        is ErrorModel.Message -> message
        is ErrorModel.MessageCode -> "$message (#$code)"
        is ErrorModel.Resource -> stringResource(messageResId, *args)
    }
