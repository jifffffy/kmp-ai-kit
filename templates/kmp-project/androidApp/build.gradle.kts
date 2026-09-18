import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "dev.kmpapp.app"

    defaultConfig {
        applicationId = "dev.kmpapp"
        versionCode =
            libs.versions.android.versionCode
                .get()
                .toInt()
        versionName =
            libs.versions.android.versionName
                .get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystoreParent = File("${projectDir.path}/signing")
    val keystorePropFile = File(keystoreParent, "base-keystore.properties")
    if (keystorePropFile.exists()) {
        signingConfigs {
            val props = Properties().apply { load(FileInputStream(keystorePropFile)) }
            create("mainKey") {
                storeFile = File(keystoreParent, props["storeFile"].toString())
                storePassword = props["storePassword"].toString()
                keyAlias = props["keyAlias"].toString()
                keyPassword = props["keyPassword"].toString()
            }
        }
    } else {
        println("signing key not found!")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("mainKey")?.let { signingConfig = it }
        }
        debug {
            versionNameSuffix = "-SNAPSHOT"
        }
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.activityCompose)
    implementation(libs.koin.android)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
}
