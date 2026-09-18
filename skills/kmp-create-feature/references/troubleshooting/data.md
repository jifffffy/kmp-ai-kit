# Data Layer Troubleshooting

Quick fixes for common data layer errors.

## Unresolved References - Either/ErrorModel

**Error:** `Unresolved reference: Either` or `Unresolved reference: ErrorModel`

**Fix:** Add imports + dependency (use project's core package):
```kotlin
// In file (replace {CORE_COMMON_PKG} with actual package):
import {CORE_COMMON_PKG}.Either
import {CORE_COMMON_PKG}.ErrorModel

// In build.gradle.kts commonMain:
implementation(projects.core.common)
```

## Package/Directory Mismatch

**Error:** `Package directive doesn't match file location`

**Fix:** Package must be lowercase, no hyphens:
```kotlin
// Wrong: package com.example.user-profile or com.example.userProfile
// Right: package com.example.userprofile
```

## Serialization

**Error:** `Serializer not found for type X`

**Fix:**
```kotlin
@Serializable data class X(...) // Add @Serializable

// In build.gradle.kts:
plugins { alias(libs.plugins.kotlinSerialization) }
commonMain.dependencies { implementation(libs.kotlinx.serialization.json) }
```

## Repository/DataSource Binding

**Error:** `No definition found for UserRepository` or `No definition found for UserRemoteDataSource`

**Fix:** Bind interfaces in DI:
```kotlin
// Wrong: singleOf(::UserRepositoryImpl)
// Right: singleOf(::UserRepositoryImpl).bind<UserRepository>()

// Wrong: singleOf(::UserRemoteDataSourceImpl)
// Right: singleOf(::UserRemoteDataSourceImpl).bind<UserRemoteDataSource>()
```

## Module Not Found

**Error:** `Project ':feature:x' not found`

**Fix:** Add to `settings.gradle.kts`:
```kotlin
include(":feature:x")
```

## Build Commands

```bash
# Fast validation (data layer only)
./gradlew :feature:{featurename}:assembleAndroidMain

# Full validation (entire project)
./gradlew assembleDebug

# With stacktrace for detailed errors
./gradlew :feature:{featurename}:assembleAndroidMain --stacktrace
```

## General Strategy

1. Read error line number carefully
2. Check architecture/data.md for correct pattern
3. Compare your code to pattern
4. Fix and rebuild with `:assembleAndroidMain`
5. If stuck after 3 attempts, escalate to user
