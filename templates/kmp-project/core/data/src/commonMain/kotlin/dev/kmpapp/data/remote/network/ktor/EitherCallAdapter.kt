package dev.kmpapp.data.remote.network.ktor

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import dev.kmpapp.common.Either
import dev.kmpapp.common.ErrorModel
import dev.kmpapp.data.ErrorConst
import dev.kmpapp.data.model.NetworkErrorModel

/**
 * Created by Ali Sadeghi
 * on 15,Apr,2025
 */

// Implementation for adapting calls to Either
class EitherCallAdapter<T>(
    private val converter: suspend (HttpResponse) -> T,
) : KtorCallAdapter<T, Either<T>> {
    override suspend fun adapt(call: suspend () -> HttpResponse): Either<T> =
        try {
            call.invoke().toEither(converter)
        } catch (e: Throwable) {
            Either.Failure(ErrorConst.NoNetwork)
        }
}

private suspend fun <T> HttpResponse.toEither(converter: suspend (HttpResponse) -> T): Either<T> =
    when (status.value) {
        in 200 until 300 -> {
            try {
                val body = converter(this)
                Either.Success(body)
            } catch (e: Throwable) {
                Either.Failure(ErrorConst.SerializationError)
            }
        }

        HttpStatusCode.Unauthorized.value -> Either.Failure(ErrorConst.Unauthorized)

        else -> {
            val errorContent = body<NetworkErrorModel?>()

            val error =
                when {
                    errorContent?.errorCode in ErrorConst.ServerHandledError ->
                        ErrorConst.ServerHandledError[errorContent?.errorCode]

                    errorContent?.detailMessage.isNullOrBlank() -> ErrorConst.ServerUnknownError(status.value)

                    errorContent.errorCode == null ->
                        ErrorModel.Message(errorContent.detailMessage)

                    else ->
                        ErrorModel.MessageCode(
                            errorContent.detailMessage,
                            errorContent.errorCode,
                        )
                } ?: ErrorConst.ServerUnknownError(status.value)

            NetworkFailure(
                baseError = error,
                errorText = errorContent?.detailMessage,
                errorCode = status.value,
            )
        }
    }

private data class NetworkFailure(
    val errorText: String?,
    val errorCode: Int,
    private val baseError: ErrorModel,
) : Either.Failure(baseError)
