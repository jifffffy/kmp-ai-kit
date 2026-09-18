package dev.kmpapp.data.remote.network.ktor

import io.ktor.client.call.body
import dev.kmpapp.common.Either

/**
 * Created by Ali Sadeghi
 * on 15,Apr,2025
 */

// Factory to create call adapters
class CallAdapterFactory {
    inline fun <reified T> create(): KtorCallAdapter<T, Either<T>> =
        EitherCallAdapter { response ->
            response.body<T>()
        }
}
