package dev.kmpapp.designsystem.motion

import androidx.compose.runtime.Composable

// Desktop (JVM) has no system reduce-motion signal.
@Composable
actual fun rememberReducedMotion(): Boolean = false
