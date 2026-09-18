package dev.kmpapp.common.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import dev.kmpapp.common.util.DesktopLinkHandler
import dev.kmpapp.common.util.LinkHandler

internal actual val commonPlatformModule: Module =
    module {
        singleOf(::DesktopLinkHandler).bind<LinkHandler>()
    }
