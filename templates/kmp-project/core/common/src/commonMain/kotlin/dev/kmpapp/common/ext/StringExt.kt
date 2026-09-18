package dev.kmpapp.common.ext

/**
 * Created by Ali Sadeghi
 * on 28,Apr,2025
 */

fun String.isValidEmail(): Boolean {
    val simpleEmailRegex = Regex(".+@.+\\..+")
    return simpleEmailRegex.matches(this)
}
