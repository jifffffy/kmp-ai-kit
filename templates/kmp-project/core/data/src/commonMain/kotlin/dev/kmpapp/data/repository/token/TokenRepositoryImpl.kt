package dev.kmpapp.data.repository.token

import dev.kmpapp.data.datasource.local.token.TokenLocalDataSource

/**
 * Delegates token persistence to [TokenLocalDataSource]; derives [hasValidTokens] from the
 * stored access token.
 */
internal class TokenRepositoryImpl(
    private val localDataSource: TokenLocalDataSource,
) : TokenRepository {
    override suspend fun getAccessToken(): String? = localDataSource.getAccessToken()

    override suspend fun saveAccessToken(accessToken: String) = localDataSource.saveAccessToken(accessToken)

    override suspend fun clearTokens() = localDataSource.clearTokens()

    override suspend fun hasValidTokens(): Boolean = getAccessToken()?.isNotEmpty() == true
}
