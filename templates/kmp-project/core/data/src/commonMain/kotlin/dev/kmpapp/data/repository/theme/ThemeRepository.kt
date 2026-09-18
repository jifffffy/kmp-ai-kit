package dev.kmpapp.data.repository.theme

import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the selected [AppThemeMode] and persists it across launches.
 *
 * Exposed as a Koin singleton (see `LocalDataSourceModule.kt`). The app root collects
 * [themeMode] and feeds it to `XTheme`; the settings screen calls [setThemeMode].
 */
interface ThemeRepository {
    val themeMode: StateFlow<AppThemeMode>

    fun setThemeMode(mode: AppThemeMode)
}
