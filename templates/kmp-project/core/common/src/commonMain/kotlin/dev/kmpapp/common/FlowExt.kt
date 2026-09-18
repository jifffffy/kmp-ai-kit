package dev.kmpapp.common

import kotlinx.coroutines.flow.MutableStateFlow

inline fun <T> MutableStateFlow<T>.setState(function: T.() -> T) {
    value = function(value)
}
