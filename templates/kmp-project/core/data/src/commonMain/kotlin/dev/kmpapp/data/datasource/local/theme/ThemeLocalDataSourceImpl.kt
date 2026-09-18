package dev.kmpapp.data.datasource.local.theme

import kotlinx.coroutines.flow.first
import dev.kmpapp.data.local.pref.PreferencesManager
import dev.kmpapp.data.repository.theme.AppThemeMode

internal class ThemeLocalDataSourceImpl(
    private val preferencesManager: PreferencesManager,
) : ThemeLocalDataSource {
    override suspend fun getThemeMode(): AppThemeMode? =
        preferencesManager.getString(KEY).first()?.let { name ->
            AppThemeMode.entries.firstOrNull { it.name == name }
        }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        preferencesManager.putString(KEY, mode.name)
    }

    private companion object {
        const val KEY = "theme_mode"
    }
}
