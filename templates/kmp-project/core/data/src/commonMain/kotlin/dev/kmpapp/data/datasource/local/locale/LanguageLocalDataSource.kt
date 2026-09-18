package dev.kmpapp.data.datasource.local.locale

/** Reads/writes the persisted BCP-47 language tag. `null` ⇒ follow the system locale. */
internal interface LanguageLocalDataSource {
    suspend fun getLanguageTag(): String?

    suspend fun setLanguageTag(tag: String?)
}
