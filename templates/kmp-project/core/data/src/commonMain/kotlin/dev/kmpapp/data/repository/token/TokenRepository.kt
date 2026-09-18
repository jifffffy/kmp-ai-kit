package dev.kmpapp.data.repository.token

/**
 * Interface for managing authentication tokens
 * Provides methods to store, retrieve, and clear tokens
 */
interface TokenRepository {
    /**
     * Get the current access token
     * @return access token or null if not available
     */
    suspend fun getAccessToken(): String?

    /**
     * Save access token
     * @param accessToken the access token to save
     */
    suspend fun saveAccessToken(accessToken: String)

    /**
     * Clear all stored tokens
     */
    suspend fun clearTokens()

    /**
     * Check if user has valid tokens (is logged in)
     * @return true if access token exists
     */
    suspend fun hasValidTokens(): Boolean
}
