package dev.kmpapp.designsystem

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import kotlin.reflect.KClass

@Composable
fun XNavHost(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    route: KClass<*>? = null,
    enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) =
        {
            slideIn(
                animationSpec = tween(),
                initialOffset = { IntOffset(it.width, 0) },
            )
        },
    exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) =
        {
            slideOut(
                animationSpec = tween(),
                targetOffset = { IntOffset(-it.width, 0) },
            )
        },
    popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) =
        {
            slideIn(
                animationSpec = tween(),
                initialOffset = { IntOffset(-it.width, 0) },
            )
        },
    popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) =
        {
            slideOut(
                animationSpec = tween(),
                targetOffset = { IntOffset(it.width, 0) },
            )
        },
    builder: NavGraphBuilder.() -> Unit,
) {
    NavHost(
        navController = navController,
        modifier = modifier,
        route = route,
        startDestination = startDestination,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition,
        builder = builder,
        contentAlignment = contentAlignment,
    )
}
