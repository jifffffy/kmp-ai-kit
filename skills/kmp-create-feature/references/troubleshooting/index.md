# Troubleshooting Quick Reference

Quick fixes for common build errors. For layer-specific issues, see detailed files below.

---

## Layer-Specific Guides

| Layer | File | Common Issues |
|-------|------|---------------|
| Data | [troubleshooting/data.md](data.md) | Either, Serialization, DataSource binding |
| UI | [troubleshooting/ui.md](ui.md) | setState, UiState, X-components |
| Integration | [troubleshooting/integration.md](integration.md) | 4 integration points, DI, Navigation |

---

## Universal Quick Fixes

### Unresolved Reference: Core Types

```kotlin
// Either, ErrorModel, UiState, setState
import {CORE_COMMON_PKG}.Either
import {CORE_COMMON_PKG}.ErrorModel
import {CORE_COMMON_PKG}.UiState
import {CORE_COMMON_PKG}.setState

// X-components
import {CORE_DESIGNSYSTEM_PKG}.XButton
import {CORE_DESIGNSYSTEM_PKG}.XScreen   // feature screen container — not XScaffold
```

### Package Mismatch

```kotlin
// WRONG
package com.example.user-profile
package com.example.userProfile

// RIGHT (lowercase, no hyphens)
package com.example.userprofile
```

### Koin Binding

```kotlin
// WRONG
singleOf(::UserRepositoryImpl)

// RIGHT
singleOf(::UserRepositoryImpl).bind<UserRepository>()
```

### Module Not Found

```kotlin
// Add to settings.gradle.kts
include(":feature:{featurename}")
```

---

## Build Commands

```bash
# Fast validation (per layer)
./gradlew :feature:{featurename}:assembleAndroidMain

# Full validation (integration)
./gradlew assembleDebug

# With stacktrace
./gradlew assembleDebug --stacktrace

# Clean build
./gradlew clean assembleDebug

# Format code
./gradlew ktlintFormat
```

---

## General Strategy

1. Read error message carefully (note line number)
2. Check appropriate troubleshooting file
3. Compare your code to the correct pattern
4. Fix and rebuild
5. If stuck after 3 attempts, escalate to user
