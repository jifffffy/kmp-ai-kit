package dev.kmpapp.common.ext

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import java.util.Locale

/**
 * Created by Ali Sadeghi
 * on 01,Oct,2021
 */

fun Context.getScreenResolution(): String {
    val wm = this.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = wm.currentWindowMetrics.bounds
        "${bounds.width()}x${bounds.height()}"
    } else {
        @Suppress("DEPRECATION")
        val metrics = DisplayMetrics().also { wm.defaultDisplay.getMetrics(it) }
        "${metrics.widthPixels}x${metrics.heightPixels}"
    }
}

fun Context.getScreenDensity(): Float = (this.resources.displayMetrics.density * 160f)

fun Context.getFormattedScreenDensity(): String =
    when (getScreenDensity()) {
        in 0f..160f -> "MDPI"
        in 161f..240f -> "HDPI"
        in 241f..320f -> "XHDPI"
        in 321f..480f -> "XXHDPI"
        else -> "XXXHDPI"
    }

fun Context.getGSFID(): String? {
    return try {
        val gsUri = Uri.parse("content://com.google.android.gsf.gservices")
        val query =
            this.contentResolver.query(gsUri, null, null, arrayOf("android_id"), null)
                ?: return "Not found"
        if (!query.moveToFirst() || query.columnCount < 2) {
            query.close()
            return "Not found"
        }
        val toHexString = java.lang.Long.toHexString(query.getString(1).toLong())
        query.close()
        toHexString.uppercase(Locale.getDefault()).trim { it <= ' ' }
    } catch (e: SecurityException) {
        e.printStackTrace()
        null
    } catch (e2: java.lang.Exception) {
        e2.printStackTrace()
        null
    }
}

fun Context.copyToClipboard(
    text: String,
    label: String? = null,
) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = ClipData.newPlainText(label.orEmpty(), text)
    clipboard.setPrimaryClip(clip)
}

fun Context.openBrowser(url: String) {
    val i = Intent(Intent.ACTION_VIEW)
    i.data = Uri.parse(url)
    startActivity(i)
}

fun Context.shareLink(url: String) {
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            setType("text/plain")
            putExtra(Intent.EXTRA_TEXT, url)
        }
    val chooser = Intent.createChooser(intent, "Choose an application:")
    startActivity(chooser)
}
