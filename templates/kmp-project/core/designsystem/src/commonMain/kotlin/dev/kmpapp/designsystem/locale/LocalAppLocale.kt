package dev.kmpapp.designsystem.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue

/**
 * Per-app locale override for Compose Multiplatform string resources.
 *
 * Provide it at the app root:
 * ```
 * CompositionLocalProvider(LocalAppLocale provides languageTag) { ... }
 * ```
 * Changing the provided value recomposes the tree and re-resolves every
 * `stringResource`. A `null` value falls back to the system locale.
 *
 * Official pattern, see JetBrains `compose-resource-environment.md`.
 */
expect object LocalAppLocale {
    val current: String
        @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}
