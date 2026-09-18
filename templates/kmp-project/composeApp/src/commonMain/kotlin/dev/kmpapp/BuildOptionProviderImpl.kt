package dev.kmpapp

import dev.kmpapp.data.config.BuildOptionProvider

class BuildOptionProviderImpl : BuildOptionProvider {
    override val apiBaseUrl: String
        get() = BuildKonfig.BASE_URL

    override val appVersion: String
        get() = "${BuildKonfig.VERSION_NAME}(${BuildKonfig.VERSION_CODE})"
}
