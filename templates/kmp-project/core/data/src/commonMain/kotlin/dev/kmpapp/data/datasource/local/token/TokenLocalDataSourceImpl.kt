package dev.kmpapp.data.datasource.local.token

import kotlinx.coroutines.flow.first
import dev.kmpapp.data.local.pref.PreferencesManager

internal class TokenLocalDataSourceImpl(
    private val preferencesManager: PreferencesManager,
) : TokenLocalDataSource {
    override suspend fun getAccessToken(): String? = preferencesManager.getString(KEY).first()?.takeIf { it.isNotEmpty() }

    override suspend fun saveAccessToken(accessToken: String) {
        preferencesManager.putString(KEY, accessToken)
    }

    override suspend fun clearTokens() {
        preferencesManager.putString(KEY, "")
    }

    private companion object {
        const val KEY = "access_token"
    }
}
