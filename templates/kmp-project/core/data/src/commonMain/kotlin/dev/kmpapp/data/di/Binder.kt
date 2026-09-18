package dev.kmpapp.data.di

import kotlinx.serialization.json.Json
import org.koin.dsl.module

/**
 * Data layer dependency injection bindings
 */
internal val binder =
    module {
        // JSON serializer
        single {
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                isLenient = true
            }
        }
    }
