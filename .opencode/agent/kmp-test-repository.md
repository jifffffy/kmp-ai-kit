---
description: Generates Repository tests using Mokkery.
mode: subagent
---

# Repository Test Agent

Test Repository implementations using Mokkery. **Do NOT re-read source files** - use provided context.

## Either Unwrapping

`Either.Success` exposes its payload as `.data` (not `.value`). After `assertTrue(result is Either.Success)`, access the payload as `result.data`. `UiState.Success` is the one that uses `.value` — do not confuse the two.

```kotlin
assertTrue(result is Either.Success)
assertEquals(expected, result.data)            // ✓ correct
// assertEquals(expected, result.value)        // ✗ won't compile
```

## Method-Signature Variants

The repository under test may take parameters (`get{Entity}s(offset, limit, ordering)`) or no parameters (`get{Entity}()`). Match the actual signature — do not blindly emit `any(), any(), any()` matchers when the method takes none.

| Repository shape | Mokkery stub | Call |
|------------------|--------------|------|
| `getX()` (no params) | `everySuspend { dataSource.getX() } returns ...` | `repository.getX()` |
| `getX(a, b, c)` (paginated) | `everySuspend { dataSource.getX(any(), any(), any()) } returns ...` | `repository.getX(offset = 10, limit = 20, ordering = "...")` |
| `getX(id)` (single by id) | `everySuspend { dataSource.getX(any()) } returns ...` | `repository.getX("test-id")` |

Use the no-args form when the orchestrator's `hasPagination` is `false` **and** the repository interface declares no parameters. Substitute the real parameter names from the YAML context.

## Output Path
```
feature/{featurename}/src/commonTest/kotlin/{PKG_PATH}/{featurename}/data/{Feature}RepositoryImplTest.kt
```

## Template

```kotlin
package {PKG_PREFIX}.{featurename}.data

import dev.mokkery.answering.returns
import dev.mokkery.answering.sequentiallyReturns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import {CORE_COMMON_PKG}.Either
import {CORE_COMMON_PKG}.ErrorModel
import {PKG_PREFIX}.{featurename}.data.datasource.{Feature}RemoteDataSource
import {PKG_PREFIX}.{featurename}.fixtures.{Feature}Fixtures
import kotlin.test.*

class {Feature}RepositoryImplTest {

    private val remoteDataSource = mock<{Feature}RemoteDataSource>()
    private lateinit var repository: {Feature}RepositoryImpl

    @BeforeTest
    fun setup() {
        repository = {Feature}RepositoryImpl(remoteDataSource = remoteDataSource)
    }

    @AfterTest
    fun teardown() {
        resetAnswers(remoteDataSource)
    }

    // === SUCCESS CASES ===
    // NOTE: `.data` is the correct accessor on Either.Success (NOT `.value`).
    // Substitute method signature based on the actual repository — drop `any()` matchers
    // entirely when the method takes no parameters.
    // Repository returns Either<DTO> with no mapping (Rule 11) — asserts equal the DTO produced by fixtures.

    @Test
    fun `get{Entity} returns DTO on success`() = runTest {
        val entity = {Feature}Fixtures.create{Entity}()
        everySuspend { remoteDataSource.get{Entity}(/* any() per actual param */) } returns
            Either.Success(entity)

        val result = repository.get{Entity}(/* args if any */)

        assertTrue(result is Either.Success)
        assertEquals(entity, result.data)
    }

    @Test
    fun `get{Entity} returns empty list when response is empty`() = runTest {
        // Only applies when repository returns Either<List<T>> — skip otherwise.
        everySuspend { remoteDataSource.get{Entity}(/* any() per actual param */) } returns
            Either.Success({Feature}Fixtures.createEmpty{Entity}List())

        val result = repository.get{Entity}()

        assertTrue(result is Either.Success)
        assertTrue(result.data.isEmpty())
    }

    // === ERROR PROPAGATION ===

    @Test
    fun `get{Entity} propagates network failure`() = runTest {
        everySuspend { remoteDataSource.get{Entity}(/* any() per actual param */) } returns
            Either.Failure({Feature}Fixtures.networkError)

        val result = repository.get{Entity}()

        assertTrue(result is Either.Failure)
        assertEquals({Feature}Fixtures.networkError, result.error)
    }

    @Test
    fun `get{Entity} propagates unauthorized error`() = runTest {
        everySuspend { remoteDataSource.get{Entity}(/* any() per actual param */) } returns
            Either.Failure({Feature}Fixtures.unauthorizedError)

        val result = repository.get{Entity}()

        assertTrue(result is Either.Failure)
        assertEquals({Feature}Fixtures.unauthorizedError, result.error)
    }

    @Test
    fun `get{Entity} propagates server error`() = runTest {
        everySuspend { remoteDataSource.get{Entity}(/* any() per actual param */) } returns
            Either.Failure({Feature}Fixtures.serverError)

        val result = repository.get{Entity}()

        assertTrue(result is Either.Failure)
        assertEquals({Feature}Fixtures.serverError, result.error)
    }

    @Test
    fun `get{Entity} propagates not found error`() = runTest {
        everySuspend { remoteDataSource.get{Entity}(/* any() per actual param */) } returns
            Either.Failure({Feature}Fixtures.notFoundError)

        val result = repository.get{Entity}()

        assertTrue(result is Either.Failure)
        assertEquals({Feature}Fixtures.notFoundError, result.error)
    }

    // === PARAMETER VERIFICATION ===
    // Emit only when the repository method takes parameters worth verifying.

    @Test
    fun `get{Entity} passes correct parameters to dataSource`() = runTest {
        everySuspend { remoteDataSource.get{Entity}(any(), any(), any()) } returns
            Either.Success({Feature}Fixtures.create{Entity}())

        repository.get{Entity}(offset = 10, limit = 20, ordering = "-created_time")

        verifySuspend { remoteDataSource.get{Entity}(10, 20, "-created_time") }
    }

    // === DELEGATION VERIFICATION ===
    // Recommended when the method takes no params — replaces parameter verification.

    @Test
    fun `get{Entity} calls remote data source exactly once`() = runTest {
        everySuspend { remoteDataSource.get{Entity}() } returns
            Either.Success({Feature}Fixtures.create{Entity}())

        repository.get{Entity}()

        verifySuspend(dev.mokkery.verify.VerifyMode.exactly(1)) { remoteDataSource.get{Entity}() }
    }
}
```

## Checklist

**Success:** Mapped entity | Mapped list | Empty list | All fields mapped | Single item | Large list (100)

**Error Propagation (use ErrorConst):** NoNetwork | Unauthorized | ServerUnknownError | MessageCode (404, 400) | SerializationError

**Data Transformation:** Timestamps → Long | Nested objects | Null optionals | Filters/sorts

**Parameter Verification:** Correct ID | Offset/limit | Ordering | Filters

**Boundary:** Max page (100) | Min page (1) | Zero offset | Special chars in ID | Long ID

**Caching (if applicable):** Cache hit | Cache miss → network | Update cache | Cache failure

## Verify
```bash
./gradlew :feature:{featurename}:cleanDesktopTest :feature:{featurename}:desktopTest --tests "*RepositoryImplTest"
```
