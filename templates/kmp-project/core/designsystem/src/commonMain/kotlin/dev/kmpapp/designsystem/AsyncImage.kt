package dev.kmpapp.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Wrap the third-party image-loaders to
 */
@Composable
fun AsyncImage(
    url: String,
    modifier: Modifier = Modifier,
    loadingResId: DrawableResource? = null,
    contentDescription: String? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    colorFilter: ColorFilter? = null,
    onDrawableReady: () -> Unit = {},
) {
    AsyncImageInternal(
        model = url,
        modifier = modifier,
        loadingResId = loadingResId,
        contentDescription = contentDescription,
        alignment = alignment,
        contentScale = contentScale,
        colorFilter = colorFilter,
        onDrawableReady = onDrawableReady,
    )
}

@Composable
private fun AsyncImageInternal(
    model: Any?,
    modifier: Modifier,
    loadingResId: DrawableResource?,
    contentDescription: String?,
    alignment: Alignment,
    contentScale: ContentScale,
    colorFilter: ColorFilter?,
    onDrawableReady: () -> Unit = {},
) {
    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        alignment = alignment,
        contentScale = contentScale,
        colorFilter = colorFilter,
    ) {
        val state by painter.state.collectAsStateWithLifecycle()

        when (state) {
            is AsyncImagePainter.State.Loading,
            is AsyncImagePainter.State.Error,
            -> {
                if (loadingResId != null) {
                    Image(
                        painter = painterResource(loadingResId),
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        alignment = alignment,
                        contentScale = contentScale,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
            is AsyncImagePainter.State.Success -> {
                SubcomposeAsyncImageContent(
                    modifier = Modifier.fillMaxSize(),
                    alignment = alignment,
                    contentScale = contentScale,
                    colorFilter = colorFilter,
                )

                onDrawableReady.invoke()
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
