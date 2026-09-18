package dev.kmpapp.common.ext

import java.util.Calendar
import java.util.Date

infix fun Calendar.isNotSameDay(c: Calendar) = !(this isSameDay c)

infix fun Calendar.isSameDay(c: Calendar): Boolean =
    get(Calendar.YEAR) == c.get(Calendar.YEAR) &&
        get(Calendar.MONTH) == c.get(Calendar.MONTH) &&
        get(Calendar.DAY_OF_MONTH) == c.get(Calendar.DAY_OF_MONTH)

fun Long.toCalendar() =
    Calendar.getInstance().apply {
        time = Date(this@toCalendar)
    }
