package dev.kmpapp.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subscript
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kmpapp.designsystem.toolbar.XModalTopAppBar

@Composable
fun SimpleDialog(
    onDismissRequest: () -> Unit,
    title: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
    positiveAction: @Composable (RowScope.() -> Unit)? = null,
    negativeAction: @Composable (RowScope.() -> Unit)? = null,
) {
    XDialog(onDismissRequest = onDismissRequest) {
        Column {
            if (title != null) {
                XModalTopAppBar(
                    title = title,
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(imageVector = Icons.Default.Subscript, contentDescription = null)
                        }
                    },
                )
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
            ) {
                content()

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    positiveAction?.invoke(this)
                    negativeAction?.invoke(this)
                }
            }
        }
    }
}
