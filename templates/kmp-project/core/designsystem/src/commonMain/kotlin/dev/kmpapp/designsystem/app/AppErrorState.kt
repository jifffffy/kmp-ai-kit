package dev.kmpapp.designsystem.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import dev.kmpapp.designsystem.DesignSystemResources
import dev.kmpapp.designsystem.XButton
import dev.kmpapp.designsystem.XIcon
import dev.kmpapp.designsystem.XText

/**
 * Shared, project-level **Failed** state — one design per project, reused by every feature for
 * Rule 4's `UiState.Failed`. Features call this instead of re-implementing a per-screen
 * `FailedContent`.
 *
 * Copy and navigation are **parameters**, so nothing app-specific is baked in:
 *  - [title]/[message] come from the calling feature's own string resources (feature-specific copy);
 *  - [onRetry] is the primary action; [retryLabel] defaults to the shared design-system label;
 *  - [secondaryAction] is an optional slot for feature navigation (e.g. a "Return to …" button).
 *
 * Lives in the [dev.kmpapp.designsystem.app] tier (project-owned composed UI): `install.sh`
 * resets it to a neutral default for downstream projects, which redesign it via the design
 * pipeline. Built only from generic primitives ([XButton], [XIcon], [XText]);
 * never imported by generic (root) design-system code.
 */
@Composable
fun AppErrorState(
    title: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = stringResource(DesignSystemResources.string.retry_label),
    secondaryAction: (@Composable () -> Unit)? = null,
) {
    val background = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val error = MaterialTheme.colorScheme.error

    Box(
        modifier = modifier.fillMaxSize().background(background),
        contentAlignment = Alignment.Center,
    ) {
        // Decorative bottom image — abstract premium texture
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(265.dp)
                    .alpha(0.2f),
        ) {
            Image(
                painter = painterResource(DesignSystemResources.drawable.failed_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // Main content
        Column(
            modifier =
                Modifier
                    .widthIn(max = 448.dp)
                    .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Icon hero with glow
            Box(
                modifier = Modifier.padding(bottom = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(96.dp)
                            .background(error.copy(alpha = 0.1f), CircleShape),
                )
                XIcon(
                    painter = painterResource(DesignSystemResources.drawable.warning),
                    contentDescription = null,
                    tint = error,
                    modifier = Modifier.size(80.dp),
                )
            }

            // Title
            XText(
                text = title,
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                    ),
                color = onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // Message
            XText(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 240.dp).padding(bottom = 32.dp),
            )

            // Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                XButton(
                    onClick = onRetry,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .widthIn(max = 200.dp)
                            .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = onPrimary,
                        ),
                ) {
                    XText(
                        text = retryLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
                secondaryAction?.invoke()
            }
        }
    }
}
