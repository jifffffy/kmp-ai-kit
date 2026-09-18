package dev.kmpapp.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

fun Modifier.invisible(hide: Boolean): Modifier =
    then(
        layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                if (hide.not()) placeable.place(0, 0)
            }
        },
    )
