package dev.kmpapp.data.repository.theme

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dev.kmpapp.data.datasource.local.theme.ThemeLocalDataSource

/**
 * Seeds the theme mode from [ThemeLocalDataSource] on creation and writes back on every
 * [setThemeMode]; the in-memory [StateFlow] is the synchronous read source for callers.
 */
internal class ThemeRepositoryImpl(
    private val localDataSource: ThemeLocalDataSource,
) : ThemeRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    override val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    init {
        scope.launch { localDataSource.getThemeMode()?.let { _themeMode.value = it } }
    }

    override fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        scope.launch { localDataSource.setThemeMode(mode) }
    }
}
