# Component Mappings: Material3 → X-Design System

**Note**: Replace `{CORE_DESIGNSYSTEM_PKG}` with your project's design system package (e.g., `com.example.designsystem`, `com.myapp.designsystem`)

## Buttons

| Material3 | X-Component | Use Case |
|-----------|------------------|----------|
| `Button` | `XButton` | Primary action button |
| `IconButton` | `XIconButton` | Icon-only button |
| `TextButton` | `XTextButton` | Text-only button (secondary actions) |
| N/A | `XTextIconButton` | Text + icon button |
| `OutlinedButton` | `XOutlinedButton` | Outlined variant |
| N/A | `XOutlinedIconButton` | Outlined icon button |
| N/A | `XButtonProgress` | Button with loading indicator |

**All buttons default to `CircleShape` (fully rounded).**

**Import**: `import {CORE_DESIGNSYSTEM_PKG}.XButton`

**Example**:
```kotlin
// ❌ Material3
Button(onClick = { }) {
    Text("Submit")
}

// ✅ X-component
XButton(onClick = { }) {
    XText("Submit")
}
```

---

## Text Input

| Material3 | X-Component | Use Case |
|-----------|------------------|----------|
| `TextField` / `OutlinedTextField` | `XTextField` | Single/multiline text input |
| N/A | `SearchField` | Search with icon (wraps XTextField) |

**Single-line XTextField uses `CircleShape`, multiline uses `RoundedCornerShape`.**

**Import**: `import {CORE_DESIGNSYSTEM_PKG}.XTextField`

**Example**:
```kotlin
// ❌ Material3
OutlinedTextField(
    value = state.email,
    onValueChange = { viewModel.onEmailChange(it) }
)

// ✅ X-component
XTextField(
    value = state.email,
    onValueChange = { viewModel.onEmailChange(it) }
)
```

---

## Text Display

| Material3 | X-Component | Use Case |
|-----------|------------------|----------|
| `Text` | `XText` | All text display |

**Preset styles**: `XTextDefaults.titleStyle()`, `bodyStyle()`, `labelStyle()`, `errorStyle()`

**Import**: `import {CORE_DESIGNSYSTEM_PKG}.XText`

**Example**:
```kotlin
// ❌ Material3
Text(
    text = "Welcome",
    style = MaterialTheme.typography.titleLarge
)

// ✅ X-component
XText(
    text = "Welcome",
    style = XTextDefaults.titleStyle()
)
```

---

## Layout & Structure

| Material3 | X-Component | Use Case |
|-----------|------------------|----------|
| `Scaffold` (feature screen) | `XScreen` | Feature screen container: topBar/bottomBar/content, **no insets** (Rule 13) |
| `Scaffold` (app shell only) | `XScaffold` | The ONE app-shell Scaffold in `App.kt` — never in a feature |
| `Card` | `XCard` | Card container (clickable or static) |

**XScreen** is a plain `Column { topBar(); Box(weight 1f){content}; bottomBar() }` that touches **no** window insets — the single app-shell `Scaffold` owns them (Rule 13). Feature screens **always** use `XScreen`; nesting an `XScaffold`/`Scaffold` inside a feature double-counts safe-area/nav-bar padding.
**XCard** uses `MaterialTheme.shapes.medium` (12dp).

**Import**: `import {CORE_DESIGNSYSTEM_PKG}.{XScreen, XCard}`

**Example**:
```kotlin
// ❌ Material3 / nested Scaffold inside a feature
Scaffold(topBar = { TopAppBar(...) }) { padding -> /* content */ }

// ❌ XScaffold inside a feature (still a nested Scaffold)
XScaffold(topBar = { XTopAppBar(...) }) { padding -> /* content */ }

// ✅ XScreen — no paddingValues; content fills the weight box
XScreen(
    topBar = { XTopAppBar(...) },
    bottomBar = { /* optional sticky CTA */ },
) {
    // content
}
```

---

## Navigation

| Material3 | X-Component | Use Case |
|-----------|------------------|----------|
| `NavHost` | `XNavHost` | Navigation with pre-configured animations |
| `TopAppBar` | `XTopAppBar` / `XModalTopAppBar` | App bar with back/actions |

**Import**: `import {CORE_DESIGNSYSTEM_PKG}.{XNavHost, XTopAppBar}`

---

## Loading & Feedback

| Material3 | X-Component | Use Case |
|-----------|------------------|----------|
| `CircularProgressIndicator` | `XCircularProgressIndicator` | Loading spinner (primary color) |
| N/A | `XSnackbarHost` + `SnackbarController` | Snackbar messaging |
| N/A | `Toast` | Toast notifications |
| N/A | `XPullRefresh` | Pull-to-refresh |

**Import**: `import {CORE_DESIGNSYSTEM_PKG}.{XCircularProgressIndicator, XSnackbarHost}`

---

## Images

| Library Import | X-Component | Use Case |
|----------------|------------------|----------|
| `coil3.compose.AsyncImage` | `{CORE_DESIGNSYSTEM_PKG}.AsyncImage` | **Dynamic content** image (avatar, flag, thumbnail, product photo) — runtime URL from data, with loading/error states |

**IMPORTANT**: Always use `{CORE_DESIGNSYSTEM_PKG}.AsyncImage`, not `coil3.compose.AsyncImage`. The wrapper's first parameter is **`url`** (a `String`), not `model`.

**Dynamic vs static**: use `AsyncImage(url = …)` only for images whose source is **runtime data** (a URL from the repository/API). A **static design asset** (hero, decorative background, logo bundled with the app) is rendered with `Image(painter = painterResource(...))`, never AsyncImage. The rule is `remote` → AsyncImage; `bundled` → painterResource.

**Import**: `import {CORE_DESIGNSYSTEM_PKG}.AsyncImage`

**Example**:
```kotlin
// ❌ Wrong import
import coil3.compose.AsyncImage

// ✅ Correct import
import {CORE_DESIGNSYSTEM_PKG}.AsyncImage

AsyncImage(
    url = uiModel.avatarUrl,                                   // runtime data, not a design asset URL
    loadingResId = DesignSystemResources.drawable.ds_image_placeholder,
    contentDescription = "Avatar",
)
```

---

## Selection Controls

| Material3 | X-Component | Use Case |
|-----------|------------------|----------|
| `RadioButton` | `XRadioButton` | Radio selection |
| `DropdownMenu` | `XDropDown` | Dropdown selection |
| N/A | `XSelectionButton` + `XSelectionButtonContainer` | Multi-selection buttons |

---

## Dialogs & Modals

| Material3 | X-Component | Use Case |
|-----------|------------------|----------|
| `AlertDialog` / `Dialog` | `XDialog` / `SimpleDialog` | Dialogs |
| N/A | `ItemPickerModal` | Item picker bottom sheet |

**Import**: `import {CORE_DESIGNSYSTEM_PKG}.{XDialog, SimpleDialog}`

---

## Icons

| Material3 | X-Component | Use Case |
|-----------|------------------|----------|
| `Icon` | `XIcon` | Icon display |

**Import**: `import {CORE_DESIGNSYSTEM_PKG}.XIcon`

---

## Theme

| Material3 | X-Component | Use Case |
|-----------|------------------|----------|
| `MaterialTheme` | `XTheme` | Theme wrapper (App level only) |

**⚠️ XTheme is applied ONCE at App.kt level. Never wrap individual screens.**

```kotlin
// ❌ WRONG - Don't wrap screens in XTheme
fun LoginScreen() {
    XTheme { XScreen(...) }
}

// ✅ CORRECT - Use XScreen directly (Rule 13; never XScaffold in a feature)
fun LoginScreen() {
    XScreen(...) { /* content */ }
}
```

---

## Other Components

| Component | Package | Use Case |
|-----------|---------|----------|
| `MoneyText` | `{CORE_DESIGNSYSTEM_PKG}` | Currency display with smaller symbol |
| `XWebView` | `{CORE_DESIGNSYSTEM_PKG}` | WebView wrapper |
| `Placeholder` | `{CORE_DESIGNSYSTEM_PKG}` | Placeholder skeleton |
| `SupportText` | `{CORE_DESIGNSYSTEM_PKG}` | Support/helper text |
| `TextFieldCounter` | `{CORE_DESIGNSYSTEM_PKG}` | Character counter for fields |

---

## Package Import Summary

**For all X-components** (replace with actual package):
```kotlin
import {CORE_DESIGNSYSTEM_PKG}.*
```

**Avoid in feature modules**:
```kotlin
import androidx.compose.material3.* // ❌
```

**Compose Foundation is OK**:
```kotlin
import androidx.compose.foundation.layout.* // ✅
import androidx.compose.foundation.* // ✅
```

---

Use this mapping table whenever working on UI code to ensure correct component usage.
