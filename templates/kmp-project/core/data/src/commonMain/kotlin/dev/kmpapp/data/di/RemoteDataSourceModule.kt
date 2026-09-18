package dev.kmpapp.data.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.koin.core.module.Module
import org.koin.dsl.module
import dev.kmpapp.data.config.BuildOptionProvider
import dev.kmpapp.data.remote.network.adapters.ColorHexJsonAdapter
import dev.kmpapp.data.remote.network.adapters.InstantSerializer
import dev.kmpapp.data.remote.network.adapters.PhoneJsonAdapter
import dev.kmpapp.data.remote.network.ktor.ApiClient
import dev.kmpapp.data.remote.network.ktor.TokenHeaderPlugin

/**
 * Created by Ali Sadeghi
 * on 17,Apr,2025
 */

internal expect val platformRemoteDataSourceModule: Module

const val TIME_OUT = 20_000L

internal val RemoteDataSourceModule =
    module {
        single<ApiClient> {
            ApiClient(get())
        }

        single<HttpClient> {
            HttpClient(get()) {
                install(Logging) {
                    logger =
                        object : Logger {
                            override fun log(message: String) {
                                println(message)
                            }
                        }
                    level = LogLevel.ALL
                }

                val serializers =
                    SerializersModule {
                        contextual(ColorHexJsonAdapter)
                        contextual(PhoneJsonAdapter)
                        contextual(InstantSerializer)
                    }

                install(ContentNegotiation) {
                    json(
                        json =
                            Json {
                                ignoreUnknownKeys = true
                                prettyPrint = true
                                serializersModule = serializers
                                coerceInputValues = true
                            },
                    )
                }

                install(HttpTimeout) {
                    socketTimeoutMillis = TIME_OUT
                    requestTimeoutMillis = TIME_OUT
                }

                install(Resources)

                install(TokenHeaderPlugin) {
                    tokenRepository = get()
                }

                defaultRequest {

                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    header("Accept-Language", "en-us")

                    url(get<BuildOptionProvider>().apiBaseUrl)
                }
            }
        }
    }
