package dev.kmpapp.data.datasource.local.locale

import kotlinx.coroutines.flow.first
import dev.kmpapp.data.local.pref.PreferencesManager

internal class LanguageLocalDataSourceImpl(
    private val preferencesManager: PreferencesManager,
) : LanguageLocalDataSource {
    override suspend fun getLanguageTag(): String? = preferencesManager.getString(KEY).first()?.takeIf { it.isNotEmpty() }

    override suspend fun setLanguageTag(tag: String?) {
        preferencesManager.putString(KEY, tag.orEmpty())
    }

    private companion object {
        const val KEY = "language_tag"
    }
}
