# Integration Troubleshooting

Quick fixes for common integration errors.

## Module Not Included

**Error:** `Project ':feature:x' not found in root project`

**Fix:** Add to `settings.gradle.kts`:
```kotlin
include(":feature:x")
```

## Feature Dependency Not Found

**Error:** `Could not find :feature:x`

**Fix:** Add to `composeApp/build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":feature:x"))
}
```

## DI Module Not Registered

**Error:** `No definition found for class` (at runtime)

**Fix:** List the feature module in `initKoin.kt`'s `modules(...)`:
```kotlin
import {PKG_PREFIX}.featurename.di.featurenameModule

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication =
    startKoin {
        appDeclaration()
        modules(
            appModule,
            commonModule,
            dataModule,
            // ... existing feature modules
            featurenameModule,   // ← add it on its own line
        )
    }
```

## Navigation Extension Not Found

**Error:** `Unresolved reference: featurename (in NavGraphBuilder)`

**Fix:** Import extension in `BaseAppNavHost.kt`:
```kotlin
import {PKG_PREFIX}.featurename.presentation.navigation.featurename
import {PKG_PREFIX}.featurename.presentation.navigation.FeatureRoute
```

## Koin Binding Missing

**Error:** `No definition found for UserRepository`

**Fix:** Bind interfaces in DI module:
```kotlin
// Wrong: singleOf(::UserRepositoryImpl)
// Right: singleOf(::UserRepositoryImpl).bind<UserRepository>()
```

## DI Module Wrong Shape

**Error:** Feature module not found / not loadable from `initKoin`

**Fix:** Expose a single top-level `val` module (no object, no base class, no `initialize()`):
```kotlin
// Wrong:
object ProfileModules {
    val modules = listOf(...)
}

// Right:
val profileModule: Module =
    module {
        singleOf(::ProfileRepositoryImpl).bind<ProfileRepository>()
        viewModelOf(::ProfileViewModel)
        // includes(platformModule)   // only if Rule 14 platform bindings exist
    }
```
Then add `profileModule` to `initKoin`'s `modules(...)`.

## Navigation Route Not Found

**Error:** `Unresolved reference: ProfileRoute`

**Fix:** Ensure route is @Serializable and imported:
```kotlin
// In ProfileNavigation.kt:
@Serializable
data object ProfileRoute

// In BaseAppNavHost.kt:
import {PKG_PREFIX}.profile.presentation.navigation.ProfileRoute
```

## App Still Launches to Welcome Screen (First Feature)

**Symptom:** Build passes, but the app opens the kit installer's placeholder instead of the new feature; the feature is unreachable as start destination.

**Cause:** The first-feature Welcome handoff (Integration Point 4a) was skipped. The build compiles fine with a leftover Welcome, so nothing fails loudly.

**Fix:** Complete the handoff in `{NAV_HOST_PATH}` (`BaseAppNavHost.kt`):
```kotlin
// Before
XNavHost(..., startDestination = WelcomeRoute) {
    composable<WelcomeRoute> { WelcomeScreen() }
}
// After
XNavHost(..., startDestination = {Feature}Route) {
    {featurename}(onBackClick = { navController.navigateUp() })
}
```
Then drop the dead `WelcomeRoute`/`WelcomeScreen` imports and delete the file:
```bash
rm -f composeApp/src/commonMain/kotlin/**/WelcomeScreen.kt
# verify clean — both must return nothing:
find composeApp/src/commonMain/kotlin -name 'WelcomeScreen.kt'
grep -rn "WelcomeRoute" composeApp/src/commonMain/kotlin
```
See architecture/integration.md → "4a. First-feature (Welcome) Handoff".

## Build Commands

```bash
# Full validation (required for integration)
./gradlew assembleDebug

# Clean build if caching issues
./gradlew clean assembleDebug

# Format code
./gradlew ktlintFormat

# With stacktrace for detailed errors
./gradlew assembleDebug --stacktrace
```

## Integration Checklist

If build fails, verify all 4 integration points:
1. ✅ `settings.gradle.kts` includes module
2. ✅ `composeApp/build.gradle.kts` has dependency
3. ✅ `initKoin.kt` initializes modules
4. ✅ `BaseAppNavHost.kt` wires navigation
4a. ✅ First feature only: Welcome handoff done — `startDestination` repointed, `WelcomeScreen.kt` deleted, no `WelcomeRoute` left

## General Strategy

1. Read error line number carefully
2. Check architecture/integration.md for correct pattern
3. Verify all 4 integration points completed
4. Fix and rebuild with `assembleDebug`
5. If stuck after 3 attempts, escalate to user
