package dev.kmpapp.common.util

/**
 * Created by Ali Sadeghi
 * on 07,May,2025
 */

enum class Platform {
    Android,
    IOS,
    Desktop,
}

expect fun getPlatform(): Platform

fun isOnAndroid() = getPlatform() == Platform.Android

fun isOnIOS() = getPlatform() == Platform.IOS
