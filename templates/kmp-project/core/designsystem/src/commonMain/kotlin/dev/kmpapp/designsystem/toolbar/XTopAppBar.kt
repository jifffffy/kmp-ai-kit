package dev.kmpapp.designsystem.toolbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kmpapp.core.designsystem.generated.resources.Res
import kmpapp.core.designsystem.generated.resources.app_logo_type
import org.jetbrains.compose.resources.painterResource
import dev.kmpapp.designsystem.util.ContentAlpha
import dev.kmpapp.designsystem.util.LocalContentAlpha

enum class XTopAppBarAlignment {
    Start,
    Center,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    alignment: XTopAppBarAlignment = XTopAppBarAlignment.Start,
) {
    val titleSlut = @Composable {
        // because of Material3's component has its own contentColor or textStyle and colors and the app is
        // based on old material we should provide/override requirements of old one.
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            LocalContentColor provides LocalContentColor.current,
            LocalContentAlpha provides ContentAlpha.high,
        ) {
            title()
        }
    }
    val navigationIconSlut = @Composable {
        CompositionLocalProvider(
            LocalContentColor provides LocalContentColor.current,
            LocalContentAlpha provides ContentAlpha.high,
        ) {
            navigationIcon?.invoke()
        }
    }
    val actionsSlut: @Composable RowScope.() -> Unit = {
        CompositionLocalProvider(
            LocalContentColor provides LocalContentColor.current,
            LocalContentAlpha provides ContentAlpha.medium,
        ) {
            actions()
        }
    }
    val colors =
        TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            scrolledContainerColor = contentColor,
            navigationIconContentColor = contentColor,
            titleContentColor = contentColor,
            actionIconContentColor = contentColor,
        )

    when (alignment) {
        XTopAppBarAlignment.Center ->
            CenterAlignedTopAppBar(
                modifier = modifier,
                title = titleSlut,
                navigationIcon = navigationIconSlut,
                actions = actionsSlut,
                colors = colors,
                windowInsets = WindowInsets(0, 0, 0, 0),
            )

        XTopAppBarAlignment.Start ->
            TopAppBar(
                modifier = modifier,
                title = titleSlut,
                navigationIcon = navigationIconSlut,
                actions = actionsSlut,
                colors = colors,
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
    }
}

@Composable
fun XModalTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    backgroundColor: Color = Color.Transparent,
    contentColor: Color = contentColorFor(backgroundColor),
) {
    // swapping actions and navigation sluts because of implementing custom modals appbar design
    // this code may have some bugs in different situations
    XTopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions()
            }
        },
        actions = { navigationIcon?.invoke() },
        backgroundColor = backgroundColor,
        contentColor = contentColor,
    )
}

@Composable
fun XTopLogo(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(Res.drawable.app_logo_type),
            contentDescription = null,
            modifier = Modifier.size(44.dp),
        )
    }
}
