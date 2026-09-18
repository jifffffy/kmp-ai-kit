package dev.kmpapp.data.local.pref

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Created by Ali Sadeghi
 * on 27,Aug,2023
 */
class PreferencesManager(
    private val dataStorePreference: DataStore<Preferences>,
) {
    companion object {
        // KEYS
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("KEY_ACCESS_TOKEN")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("KEY_REFRESH_TOKEN")
        private val KEY_API = stringPreferencesKey("KEY_API")
        private val KEY_CURRENCY_SYMBOL = stringPreferencesKey("KEY_CURRENCY_SYMBOL")
        private val KEY_CURRENCY_CODE = stringPreferencesKey("KEY_CURRENCY_CODE")
        private val KEY_STORE_LOGO = stringPreferencesKey("KEY_STORE_LOGO")

        // DEFAULTS
        private const val DEFAULT_TOKEN = ""
        const val DEFAULT_API_KEY = "2d5abdf6-bc17-4c08-920e-d949f1ecf46f"
        private const val DEFAULT_CURRENCY = ""
        private const val DEFAULT_LOGO = ""
    }

    suspend fun setAccessToken(token: String) {
        dataStorePreference.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = token
        }
    }

    fun getAccessToken(): Flow<String> =
        dataStorePreference.data.map { preferences ->
            preferences[KEY_ACCESS_TOKEN] ?: DEFAULT_TOKEN
        }

    suspend fun setRefreshToken(token: String) {
        dataStorePreference.edit { preferences ->
            preferences[KEY_REFRESH_TOKEN] = token
        }
    }

    fun getRefreshToken(): Flow<String> =
        dataStorePreference.data.map { preferences ->
            preferences[KEY_REFRESH_TOKEN] ?: DEFAULT_TOKEN
        }

    suspend fun setApiKey(token: String) {
        dataStorePreference.edit { preferences ->
            preferences[KEY_API] = token
        }
    }

    fun getApiKey(): Flow<String> =
        dataStorePreference.data.map { preferences ->
            preferences[KEY_API] ?: DEFAULT_API_KEY
        }

    suspend fun setCurrencySymbol(symbol: String) {
        dataStorePreference.edit { preferences ->
            preferences[KEY_CURRENCY_SYMBOL] = symbol
        }
    }

    suspend fun getCurrencySymbol(): String =
        dataStorePreference.data
            .map { preferences ->
                preferences[KEY_CURRENCY_SYMBOL] ?: DEFAULT_CURRENCY
            }.first()

    suspend fun setCurrencyCode(code: String) {
        dataStorePreference.edit { preferences ->
            preferences[KEY_CURRENCY_CODE] = code
        }
    }

    suspend fun getCurrencyCode(): String =
        dataStorePreference.data
            .map { preferences ->
                preferences[KEY_CURRENCY_CODE] ?: DEFAULT_CURRENCY
            }.first()

    suspend fun setStoreLogo(logo: String) {
        dataStorePreference.edit { preferences ->
            preferences[KEY_STORE_LOGO] = logo
        }
    }

    suspend fun getStoreLogo(): String =
        dataStorePreference.data
            .map { preferences ->
                preferences[KEY_STORE_LOGO] ?: DEFAULT_LOGO
            }.first()

    suspend fun putString(
        key: String,
        value: String,
    ) {
        dataStorePreference.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }

    fun getString(key: String): Flow<String?> =
        dataStorePreference.data.map { preferences ->
            preferences[stringPreferencesKey(key)]
        }

    suspend fun clear() {
        dataStorePreference.edit { preference ->
            preference.clear()
        }
    }
}
