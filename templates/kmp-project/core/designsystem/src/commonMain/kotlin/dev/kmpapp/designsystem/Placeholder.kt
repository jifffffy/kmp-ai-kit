package dev.kmpapp.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Placeholder(
    modifier: Modifier,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Box(modifier = Modifier.sizeIn(maxWidth = 150.dp)) { icon() }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (title != null) {
            ProvideTextStyle(
                value =
                    LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    ),
            ) {
                title()
            }
        }

        if (subtitle != null) {
            ProvideTextStyle(value = subtitleTextStyle) {
                Spacer(modifier = Modifier.height(2.dp))
                subtitle()
            }
        }

        if (action != null) {
            Spacer(modifier = Modifier.height(16.dp))

            action()
        }
    }
}

private val subtitleTextStyle: TextStyle
    @Composable
    get() =
        LocalTextStyle.current.copy(
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
