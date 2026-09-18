package dev.kmpapp.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kmpapp.common.ErrorModel

enum class SupportType { Error, Success, Info }

@Composable
fun SupportText(
    text: String?,
    type: SupportType,
) {
    AnimatedVisibility(
        visible = text != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Text(
            text = text.orEmpty(),
            modifier = Modifier.padding(start = 8.dp),
            color =
                when (type) {
                    SupportType.Error -> MaterialTheme.colorScheme.error
                    SupportType.Success -> XTheme.Colors.Success
                    SupportType.Info -> Color.Unspecified
                },
            fontSize = 11.sp,
        )
    }
}

@Composable
fun AnimatedError(error: ErrorModel?) {
    SupportText(
        text = error?.asString(),
        type = SupportType.Error,
    )
}
