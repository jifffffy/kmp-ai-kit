package dev.kmpapp.data.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Aggregates the data-layer Koin modules (binders, remote/local data sources,
 * and their platform-specific bindings) into a single module.
 */
val dataModule: Module =
    module {
        includes(
            binder,
            RemoteDataSourceModule,
            platformRemoteDataSourceModule,
            localDataSourceModule,
            platformLocalDataSourceModule,
        )
    }
