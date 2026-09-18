package dev.kmpapp.designsystem.locale

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import java.util.Locale

actual object LocalAppLocale {
    // Captured once at first access, before any override is applied, so `null` can restore it.
    private val default: Locale = Locale.getDefault()

    actual val current: String
        @Composable get() = ConfigurationCompat.getLocales(LocalConfiguration.current)[0]?.toString().orEmpty()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current
        val new = if (value == null) default else Locale.forLanguageTag(value)
        Locale.setDefault(new)

        // Provide an updated Configuration so LocalResources / stringResource recompose
        // in the new locale. Avoids the deprecated Resources.updateConfiguration global mutation.
        val newConfiguration = Configuration(configuration).apply { setLocale(new) }
        return LocalConfiguration.provides(newConfiguration)
    }
}
