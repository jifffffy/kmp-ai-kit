package dev.kmpapp.data.repository.theme

/**
 * App-wide color scheme preference.
 *
 * [SYSTEM] follows the OS dark/light setting; [LIGHT]/[DARK] pin the app to a scheme
 * regardless of the OS setting.
 */
enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
