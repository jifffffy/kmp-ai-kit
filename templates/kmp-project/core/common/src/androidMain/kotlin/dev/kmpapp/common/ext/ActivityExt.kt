package dev.kmpapp.common.ext

import android.app.Activity
import android.graphics.Insets
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets

/**
 * Created by Ali Sadeghi
 * on 10,Jan,2023
 */
fun Activity.getScreenWidth(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = this.windowManager.currentWindowMetrics
        val insets: Insets =
            windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        windowMetrics.bounds.width() - insets.left - insets.right
    } else {
        @Suppress("DEPRECATION")
        val displayMetrics =
            DisplayMetrics().also { this.windowManager.defaultDisplay.getMetrics(it) }
        displayMetrics.widthPixels
    }

fun Activity.getScreenHeight(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = this.windowManager.currentWindowMetrics
        val insets: Insets =
            windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        windowMetrics.bounds.height() - insets.top - insets.bottom
    } else {
        @Suppress("DEPRECATION")
        val displayMetrics =
            DisplayMetrics().also { this.windowManager.defaultDisplay.getMetrics(it) }
        displayMetrics.heightPixels
    }
