package dev.kmpapp.data.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Created by Ali Sadeghi
 * on 17,Apr,2025
 */
internal actual val platformRemoteDataSourceModule: Module =
    module {
        single<HttpClientEngine> {
            OkHttp.create()
        }
    }
