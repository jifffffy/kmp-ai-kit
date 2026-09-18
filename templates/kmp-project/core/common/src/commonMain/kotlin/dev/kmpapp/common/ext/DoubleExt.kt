package dev.kmpapp.common.ext

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

/**
 * Multiplatform-safe replacement for `"%.<decimals>f".format(this)`.
 * `kotlin.text.String.format` is JVM-only and unresolved on Kotlin/Native.
 */
fun Double.formatDecimals(decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val rounded = round(abs(this) * factor)
    val whole = (rounded / factor).toLong()
    val sign = if (this < 0 && rounded != 0.0) "-" else ""
    return if (decimals > 0) {
        val fraction = (rounded % factor).toLong()
        "$sign$whole.${fraction.toString().padStart(decimals, '0')}"
    } else {
        "$sign$whole"
    }
}

fun Float.formatDecimals(decimals: Int): String = toDouble().formatDecimals(decimals)
