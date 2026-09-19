package dev.kmpapp

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * Desktop (JVM) entry point — the desktop twin of the iOS `MainViewController`.
 *
 * The `jvm("desktop")` target exists so shared code and feature UI can be built and
 * tested on the JVM. This entry point makes it *runnable* too, which matters because a
 * target that only compiles hides startup-time failures (Koin graph gaps, missing Ktor
 * engine, DataStore path) that also break Android and iOS at launch.
 *
 * Opens the app in a phone-sized window so the mobile layout is what you see, and
 * initialises Koin before the first composition (the shared `App()` injects the theme
 * repository and feature ViewModels from the graph).
 */
fun main() =
    application {
        initKoin()
        Window(
            onCloseRequest = ::exitApplication,
            title = "KmpApp",
            state = rememberWindowState(size = DpSize(420.dp, 900.dp)),
        ) {
            App()
        }
    }
