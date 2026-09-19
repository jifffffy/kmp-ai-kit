import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    android {
        namespace = "dev.kmpapp"
    }
    jvm("desktop")

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.util)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
            implementation(libs.kotlinxSerialization)
            implementation(libs.jetbrains.compose.navigation)
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(project(":core:data"))
            implementation(project(":core:common"))
            implementation(project(":core:designsystem"))
        }

        // The desktop target is declared above (`jvm("desktop")`); without a
        // desktopMain block it has no compose-desktop runtime and the target can
        // compile but never run.
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

// Desktop entry point. The task is `:composeApp:run` — NOT `:composeApp:desktopRun`,
// which ignores this block and fails with "No main class specified".
compose.desktop {
    application {
        mainClass = "dev.kmpapp.MainKt"
    }
}

base.archivesName.set("KmpApp-${libs.versions.android.versionName.get()}")

buildkonfig {
    packageName = "dev.kmpapp"

    defaultConfigs {
        buildConfigField(
            INT,
            "VERSION_CODE",
            libs.versions.android.versionCode
                .get(),
        )
        buildConfigField(
            STRING,
            "VERSION_NAME",
            libs.versions.android.versionName
                .get(),
        )
    }

    // Develop flavor
    defaultConfigs("develop") {
        buildConfigField(STRING, "BASE_URL", "https://api.example.com/")
        buildConfigField(STRING, "FLAVOR_NAME", "develop")
    }

    // Production flavor
    defaultConfigs("production") {
        buildConfigField(STRING, "BASE_URL", "https://api.example.com/")
        buildConfigField(STRING, "FLAVOR_NAME", "production")
    }
}
