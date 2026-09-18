package dev.kmpapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import dev.kmpapp.data.repository.theme.AppThemeMode
import dev.kmpapp.data.repository.theme.ThemeRepository
import dev.kmpapp.designsystem.SnackbarController
import dev.kmpapp.designsystem.Toast
import dev.kmpapp.designsystem.XScaffold
import dev.kmpapp.designsystem.XTheme
import dev.kmpapp.designsystem.rememberToastState

@Composable
fun App() {
    ProvideAppLocale {
        AppContent()
    }
}

@Composable
private fun AppContent() {
    val themeRepository = koinInject<ThemeRepository>()
    val themeMode by themeRepository.themeMode.collectAsStateWithLifecycle()
    val darkTheme =
        when (themeMode) {
            AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK -> true
        }

    XTheme(darkTheme = darkTheme) {
        val toastState = rememberToastState()
        val snackbarHostState = remember { SnackbarHostState() }

        // The single app-shell Scaffold : owns shared chrome + the screen-frame insets.
        // contentWindowInsets = 0 so the Scaffold consumes nothing. The NavHost is padded by the
        // TOP + HORIZONTAL safe area (status bar + display cutout) plus imePadding, but NOT the
        // bottom — so bottom action bars can bleed their background to the screen edge and inset
        // their own content via navigationBarsPadding (standard edge-to-edge pattern).
        XScaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                )
            },
            bottomBar = {},
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { _ ->
            BaseAppNavHost(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                        ).imePadding(),
            )

            Toast(state = toastState)

            SnackbarController(snackbarHostState = snackbarHostState)
        }
    }
}
