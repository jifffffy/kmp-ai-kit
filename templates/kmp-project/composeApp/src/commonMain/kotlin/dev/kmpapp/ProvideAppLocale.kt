package dev.kmpapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import dev.kmpapp.data.repository.locale.LanguageRepository
import dev.kmpapp.designsystem.locale.LocalAppLocale

/**
 * Wraps [content] with the app-wide locale override and matching layout direction.
 *
 * Collects the selected language from [LanguageRepository] and provides it through
 * [LocalAppLocale]; the [key] forces the subtree to recompose — re-resolving every
 * `stringResource` — when the language changes. `null` ⇒ follow the system locale.
 *
 * Layout direction is provided to match the language (RTL for Arabic/Farsi/Hebrew/…,
 * LTR otherwise). When the tag is `null` the inherited (system) direction is kept, so
 * Compose's automatic mirroring still applies.
 *
 * Place once at the app root so callers never touch [LanguageRepository] directly:
 * ```
 * @Composable
 * fun App() = ProvideAppLocale { AppContent() }
 * ```
 *
 * [languageRepository] defaults to the Koin-provided singleton; override it in tests/previews.
 */
@Composable
fun ProvideAppLocale(
    languageRepository: LanguageRepository = koinInject(),
    content: @Composable () -> Unit,
) {
    val language by languageRepository.language.collectAsStateWithLifecycle()
    CompositionLocalProvider(
        LocalAppLocale provides language,
        LocalLayoutDirection provides (language?.let(::layoutDirectionForLanguageTag) ?: LocalLayoutDirection.current),
    ) {
        key(language) {
            content()
        }
    }
}

/**
 * Maps a BCP-47 language tag to its writing direction. There is no cross-platform
 * `isRtl(locale)` API, so RTL languages are matched by their primary language subtag.
 */
internal fun layoutDirectionForLanguageTag(languageTag: String): LayoutDirection {
    val language = languageTag.substringBefore('-').lowercase()
    return if (language in RtlLanguageSubtags) LayoutDirection.Rtl else LayoutDirection.Ltr
}

private val RtlLanguageSubtags =
    setOf(
        "ar", // Arabic
        "fa", // Persian (Farsi)
        "he", // Hebrew
        "iw", // Hebrew (legacy code)
        "ur", // Urdu
        "ps", // Pashto
        "sd", // Sindhi
        "ug", // Uyghur
        "yi", // Yiddish
        "dv", // Divehi / Maldivian
        "ckb", // Central Kurdish (Sorani)
        "prs", // Dari
        "nqo", // N'Ko
        "rhg", // Rohingya
    )
