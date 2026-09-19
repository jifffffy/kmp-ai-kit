package dev.kmpapp.data.di

import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

/**
 * Desktop (JVM) actual: supplies the on-disk location the shared DataStore binding
 * resolves via [DataStorePathStringQualifier].
 *
 * This module used to be empty, which meant the desktop target compiled but crashed on
 * startup: `PreferenceDataStoreFactory.createWithPath` had no path to inject.
 *
 * Stores under the user's home directory so preferences survive between runs. The parent
 * directory is created up front — `createWithPath` expects it to exist.
 */
internal actual val platformLocalDataSourceModule: Module =
    module {
        single<String>(qualifier = DataStorePathStringQualifier) {
            val directory =
                File(System.getProperty("user.home"), ".kmpapp").apply { mkdirs() }
            File(directory, DATA_STORE_FILE_NAME).absolutePath
        }
    }
