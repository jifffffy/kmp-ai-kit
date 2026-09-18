package dev.kmpapp.data.datasource.local.theme

import dev.kmpapp.data.repository.theme.AppThemeMode

/** Reads/writes the persisted [AppThemeMode]. `null` ⇒ nothing stored yet. */
internal interface ThemeLocalDataSource {
    suspend fun getThemeMode(): AppThemeMode?

    suspend fun setThemeMode(mode: AppThemeMode)
}
