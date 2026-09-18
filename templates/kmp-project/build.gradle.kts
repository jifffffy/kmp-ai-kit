import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

// Define SDK versions once at root
val compileSdkVer: Int by lazy {
    libs.versions.android.compileSdk
        .get()
        .toInt()
}
val minSdkVer: Int by lazy {
    libs.versions.android.minSdk
        .get()
        .toInt()
}
val targetSdkVer: Int by lazy {
    libs.versions.android.targetSdk
        .get()
        .toInt()
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinKsp) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.mokkery) apply false
}

val koverIncludedClasses =
    listOf(
        "*ViewModel",
        "*ViewModel\$*",
        "*Repository",
        "*RepositoryImpl",
        "*Repository\$*",
        "*DataSource",
        "*DataSourceImpl",
        "*RemoteDataSource",
        "*RemoteDataSourceImpl",
        "*ScreenKt",
    )
val koverExcludedClasses = listOf("*Test", "*Test\$*", "*Fixtures*")
dependencies {
}

// Aggregate coverage from all feature modules

allprojects {
    plugins.apply("org.jlleitschuh.gradle.ktlint")
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
        outputColorName.set("RED")
        ignoreFailures.set(true)
    }

    // Apply Kover configuration to all modules with the plugin (except root)
    if (project != rootProject) {
        pluginManager.withPlugin("org.jetbrains.kotlinx.kover") {
            extensions.configure<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension> {
                currentProject {
                    sources {
                        excludedSourceSets.addAll("androidTest", "iosTest")
                    }
                }

                reports {
                    filters {
                        includes { classes(koverIncludedClasses) }
                        excludes { classes(koverExcludedClasses) }
                    }

                    total {
                        xml {
                            onCheck = true
                            xmlFile =
                                layout.buildDirectory
                                    .file("reports/kover/report.xml")
                                    .get()
                                    .asFile
                        }
                        html {
                            title = "${project.name} Coverage"
                            onCheck = true
                            htmlDir =
                                layout.buildDirectory
                                    .dir("reports/kover/html")
                                    .get()
                                    .asFile
                        }
                        verify {
                            onCheck = true
                            rule {
                                bound {
                                    minValue = 80
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Configure Android application/library modules
    pluginManager.withPlugin("com.android.application") {
        extensions.configure<ApplicationExtension> {
            compileSdk = compileSdkVer
            defaultConfig {
                minSdk = minSdkVer
                targetSdk = targetSdkVer
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
    }
    // Configure Java projects
    extensions.findByType<JavaPluginExtension>()?.apply {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    // Configure Kotlin/JVM projects
    extensions.findByType<KotlinJvmProjectExtension>()?.apply {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }

    // Configure JVM target for KMP jvm("desktop") targets
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension> {
            targets.withType(KotlinJvmTarget::class.java).configureEach {
                compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }

    // Configure Kotlin Multiplatform library modules
    pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
        extensions.configure<KotlinMultiplatformExtension> {
            targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach {
                compileSdk = compileSdkVer
                minSdk = minSdkVer
                androidResources.enable = true
            }
        }
    }
}

// Deterministic architecture checker (see shared/scripts/kmp_check.py, vendored by
// the KMP AI Kit). Thin wrapper on purpose — all logic lives in the script so it stays
// runnable standalone from CI, a pre-commit hook, or a project without this task.
tasks.register<Exec>("archTest") {
    group = "verification"
    description = "Checks feature modules against the KMP architecture rules."
    workingDir = rootDir
    commandLine("python3", "shared/scripts/kmp_check.py", "--all")
    doFirst {
        val onPath =
            System.getenv("PATH")
                ?.split(File.pathSeparator)
                ?.any { dir -> File(dir, "python3").canExecute() || File(dir, "python3.exe").canExecute() }
                ?: false
        if (!onPath) {
            throw GradleException(
                "archTest needs python3 on PATH (macOS: `brew install python`, " +
                    "Windows: python.org installer + Git Bash). Or run the checker directly: " +
                    "python3 shared/scripts/kmp_check.py --all",
            )
        }
    }
}

// Root project Kover configuration for aggregating all feature modules
kover {
    reports {
        filters {
            includes { classes(koverIncludedClasses) }
            excludes { classes(koverExcludedClasses) }
        }

        total {
            xml {
                onCheck = true
                xmlFile =
                    layout.buildDirectory
                        .file("reports/kover/coverage.xml")
                        .get()
                        .asFile
            }
            html {
                title = "App Coverage"
                onCheck = true
                htmlDir =
                    layout.buildDirectory
                        .dir("reports/kover/html")
                        .get()
                        .asFile
            }
        }
    }
}
