package dev.kmpapp.data.repository.locale

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dev.kmpapp.data.datasource.local.locale.LanguageLocalDataSource

/**
 * Seeds the language tag from [LanguageLocalDataSource] on creation and writes back on every
 * [setLanguage]; the in-memory [StateFlow] is the synchronous read source for callers.
 */
internal class LanguageRepositoryImpl(
    private val localDataSource: LanguageLocalDataSource,
) : LanguageRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _language = MutableStateFlow<String?>(null)
    override val language: StateFlow<String?> = _language.asStateFlow()

    init {
        scope.launch { localDataSource.getLanguageTag()?.let { _language.value = it } }
    }

    override fun setLanguage(tag: String?) {
        _language.value = tag
        scope.launch { localDataSource.setLanguageTag(tag) }
    }
}
