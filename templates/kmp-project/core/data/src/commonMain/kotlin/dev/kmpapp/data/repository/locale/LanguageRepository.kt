package dev.kmpapp.data.repository.locale

import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the selected language tag and persists it across launches.
 *
 * Exposed as a Koin singleton (see `LocalDataSourceModule.kt`). The app root collects
 * [language] and feeds it to `LocalAppLocale`; the picker calls [setLanguage].
 *
 * `null` ⇒ follow the system locale.
 */
interface LanguageRepository {
    /** Selected BCP-47 language tag, or `null` to follow the system locale. */
    val language: StateFlow<String?>

    fun setLanguage(tag: String?)
}
