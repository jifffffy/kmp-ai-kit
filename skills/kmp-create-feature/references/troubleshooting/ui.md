# UI Layer Troubleshooting

Quick fixes for common UI layer errors.

## Unresolved References - UiState/setState

**Error:** `Unresolved reference: UiState` or `Unresolved reference: setState`

**Fix:**
```kotlin
// In file (replace {CORE_COMMON_PKG} with actual package):
import {CORE_COMMON_PKG}.UiState
import {CORE_COMMON_PKG}.setState

// In build.gradle.kts commonMain:
implementation(projects.core.common)

// Use: _state.setState { copy(loading = true) }
// NOT: _state.value = _state.value.copy(loading = true)
```

## X-Components Not Found

**Error:** `Unresolved reference: XButton/XText/XScreen/XTheme`

**Fix:**
```kotlin
// In file (replace {CORE_DESIGNSYSTEM_PKG} with actual package):
import {CORE_DESIGNSYSTEM_PKG}.XButton
import {CORE_DESIGNSYSTEM_PKG}.XText
import {CORE_DESIGNSYSTEM_PKG}.XScreen
import {CORE_DESIGNSYSTEM_PKG}.XTopAppBar
import {CORE_DESIGNSYSTEM_PKG}.XTheme

// In build.gradle.kts commonMain:
implementation(projects.core.designsystem)
```

## ImmutableList

**Error:** `Unresolved reference: toImmutableList`

**Fix:**
```kotlin
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.ImmutableList

// In build.gradle.kts:
implementation(libs.kotlinx.collections.immutable)
```

## Material3 Components Used

**Error:** Feature UI should use X-components, not Material3

**Fix:** Replace Material3 with X-components:
```kotlin
// Wrong:
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold

// Right (replace {CORE_DESIGNSYSTEM_PKG} with actual package):
import {CORE_DESIGNSYSTEM_PKG}.XButton
import {CORE_DESIGNSYSTEM_PKG}.XText
import {CORE_DESIGNSYSTEM_PKG}.XScreen   // feature screen container (Rule 13) — not XScaffold
```

**Note:** Reference `kmp-using-design-system` skill for complete mappings

## Missing UI States

**Error:** Not handling all 4 UI states

**Fix:** Ensure when expression covers all states:
```kotlin
when (val state = uiModel.dataState) {
    UiState.Uninitialized -> { /* Empty */ }
    UiState.Loading -> { XCircularProgressIndicator() }
    is UiState.Success -> { ContentView(state.value) }
    is UiState.Failed -> { ErrorView(state.error, onRetry) }
}
```

## ViewModel Binding

**Error:** `No definition found for ProfileViewModel`

**Fix:** Add to DI module:
```kotlin
viewModelOf(::ProfileViewModel)
```

## Build Commands

```bash
# Fast validation (UI layer only)
./gradlew :feature:{featurename}:assembleAndroidMain

# Full validation (entire project)
./gradlew assembleDebug

# With stacktrace for detailed errors
./gradlew :feature:{featurename}:assembleAndroidMain --stacktrace
```

## General Strategy

1. Read error line number carefully
2. Check architecture/ui.md for correct pattern
3. Reference the kmp-using-design-system skill for X-components
4. Fix and rebuild with `:assembleAndroidMain`
5. If stuck after 3 attempts, escalate to user
