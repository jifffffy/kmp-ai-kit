package dev.kmpapp.data.remote.network.ktor

import io.ktor.util.AttributeKey

/**
 * Created by Ali Sadeghi
 * on 01,May,2025
 */

val RequestConfigKey = AttributeKey<List<RequestConfig>>("RequestConfigs")

sealed class RequestConfig {
    data object UserAuthHeader : RequestConfig()

    data object NoApiKeyHeader : RequestConfig()

    companion object {
        fun build(
            userAuthRequired: Boolean = true,
            apiKeyNotRequired: Boolean = false,
        ): List<RequestConfig> =
            mutableListOf<RequestConfig>().apply {
                if (userAuthRequired) add(UserAuthHeader)
                if (apiKeyNotRequired) add(NoApiKeyHeader)
            }
    }
}

fun List<RequestConfig>.isUserAuthRequired(): Boolean = any { it is RequestConfig.UserAuthHeader }

fun List<RequestConfig>.isApiKeyNotRequired(): Boolean =
    this.any {
        it is RequestConfig.NoApiKeyHeader
    }
