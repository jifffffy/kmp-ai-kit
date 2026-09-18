package dev.kmpapp.designsystem

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/**
 * X-Text component that wraps Material3 Text with theme-aware defaults.
 * Use this instead of Material3 Text for consistent theming.
 */
@Composable
fun XText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style,
    )
}

/**
 * Preset text variants for common use cases.
 */
object XTextDefaults {
    /**
     * Title text style with bold weight.
     */
    @Composable
    fun titleStyle(): TextStyle =
        MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
        )

    /**
     * Body text style (default).
     */
    @Composable
    fun bodyStyle(): TextStyle = MaterialTheme.typography.bodyMedium

    /**
     * Label text style for form labels.
     */
    @Composable
    fun labelStyle(): TextStyle = MaterialTheme.typography.labelMedium

    /**
     * Error text style with primary (error) color.
     */
    @Composable
    fun errorStyle(): TextStyle =
        MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.error,
        )
}
