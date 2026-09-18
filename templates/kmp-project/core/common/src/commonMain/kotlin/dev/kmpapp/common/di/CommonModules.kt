package dev.kmpapp.common.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Created by Ali Sadeghi
 * on 07,May,2025
 */

internal expect val commonPlatformModule: Module

val commonModule: Module =
    module {
        includes(commonPlatformModule)
    }
