package dev.kmpapp.data.remote.network.ktor

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.util.AttributeKey
import kotlinx.coroutines.runBlocking
import dev.kmpapp.data.repository.token.TokenRepository

/**
 * Ktor plugin that automatically adds authentication headers to requests
 * based on RequestConfig settings
 */
class TokenHeaderPlugin private constructor(
    private val tokenRepository: TokenRepository,
) {
    companion object Plugin : HttpClientPlugin<Plugin.Config, TokenHeaderPlugin> {
        override val key = AttributeKey<TokenHeaderPlugin>("TokenHeaderPlugin")

        class Config {
            var tokenRepository: TokenRepository? = null
        }

        override fun prepare(block: Config.() -> Unit): TokenHeaderPlugin {
            val config = Config().apply(block)
            return TokenHeaderPlugin(
                tokenRepository =
                    config.tokenRepository
                        ?: error("TokenRepository must be provided to TokenHeaderPlugin"),
            )
        }

        override fun install(
            plugin: TokenHeaderPlugin,
            scope: HttpClient,
        ) {
            scope.plugin(HttpSend).intercept { request ->
                val configs = request.attributes.getOrNull(RequestConfigKey)

                // Add user auth token if required
                if (configs?.isUserAuthRequired() == true) {
                    runBlocking {
                        plugin.tokenRepository.getAccessToken()?.let { token ->
                            if (token.isNotEmpty()) {
                                request.headers.append("Authorization", "Bearer $token")
                            }
                        }
                    }
                }

                execute(request)
            }
        }
    }
}
