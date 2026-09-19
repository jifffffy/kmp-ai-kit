package dev.kmpapp.data.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Desktop (JVM) actual: supplies the Ktor engine the shared `HttpClient(get())` binding
 * resolves at runtime.
 *
 * This module used to be empty, which meant the desktop target compiled but could never
 * run: the shared `HttpClient(engine)` binding had nothing to inject. OkHttp is JVM-native,
 * so the same engine Android uses serves desktop with no extra dependency.
 */
internal actual val platformRemoteDataSourceModule: Module =
    module {
        single<HttpClientEngine> {
            OkHttp.create()
        }
    }
