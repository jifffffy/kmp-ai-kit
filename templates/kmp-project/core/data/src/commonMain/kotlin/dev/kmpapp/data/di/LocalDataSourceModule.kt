package dev.kmpapp.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.QualifierValue
import org.koin.dsl.bind
import org.koin.dsl.module
import dev.kmpapp.data.datasource.local.locale.LanguageLocalDataSource
import dev.kmpapp.data.datasource.local.locale.LanguageLocalDataSourceImpl
import dev.kmpapp.data.datasource.local.theme.ThemeLocalDataSource
import dev.kmpapp.data.datasource.local.theme.ThemeLocalDataSourceImpl
import dev.kmpapp.data.datasource.local.token.TokenLocalDataSource
import dev.kmpapp.data.datasource.local.token.TokenLocalDataSourceImpl
import dev.kmpapp.data.local.pref.PreferencesManager
import dev.kmpapp.data.repository.locale.LanguageRepository
import dev.kmpapp.data.repository.locale.LanguageRepositoryImpl
import dev.kmpapp.data.repository.theme.ThemeRepository
import dev.kmpapp.data.repository.theme.ThemeRepositoryImpl
import dev.kmpapp.data.repository.token.TokenRepository
import dev.kmpapp.data.repository.token.TokenRepositoryImpl

internal const val DATA_STORE_FILE_NAME = "prefs.preferences_pb"

object DataStorePathStringQualifier : Qualifier {
    override val value: QualifierValue
        get() = DataStorePathStringQualifier::class.simpleName.toString()
}

internal expect val platformLocalDataSourceModule: Module

internal val localDataSourceModule =
    module {
        single { PreferencesManager(get()) }

        single<DataStore<Preferences>> {
            PreferenceDataStoreFactory.createWithPath(
                produceFile = { get<String>(qualifier = DataStorePathStringQualifier).toPath() },
            )
        }

        // Token
        singleOf(::TokenLocalDataSourceImpl).bind<TokenLocalDataSource>()
        singleOf(::TokenRepositoryImpl).bind<TokenRepository>()

        // Theme
        singleOf(::ThemeLocalDataSourceImpl).bind<ThemeLocalDataSource>()
        singleOf(::ThemeRepositoryImpl).bind<ThemeRepository>()

        // Locale
        singleOf(::LanguageLocalDataSourceImpl).bind<LanguageLocalDataSource>()
        singleOf(::LanguageRepositoryImpl).bind<LanguageRepository>()
    }

/*
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "X-db",
        ).build()
    }*/
