package dev.kmpapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.kmpapp.designsystem.XNavHost

/**
 * Main app navigation host.
 * Routes to WelcomeScreen until the first feature is wired in.
 */
@Composable
fun BaseAppNavHost(modifier: Modifier) {
    val navController = rememberNavController()

    XNavHost(
        modifier = modifier,
        navController = navController,
        startDestination = WelcomeRoute,
    ) {
        composable<WelcomeRoute> { WelcomeScreen() }
    }
}
