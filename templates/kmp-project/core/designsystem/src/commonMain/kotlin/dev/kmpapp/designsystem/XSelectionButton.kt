package dev.kmpapp.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun XSelectionButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable RowScope.() -> Unit,
) {
    if (selected) {
        XButton(
            modifier = modifier,
            onClick = onClick,
            content = text,
        )
    } else {
        XTextButton(
            modifier = modifier,
            onClick = onClick,
            content = text,
        )
    }
}

@Composable
fun XSelectionButtonContainer(
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surface, CircleShape),
        content = content,
    )
}
