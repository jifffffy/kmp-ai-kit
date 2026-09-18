package dev.kmpapp.designsystem.motion

import androidx.compose.runtime.Composable
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
actual fun rememberReducedMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled()
