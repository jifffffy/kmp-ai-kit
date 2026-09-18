package dev.kmpapp.designsystem

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TextFieldCounter(
    count: Int,
    maxLength: Int,
    isError: Boolean,
) {
    Text(
        text = "$count/$maxLength",
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else LocalContentColor.current.copy(alpha = 0.5f),
        modifier = Modifier.padding(end = 8.dp),
    )
}
