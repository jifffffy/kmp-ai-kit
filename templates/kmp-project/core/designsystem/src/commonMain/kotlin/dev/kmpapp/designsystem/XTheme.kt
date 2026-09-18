package dev.kmpapp.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kmpapp.core.designsystem.generated.resources.Res
import kmpapp.core.designsystem.generated.resources.manrope_variable
import org.jetbrains.compose.resources.Font

object XTheme {
    object Icons

    object Colors {
        // Semantic status colors — no M3 role equivalent
        val Success = Color(0xFF4ADE80) // Income, savings progress, on-track budgets
        val Danger = Color(0xFFFF6B6B) // Over-budget, expenses, overdue bills
        val Bitcoin = Color(0xFFF7931A) // Bitcoin brand orange for coin icon bg
    }
}

@Composable
fun XTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        content = content,
        colorScheme = if (darkTheme) XDarkColors else XLightColors,
        shapes = Shapes,
        typography = XTypography(),
    )
}

private val Shapes =
    Shapes(
        small = RoundedCornerShape(6.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(20.dp),
    )

internal val XLightColors =
    lightColorScheme(
        primary = Color(0xFF6B5000),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFDEDB5),
        onPrimaryContainer = Color(0xFF221A00),
        secondary = Color(0xFF5A5100),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFEFE480),
        onSecondaryContainer = Color(0xFF1A1800),
        background = Color(0xFFFFFBF0),
        surface = Color(0xFFFFFDF6),
        onBackground = Color(0xFF1E1B10),
        onSurface = Color(0xFF1E1B10),
        onSurfaceVariant = Color(0xFF4E4730),
        surfaceVariant = Color(0xFFEAE0C5),
        surfaceContainer = Color(0xFFF5EDD8),
        surfaceContainerHigh = Color(0xFFEDE4CE),
        surfaceContainerHighest = Color(0xFFE5DCC4),
        surfaceContainerLow = Color(0xFFFAF3E2),
        surfaceContainerLowest = Color(0xFFFFFDF6),
        outline = Color(0xFF7A7250),
        outlineVariant = Color(0xFFCDC1A0),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
    )

internal val XDarkColors =
    darkColorScheme(
        primary = Color(0xFFF5D76E),
        onPrimary = Color(0xFF2C1900),
        primaryContainer = Color(0xFF4A3200),
        onPrimaryContainer = Color(0xFFFFF0C0),
        secondary = Color(0xFFE8D44D),
        onSecondary = Color(0xFF1A1800),
        secondaryContainer = Color(0xFF3A3500),
        onSecondaryContainer = Color(0xFFEDE27A),
        background = Color(0xFF0F0D09),
        surface = Color(0xFF1C1910),
        onBackground = Color(0xFFEDE8D5),
        onSurface = Color(0xFFEDE8D5),
        onSurfaceVariant = Color(0xFFC4BA94),
        surfaceVariant = Color(0xFF302B1C),
        surfaceContainer = Color(0xFF231F12),
        surfaceContainerHigh = Color(0xFF2A2617),
        surfaceContainerHighest = Color(0xFF312C1C),
        surfaceContainerLow = Color(0xFF1C1910),
        surfaceContainerLowest = Color(0xFF0F0D09),
        outline = Color(0xFF726A48),
        outlineVariant = Color(0xFF3F3822),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )

/**
 * App-global type scale: the Material 3 [Typography] with every role re-pointed at
 * the project [FontFamily] (Manrope variable). M3 has no `defaultFontFamily`, so the
 * family is applied per-role via `.copy(fontFamily = …)`; each role keeps its M3 default
 * size/weight/line-height and the family resolves the matching weight via variation settings.
 */
@Composable
private fun XFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.manrope_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(Res.font.manrope_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
        Font(Res.font.manrope_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
        Font(Res.font.manrope_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
        Font(Res.font.manrope_variable, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
    )

@Composable
private fun XTypography(): Typography {
    val fontFamily = XFontFamily()
    return with(MaterialTheme.typography) {
        copy(
            displayLarge = displayLarge.copy(fontFamily = fontFamily),
            displayMedium = displayMedium.copy(fontFamily = fontFamily),
            displaySmall = displaySmall.copy(fontFamily = fontFamily),
            headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
            headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
            headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
            titleLarge = titleLarge.copy(fontFamily = fontFamily),
            titleMedium = titleMedium.copy(fontFamily = fontFamily),
            titleSmall = titleSmall.copy(fontFamily = fontFamily),
            bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
            bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
            bodySmall = bodySmall.copy(fontFamily = fontFamily),
            labelLarge = labelLarge.copy(fontFamily = fontFamily),
            labelMedium = labelMedium.copy(fontFamily = fontFamily),
            labelSmall = labelSmall.copy(fontFamily = fontFamily),
        )
    }
}
