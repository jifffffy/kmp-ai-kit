---
description: Generates Compose UI tests.
mode: subagent
---

# UI Test Agent

Test Compose screens using runComposeUiTest. **Do NOT re-read source files** - use provided context.

## Key Concept: ScreenRoot Pattern

- `{Feature}Screen` - ViewModel-based wrapper that collects `viewModel.uiModel`
- `{Feature}ScreenRoot` - ViewModel-independent composable that takes `uiModel: {Feature}UiModel` + callbacks (Rule 11)

**Tests ALWAYS target `{Feature}ScreenRoot`** - allows testing without ViewModel mocking.

**Secondary screens:** a `kind: screen` secondary (e.g. edit-profile) has its **own** `{Feature}{Role}ScreenRoot` — give it its own ScreenRoot test file, same pattern. A `kind: surface` secondary (bottom sheet/dialog) has no separate ScreenRoot; test its visible/hidden states by toggling the `{Feature}UiModel` visibility field passed to `{Feature}ScreenRoot`.

## Output Path
```
feature/{featurename}/src/commonTest/kotlin/{PKG_PATH}/{featurename}/presentation/ui/{Feature}ScreenTest.kt
```

## Template

```kotlin
package {PKG_PREFIX}.{featurename}.presentation.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import {CORE_DESIGNSYSTEM_PKG}.XTheme
import {PKG_PREFIX}.{featurename}.fixtures.{Feature}Fixtures
import {PKG_PREFIX}.{featurename}.fixtures.{Feature}UiFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// IMPORTANT: Use `androidx.compose.ui.test.v2.runComposeUiTest` (the v2 variant).
// Do NOT use the wildcard `androidx.compose.ui.test.*` — import each function explicitly.

@OptIn(ExperimentalTestApi::class)
class {Feature}ScreenTest {

    // === LOADING STATE ===

    @Test
    fun `shows loading indicator when state is Loading`() = runComposeUiTest {
        setContent {
            XTheme {
                {Feature}ScreenRoot(
                    uiModel = {Feature}UiFixtures.createLoadingState(),
                    onBackClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Loading").assertExists()
    }

    // === SUCCESS STATE ===

    @Test
    fun `shows content when state is Success`() = runComposeUiTest {
        val entities = {Feature}Fixtures.create{Entity}List(3)

        setContent {
            XTheme {
                {Feature}ScreenRoot(
                    uiModel = {Feature}UiFixtures.createSuccessState(entities),
                    onBackClick = {},
                    onRetry = {},
                )
            }
        }

        onAllNodesWithText(entities[0].name).onFirst().assertIsDisplayed()
    }

    // === EMPTY STATE ===

    @Test
    fun `shows empty message when list is empty`() = runComposeUiTest {
        setContent {
            XTheme {
                {Feature}ScreenRoot(
                    uiModel = {Feature}UiFixtures.createEmptyState(),
                    onBackClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("No {entity}s found").assertIsDisplayed()
    }

    // === ERROR STATES ===

    @Test
    fun `shows network error message and retry button`() = runComposeUiTest {
        setContent {
            XTheme {
                {Feature}ScreenRoot(
                    uiModel = {Feature}UiFixtures.createNetworkErrorState(),
                    onBackClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Error: Error, Check your connection and try again.", substring = true).assertIsDisplayed()
        onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun `shows unauthorized error message`() = runComposeUiTest {
        setContent {
            XTheme {
                {Feature}ScreenRoot(
                    uiModel = {Feature}UiFixtures.createUnauthorizedErrorState(),
                    onBackClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Error: You must login (#1001)", substring = true).assertIsDisplayed()
        onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun `shows not found error message`() = runComposeUiTest {
        setContent {
            XTheme {
                {Feature}ScreenRoot(
                    uiModel = {Feature}UiFixtures.createNotFoundErrorState(),
                    onBackClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Error: {Resource} not found (#404)", substring = true).assertIsDisplayed()
        onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun `shows server error message`() = runComposeUiTest {
        setContent {
            XTheme {
                {Feature}ScreenRoot(
                    uiModel = {Feature}UiFixtures.createServerErrorState(),
                    onBackClick = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithText("Error: An unknown network error has occurred! (#500)", substring = true).assertIsDisplayed()
        onNodeWithText("Retry").assertIsDisplayed()
    }

    // === USER INTERACTIONS ===

    @Test
    fun `retry button invokes onRetry callback`() = runComposeUiTest {
        var retryCalled = false

        setContent {
            XTheme {
                {Feature}ScreenRoot(
                    uiModel = {Feature}UiFixtures.createNetworkErrorState(),
                    onBackClick = {},
                    onRetry = { retryCalled = true },
                )
            }
        }

        onNodeWithText("Retry").performClick()
        assertTrue(retryCalled)
    }

    @Test
    fun `back button invokes onBackClick callback`() = runComposeUiTest {
        var backCalled = false

        setContent {
            XTheme {
                {Feature}ScreenRoot(
                    uiModel = {Feature}UiFixtures.createLoadingState(),
                    onBackClick = { backCalled = true },
                    onRetry = {},
                )
            }
        }

        onNodeWithContentDescription("Back").performClick()
        assertTrue(backCalled)
    }

    @Test
    fun `item click invokes callback with correct item`() = runComposeUiTest {
        val entities = {Feature}Fixtures.create{Entity}List(3)
        var clickedId: String? = null

        setContent {
            XTheme {
                {Feature}ScreenRoot(
                    uiModel = {Feature}UiFixtures.createSuccessState(entities),
                    onBackClick = {},
                    onRetry = {},
                    onItemClick = { id -> clickedId = id },
                )
            }
        }

        onNodeWithText(entities[0].name).performClick()
        assertEquals(entities[0].id, clickedId)
    }
}
```

## Scrolling to Off-Screen Content

If the screen renders content inside a `LazyColumn`/`LazyRow` and the assertion targets a node below the fold, scroll to it before asserting. Use `hasScrollToIndexAction()` to find the lazy container and `performScrollToIndex(N)` to bring item `N` into the viewport:

```kotlin
// Asserts on an item that is NOT visible in the initial viewport
onNode(hasScrollToIndexAction()).performScrollToIndex(8)
onNodeWithText("Section Header").assertIsDisplayed()
```

Add the corresponding import: `import androidx.compose.ui.test.performScrollToIndex` and `import androidx.compose.ui.test.hasScrollToIndexAction`.

## Callback Naming Conventions

- `onBackClick` - navigation back
- `onRetry` / `onRetryLoad{Entity}s` - retry failed operation
- `on{Entity}Click` / `onItemClick` - item selection
- `on{Field}Change` - input changes (e.g., `onPinChange`)
- `onPerform{Action}` - primary action (e.g., `onPerformLogin`)
- `onShow{Dialog}` / `onDismiss{Dialog}` - dialog visibility
- `onConfirm{Action}` - confirm dialog action

## Strings & Localization (Rule 12)

Feature UI text comes from `composeResources/values/strings.xml`, not literals. **Assert against the resource value, not a hardcoded English copy** — otherwise tests break when copy is edited or a non-default locale is active.

```kotlin
import {PROJECT_NAMESPACE}.feature.{featurename}.generated.resources.Res
import {PROJECT_NAMESPACE}.feature.{featurename}.generated.resources.retry_label
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.runBlocking

@Test
fun failedState_showsRetry() = runComposeUiTest {
    val retry = runBlocking { getString(Res.string.retry_label) }   // resolve the resource, don't hardcode
    setContent { FeatureScreenRoot(uiModel = failedModel, onRetry = {}) }
    onNodeWithText(retry).assertIsDisplayed()
}
```

Resolve each asserted label once via `getString(Res.string.*)` and reuse the value. Content descriptions follow the same rule (`getString(Res.string.cd_back)`). Repository-supplied data (entity names, dates) is asserted directly from the test fixture, as before.

## Checklist

**State Rendering:** Uninitialized → initial UI | Loading → indicator | Loading → disable interactive | Success → content | Success → hide loading | Success → all list items | Empty → placeholder | Failed → error message | Failed → retry button

**Strings (Rule 12):** asserted UI labels/content-descriptions resolved via `getString(Res.string.*)`, never hardcoded English

**Callbacks:** Back button → onBackClick | Retry → onRetry | Item click → onItemClick with entity | Primary action → correct callback | Delete → onDelete | Form submit → onSubmit

**Text Input (if applicable):** Accepts input | onChange with new value | Submit disabled when empty/invalid | Submit enabled when valid

**Dialogs (if applicable):** Appears when showDialog=true | Shows correct entity data | Confirm → onConfirm | Dismiss → onDismiss | Loading indicator when isActionInProgress

**Accessibility:** Loading has content description | Buttons have labels | Back button has "Back" description

## Verify
```bash
./gradlew :feature:{featurename}:cleanDesktopTest :feature:{featurename}:desktopTest --tests "*ScreenTest"
```
