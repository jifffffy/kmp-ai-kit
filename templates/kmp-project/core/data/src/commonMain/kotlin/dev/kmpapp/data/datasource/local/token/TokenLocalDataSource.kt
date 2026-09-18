package dev.kmpapp.data.datasource.local.token

/** Persists the authentication access token. */
internal interface TokenLocalDataSource {
    suspend fun getAccessToken(): String?

    suspend fun saveAccessToken(accessToken: String)

    suspend fun clearTokens()
}
