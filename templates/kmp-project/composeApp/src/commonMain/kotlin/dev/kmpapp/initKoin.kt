package dev.kmpapp

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module
import dev.kmpapp.common.di.commonModule
import dev.kmpapp.data.config.BuildOptionProvider
import dev.kmpapp.data.di.dataModule

private val appModule =
    module {
        singleOf(::BuildOptionProviderImpl).bind<BuildOptionProvider>()
    }

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication =
    startKoin {
        appDeclaration()
        modules(
            appModule,
            commonModule,
            dataModule,
        )
    }
