# Usage Examples: X-Design System

**Note**: Replace `{PKG_PREFIX}` and `{CORE_DESIGNSYSTEM_PKG}` with your project's package prefix and design system package.

**Strings (Rule 12)**: examples below use string literals (`XText("Login")`) for brevity, to keep the focus on component shape. **Real feature code never hardcodes display text** — use `XText(stringResource(Res.string.login_button))`, shared strings via `DesignSystemResources`, and `UiText` for ViewModel-origin messages. See `shared/kmp-patterns.md` → "Strings & Localization (Rule 12)".

## Pattern 1: Screen Structure (ScreenRoot Pattern)

**Every feature screen has TWO composables:**

1. **`FeatureScreen`** - ViewModel wrapper (thin layer)
2. **`FeatureScreenRoot`** - ViewModel-independent, testable UI

```kotlin
package {PKG_PREFIX}.featurename.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import {CORE_DESIGNSYSTEM_PKG}.*
import {CORE_DESIGNSYSTEM_PKG}.toolbar.XTopAppBar

// 1. Screen: ViewModel wrapper (collects state, delegates to ScreenRoot)
@Composable
fun FeatureScreen(
    onNavigateBack: () -> Unit,
    viewModel: FeatureViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FeatureScreenRoot(
        uiState = uiState,
        onBackClick = onNavigateBack,
        onRetry = viewModel::loadData,
    )
}

// 2. ScreenRoot: ViewModel-independent (testable, takes UiState + callbacks)
@Composable
fun FeatureScreenRoot(
    uiState: FeatureUiState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Rule 13 — feature screens use XScreen, never XScaffold/Scaffold.
    // The single Scaffold (App.kt) owns all insets; XScreen adds none.
    XScreen(
        topBar = {
            XTopAppBar(
                title = { XText("Feature Title") },
                navigationIcon = { XIconButton(onClick = onBackClick) { /* icon */ } }
            )
        },
        modifier = modifier.fillMaxSize(),
    ) {
        // content fills XScreen's weight(1f) box — no paddingValues to thread
        when (val state = uiState.dataState) {
            UiState.Uninitialized -> { /* Empty */ }
            UiState.Loading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    XCircularProgressIndicator()
                }
            }
            is UiState.Success -> { SuccessContent(state.value) }
            is UiState.Failed -> { ErrorState(state.error, onRetry) }
        }
    }
}
```

**Key points**:
- ✅ **Two composables**: Screen (wrapper) + ScreenRoot (testable)
- ✅ ScreenRoot takes `UiState` + callbacks - **no ViewModel**
- ✅ UI tests target `ScreenRoot` directly with test fixtures
- ❌ **DO NOT** use `XTheme` in screens - it's already applied at App level
- ✅ `XScreen` for structure (Rule 13 — never `XScaffold`/`Scaffold` in a feature)
- ✅ `XTopAppBar` from `{CORE_DESIGNSYSTEM_PKG}.toolbar`, passed to `XScreen`'s `topBar` slot
- ✅ 4-state pattern with `when`

---

## Pattern 2: Form with Buttons

```kotlin
@Composable
fun FormContent(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isLoading: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        XText(
            text = "Login",
            style = XTextDefaults.titleStyle()
        )

        XTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = { XText("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        XTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = { XText("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        XButton(
            onClick = onSubmit,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                XButtonProgress()
            } else {
                XText("Submit")
            }
        }

        XTextButton(
            onClick = { /* navigate to signup */ }
        ) {
            XText("Don't have an account? Sign up")
        }
    }
}
```

**Key points**:
- ✅ `XTextField` for inputs
- ✅ `XButton` for primary action
- ✅ `XTextButton` for secondary action
- ✅ `XButtonProgress` for loading button state
- ✅ All text uses `XText`

---

## Pattern 3: Card List

```kotlin
@Composable
fun ProductList(
    products: List<Product>,
    onProductClick: (String) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(products) { product ->
            ProductCard(
                product = product,
                onClick = { onProductClick(product.id) }
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
) {
    XCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                url = product.imageUrl,
                loadingResId = DesignSystemResources.drawable.ds_image_placeholder,
                contentDescription = product.name,
                modifier = Modifier.size(80.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                XText(
                    text = product.name,
                    style = XTextDefaults.titleStyle()
                )

                XText(
                    text = product.description,
                    style = XTextDefaults.bodyStyle(),
                    maxLines = 2
                )

                MoneyText(
                    amount = product.price.amount,
                    currency = product.price.currency
                )
            }
        }
    }
}
```

**Key points**:
- ✅ `XCard` for list items
- ✅ `AsyncImage` from `{CORE_DESIGNSYSTEM_PKG}` (not coil3)
- ✅ `MoneyText` for currency display
- ✅ Text styles from `XTextDefaults`

---

## Pattern 4: Error Handling

```kotlin
@Composable
fun ErrorState(
    error: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        XIcon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        XText(
            text = error,
            style = XTextDefaults.errorStyle(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        XButton(onClick = onRetry) {
            XText("Retry")
        }
    }
}
```

**Key points**:
- ✅ `XIcon` for error icon
- ✅ `XText` with `errorStyle()`
- ✅ `XButton` for retry action

---

## Pattern 5: Dialog

```kotlin
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    XDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            XText(
                text = title,
                style = XTextDefaults.titleStyle()
            )

            XText(
                text = message,
                style = XTextDefaults.bodyStyle()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                XTextButton(onClick = onDismiss) {
                    XText("Cancel")
                }

                XButton(onClick = onConfirm) {
                    XText("Confirm")
                }
            }
        }
    }
}
```

**Key points**:
- ✅ `XDialog` wrapper
- ✅ Button variants for actions
- ✅ Consistent spacing and padding

---

## Pattern 6: Search Field

```kotlin
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    SearchField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { XText("Search products...") },
        modifier = Modifier.fillMaxWidth()
    )
}
```

**Key points**:
- ✅ Use `SearchField` for search (wraps XTextField with icons)

---

## Pattern 7: Pull to Refresh

```kotlin
@Composable
fun RefreshableContent(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit,
) {
    XPullRefresh(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        content()
    }
}
```

---

## Import Template

**Standard imports for feature UI files:**

```kotlin
package {PKG_PREFIX}.featurename.ui

// Compose Foundation (allowed)
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Design System (use this - replace with actual package)
import {CORE_DESIGNSYSTEM_PKG}.*
import {CORE_DESIGNSYSTEM_PKG}.toolbar.XTopAppBar  // Separate import for toolbar

// Material3 (ONLY for theme values, never components)
import androidx.compose.material3.MaterialTheme

// Koin
import org.koin.compose.viewmodel.koinViewModel
```

---

## Important Notes

### XTheme Usage
- ❌ **NEVER** wrap individual screens with `XTheme`
- ✅ `XTheme` is already applied once at the App level (`App.kt`)
- Multiple `XTheme` wrappers can cause theme override issues

### XTopAppBar Import
- ✅ Import from `{CORE_DESIGNSYSTEM_PKG}.toolbar.XTopAppBar` (not `{CORE_DESIGNSYSTEM_PKG}.XTopAppBar`)

### AsyncImage Parameter
- ✅ Use `url` parameter: `AsyncImage(url = imageUrl, ...)`
- ❌ Do not use `model` parameter

---

Use these patterns as templates when creating or modifying UI code in feature modules.
