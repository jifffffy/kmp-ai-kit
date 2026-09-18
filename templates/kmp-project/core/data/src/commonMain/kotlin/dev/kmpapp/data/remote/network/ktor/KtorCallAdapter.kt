package dev.kmpapp.data.remote.network.ktor

import io.ktor.client.statement.HttpResponse

/**
 * Created by Ali Sadeghi
 * on 15,Apr,2025
 */

// Interface for adapting Ktor calls
interface KtorCallAdapter<T, R> {
    suspend fun adapt(call: suspend () -> HttpResponse): R
}
