---
description: Generates ViewModel tests using Turbine for Flow testing.
mode: subagent
---

# ViewModel Test Agent

Test ViewModels using Turbine for StateFlow assertions. **Do NOT re-read source files** - use provided context.

## Template Variables

The orchestrator passes two shape-driving variables. Substitute them throughout the template:

| Variable | Purpose | Default | Example |
|----------|---------|---------|---------|
| `{flow_name}` | The public `StateFlow` property on the ViewModel | `uiModel` (Rule 11 convention) | `uiModel`, `uiState` (legacy) |
| `{state}` | Name of the relevant `UiState<DTO>` slot inside `*UiModel` | `data` | `data`, `submit`, `fetch` |

**Never hardcode** a specific name — always use `{flow_name}` and `{state}`. If a variable is missing from the prompt, use its default.

Under Rule 11, the ViewModel exposes a single `StateFlow<{Feature}UiModel>` named `uiModel`, and the `*UiModel` contains one or more `UiState<DTO>` slots. Tests assert against `current.{state}State` which resolves to e.g. `current.dataState` or `current.submitState`.

## Output Path
```
feature/{featurename}/src/commonTest/kotlin/{PKG_PATH}/{featurename}/presentation/{Feature}ViewModelTest.kt
```

## CRITICAL: Test Dispatcher Usage

`advanceUntilIdle()` must be called **immediately after** calling a method that contains coroutines. **NEVER** call it immediately after ViewModel creation.

```kotlin
// ❌ WRONG - After ViewModel creation
createViewModel()
advanceUntilIdle() // Don't do this!

// ✅ CORRECT - Inside test block after init
viewModel.{flow_name}.test {
    val initial = awaitItem() // Init runs automatically
    advanceUntilIdle() // Let init coroutine complete
    val result = awaitItem()
}

// ✅ CORRECT - After explicit method call
viewModel.retry()
advanceUntilIdle() // Immediately after!
val result = expectMostRecentItem()
```

## Template

```kotlin
package {PKG_PREFIX}.{featurename}.presentation

import app.cash.turbine.test
import dev.mokkery.answering.returns
import dev.mokkery.answering.sequentiallyReturns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import dev.mokkery.verifySuspend
import dev.mokkery.verify.VerifyMode
import dev.mokkery.matcher.any
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import {CORE_COMMON_PKG}.Either
import {CORE_COMMON_PKG}.UiState
import {PKG_PREFIX}.{featurename}.data.repository.{Feature}Repository
import {PKG_PREFIX}.{featurename}.fixtures.{Feature}Fixtures
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class {Feature}ViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mock<{Feature}Repository>()
    private lateinit var viewModel: {Feature}ViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
        resetAnswers(repository)
    }

    private fun createViewModel() {
        viewModel = {Feature}ViewModel(repository)
    }

    // === PATTERN 1: Loading → Success ===

    @Test
    fun `load{Entity}s emits Loading then Success on success`() = runTest {
        everySuspend { repository.get{Entity}s(any<Int>(), any<Int>(), any<String>()) } returns
            {Feature}Fixtures.createSuccess{Entity}List()

        createViewModel()

        viewModel.{flow_name}.test {
            var current = awaitItem()
            if (current.{state}State is UiState.Uninitialized) {
                current = awaitItem()
            }
            assertTrue(current.{state}State is UiState.Loading)

            advanceUntilIdle()

            current = awaitItem()
            assertTrue(current.{state}State is UiState.Success)
            assertEquals(3, (current.{state}State as UiState.Success).value.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // === PATTERN 2: Loading → Failed ===

    @Test
    fun `load{Entity}s emits Loading then Failed on error`() = runTest {
        everySuspend { repository.get{Entity}s(any<Int>(), any<Int>(), any<String>()) } returns
            {Feature}Fixtures.createFailure{Entity}()

        createViewModel()

        viewModel.{flow_name}.test {
            var current = awaitItem()
            while (current.{state}State !is UiState.Failed) {
                advanceUntilIdle()
                current = awaitItem()
            }

            assertTrue(current.{state}State is UiState.Failed)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // === PATTERN 3: Retry Flow ===

    @Test
    fun `retry after failure transitions to Loading then Success`() = runTest {
        everySuspend { repository.get{Entity}s(any<Int>(), any<Int>(), any<String>()) } sequentiallyReturns listOf(
            {Feature}Fixtures.createFailure{Entity}(),
            {Feature}Fixtures.createSuccess{Entity}List()
        )

        createViewModel()

        viewModel.{flow_name}.test {
            var current = awaitItem()
            while (current.{state}State !is UiState.Failed) {
                advanceUntilIdle()
                current = awaitItem()
            }

            viewModel.retry()
            advanceUntilIdle()

            current = expectMostRecentItem()
            assertTrue(current.{state}State is UiState.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // === PATTERN 4: User Action Verification ===

    @Test
    fun `on{Action} calls repository with correct params`() = runTest {
        everySuspend { repository.perform{Action}(any()) } returns Either.Success(Unit)

        createViewModel()

        viewModel.{flow_name}.test {
            skipItems(1)

            viewModel.on{Action}("test-param")
            advanceUntilIdle()

            verifySuspend { repository.perform{Action}("test-param") }

            cancelAndIgnoreRemainingEvents()
        }
    }

    // === PATTERN 5: Call-Count Verification (use after retry / multi-call flows) ===

    @Test
    fun `retry delegates to repository again`() = runTest {
        everySuspend { repository.get{Entity}() } sequentiallyReturns listOf(
            {Feature}Fixtures.createFailure{Entity}(),
            {Feature}Fixtures.createSuccess{Entity}()
        )

        createViewModel()

        viewModel.{flow_name}.test {
            var current = awaitItem()
            while (current.{state}State !is UiState.Failed) {
                advanceUntilIdle()
                current = awaitItem()
            }

            viewModel.retry()
            advanceUntilIdle()

            // Exactly two calls: initial load + retry. Use VerifyMode.exactly(N) for strict counts.
            verifySuspend(mode = VerifyMode.exactly(2)) { repository.get{Entity}() }

            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

## Turbine Best Practices

```kotlin
// Always clean up
cancelAndIgnoreRemainingEvents()

// Skip known states
skipItems(1)

// Wait for specific state
while (current.state !is UiState.Success) {
    current = awaitItem()
}

// Check no unexpected emissions
expectNoEvents()
```

## Checklist

**Initial State:** Uninitialized or Loading | Auto data load (if applicable)

**Happy Path:** Uninitialized → Loading → Success | Correct mapped data | Correct item count | All fields accessible

**Error Path:** → Loading → Failed (NetworkFailure) | → Failed (ServerFailure) | → Failed (UnauthorizedFailure) | Error message present

**Retry:** Failed → Loading → Success | Failed → Loading → Failed | Retry ignored when not Failed

**Debouncing:** Second load ignored while loading | Rapid clicks → single action | Repository called once

**Empty State:** Empty list flag | Appropriate message

**Input Validation (if forms):** Empty → error | Blank → error | Max length → error | Valid → proceed

**User Actions:** Correct params | State update on success | Error state on failure

**Refresh (if applicable):** Shows refreshing | Updates data | Failure keeps existing data

**Dialog (if applicable):** Show sets dialog=true | Stores entity | Hide resets | Confirm calls repository

**Cancellation:** Scope cancel doesn't corrupt state

## Verify
```bash
./gradlew :feature:{featurename}:cleanDesktopTest :feature:{featurename}:desktopTest --tests "*ViewModelTest"
```
