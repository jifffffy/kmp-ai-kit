package dev.kmpapp.designsystem.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.kmpapp.designsystem.XCircularProgressIndicator

/**
 * Shared, project-level **Loading** state — one design per project, reused by every feature for
 * Rule 4's `UiState.Loading`. Features call this instead of re-implementing a per-screen
 * `LoadingContent`.
 *
 * Lives in the [dev.kmpapp.designsystem.app] tier: this is the *project's own* composed UI
 * (not a generic primitive), so `install.sh` resets it to a neutral default for downstream
 * projects, which then redesign it via the design pipeline. Built only from generic primitives
 * ([XCircularProgressIndicator]); never imported by generic (root) design-system code.
 */
@Composable
fun AppLoadingState(modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.fillMaxSize().background(background),
        contentAlignment = Alignment.Center,
    ) {
        // Atmospheric radial gradient — subtle depth
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .alpha(0.2f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(surfaceVariant.copy(alpha = 0.2f), background, background),
                        ),
                    ),
        )

        // Spinner + center dot
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .size(64.dp)
                        .border(4.dp, surfaceVariant, CircleShape),
            )
            XCircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = primary,
                strokeWidth = 4.dp,
            )
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(primary, CircleShape),
            )
        }

        // Bottom branding accent
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .alpha(0.1f),
        ) {
            Box(
                modifier =
                    Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, primary, Color.Transparent),
                            ),
                            CircleShape,
                        ),
            )
        }
    }
}
