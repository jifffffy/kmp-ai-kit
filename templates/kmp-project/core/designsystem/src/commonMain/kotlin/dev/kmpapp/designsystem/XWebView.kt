package dev.kmpapp.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.multiplatform.webview.request.RequestInterceptor
import com.multiplatform.webview.request.WebRequest
import com.multiplatform.webview.request.WebRequestInterceptResult
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import dev.kmpapp.common.ext.doOnTrue
import dev.kmpapp.common.util.isOnIOS

@Composable
fun XStaticWebView(
    data: String,
    onLinkClick: ((url: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    backRequested: Boolean,
    navigateBackCompleted: (canGoBack: Boolean) -> Unit,
) {
    val webViewState =
        rememberWebViewStateWithHTMLData(
            data = data,
        )

    val navigator =
        rememberWebViewNavigator(
            requestInterceptor =
                object : RequestInterceptor {
                    override fun onInterceptUrlRequest(
                        request: WebRequest,
                        navigator: WebViewNavigator,
                    ): WebRequestInterceptResult {
                        if (request.url.isEmpty() || isOnIOS()) {
                            return WebRequestInterceptResult.Allow
                        }

                        onLinkClick?.invoke(request.url)
                        // Reject it in order to open it in device browser/intent
                        return WebRequestInterceptResult.Reject
                    }
                },
        )

    if (backRequested) {
        navigator.canGoBack.doOnTrue {
            navigator.navigateBack()
        }
        navigateBackCompleted(navigator.canGoBack)
    }

    val bgColor = MaterialTheme.colorScheme.background
    WebView(
        state = webViewState,
        modifier = modifier,
        captureBackPresses = false,
        onCreated = { _ ->
            webViewState.webSettings.apply {
                isJavaScriptEnabled = false
                backgroundColor = bgColor
            }
        },
        navigator = navigator,
    )
}
