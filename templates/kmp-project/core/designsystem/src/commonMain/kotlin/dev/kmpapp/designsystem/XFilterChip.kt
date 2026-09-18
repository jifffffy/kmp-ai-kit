package dev.kmpapp.designsystem

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

@Composable
fun XFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = XFilterChipDefaults.Shape,
    colors: SelectableChipColors = XFilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border =
            FilterChipDefaults.filterChipBorder(
                enabled = enabled,
                selected = selected,
            ),
        interactionSource = interactionSource,
    )
}

object XFilterChipDefaults {
    internal val Shape: Shape
        @ReadOnlyComposable
        @Composable
        get() = CircleShape

    @Composable
    fun filterChipColors(
        containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
        labelColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
        iconColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContainerColor: androidx.compose.ui.graphics.Color =
            MaterialTheme.colorScheme.surface.copy(
                alpha = 0.38f,
            ),
        disabledLabelColor: androidx.compose.ui.graphics.Color =
            MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.38f,
            ),
        disabledLeadingIconColor: androidx.compose.ui.graphics.Color =
            MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.38f,
            ),
        disabledTrailingIconColor: androidx.compose.ui.graphics.Color =
            MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.38f,
            ),
        selectedContainerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedLeadingIconColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTrailingIconColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimaryContainer,
    ): SelectableChipColors =
        FilterChipDefaults.filterChipColors(
            containerColor = containerColor,
            labelColor = labelColor,
            iconColor = iconColor,
            disabledContainerColor = disabledContainerColor,
            disabledLabelColor = disabledLabelColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            selectedContainerColor = selectedContainerColor,
            selectedLabelColor = selectedLabelColor,
            selectedLeadingIconColor = selectedLeadingIconColor,
            selectedTrailingIconColor = selectedTrailingIconColor,
        )
}
