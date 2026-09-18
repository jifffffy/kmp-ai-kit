package dev.kmpapp.common.ext

/**
 * This kotlin file contains all boolean extension functions which is defined for this project
 */
fun <T> Boolean.doOnTrue(onTrue: (Boolean) -> T): T? =
    if (this) {
        onTrue.invoke(this)
    } else {
        null
    }

fun <T> Boolean.doOnFalse(onFalse: (Boolean) -> T): T? =
    if (!this) {
        onFalse.invoke(this)
    } else {
        null
    }

fun <T, R> Boolean.doOnTrueOrFalse(
    onTrue: (Boolean) -> T,
    onFalse: (Boolean) -> R,
) {
    if (this) {
        onTrue.invoke(this)
    } else {
        onFalse.invoke(this)
    }
}

fun Boolean.toggle(): Boolean = !this
