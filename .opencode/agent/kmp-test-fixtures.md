---
description: Generates test fixtures from a provided model path.
mode: subagent
---

# Test Fixtures Agent

Generate comprehensive domain + UI fixtures. **Do NOT re-read source files** - use provided context.

## Conditional Sections

The orchestrator passes capability flags. Render sections only when their flag is true:

| Flag | Section gated |
|------|---------------|
| `hasDto: true` | DTO factories (`create{Entity}Dto`, `create{Entity}DtoList`) |
| `hasPagination: true` | Response wrappers (`create{Feature}Response`, `createEmpty{Feature}Response`) and paginated JSON (`valid{Feature}ResponseJson`, `empty{Feature}ResponseJson`) |

When a flag is false, **omit the entire section** — do not emit stub code, do not import types that don't exist.

## Template Variables

| Variable | Purpose | Default |
|----------|---------|---------|
| `{state}` | Name of the relevant `UiState<DTO>` slot inside `*UiModel` (e.g. `data`, `submit`, `fetch`) — used as `{state}State` in the fixture factories | `data` |

Under Rule 11, `*UiModel` contains plain UI fields + one or more `UiState<DTO>` slots. Fixtures populate a single async slot via the `{state}State =` pattern (e.g. `dataState = UiState.Loading`). If multiple slots exist, the orchestrator picks the primary one for the base-state fixtures.

## Output Paths
```
feature/{featurename}/src/commonTest/kotlin/{PKG_PATH}/{featurename}/fixtures/{Feature}Fixtures.kt
feature/{featurename}/src/commonTest/kotlin/{PKG_PATH}/{featurename}/fixtures/{Feature}UiFixtures.kt
```

## Template: {Feature}Fixtures.kt

```kotlin
package {PKG_PREFIX}.{featurename}.fixtures

// Import entity + DTO types from this feature's actual model packages.
// In this project, models live under `{featurename}.data.model.*` (no `domain` layer).
import {PKG_PREFIX}.{featurename}.data.model.*
import {CORE_COMMON_PKG}.ErrorModel
import {CORE_COMMON_PKG}.Either
import {CORE_DATA_PKG}.ErrorConst

object {Feature}Fixtures {

    // === DOMAIN MODEL FACTORIES ===

    fun create{Entity}(
        id: String = "test-id",
        name: String = "Test Name",
        description: String? = "Test Description",
        // ... all other fields with defaults
    ) = {Entity}(id = id, name = name, description = description)

    fun create{Entity}List(count: Int = 3) = (1..count).map { i ->
        create{Entity}(id = "id-$i", name = "Entity $i")
    }

    fun createEmpty{Entity}List() = emptyList<{Entity}>()
    fun createSingle{Entity}List() = listOf(create{Entity}())
    fun createLarge{Entity}List(count: Int = 100) = create{Entity}List(count)

    // === EDGE CASES (ALL MANDATORY) ===

    fun create{Entity}WithNullOptionals() = create{Entity}(description = null)
    fun create{Entity}WithEmptyStrings() = create{Entity}(name = "", description = "")
    fun create{Entity}WithBlankStrings() = create{Entity}(name = "   ", description = "   ")
    fun create{Entity}WithSpecialCharacters() = create{Entity}(
        name = "Test's \"Entity\" with <special> & chars",
        description = "Line1\nLine2\tTabbed"
    )
    fun create{Entity}WithUnicode() = create{Entity}(
        name = "Test 日本語 émoji 🎉 中文",
        description = "Ümlauts and açcénts"
    )
    fun create{Entity}WithLongStrings() = create{Entity}(
        name = "a".repeat(500),
        description = "b".repeat(5000)
    )
    fun create{Entity}WithMaxValues() = create{Entity}(/* numeric fields = MAX_VALUE */)
    fun create{Entity}WithMinValues() = create{Entity}(/* numeric fields = MIN_VALUE or 0 */)

    // === DTO FACTORIES ===  (ONLY if hasDto == true)

    fun create{Entity}Dto(/* ALL DTO fields */) = {Entity}Dto(/* ... */)
    fun create{Entity}DtoList(count: Int = 3) = (1..count).map { i ->
        create{Entity}Dto(id = "id-$i")
    }

    // === RESPONSE WRAPPERS ===  (ONLY if hasPagination == true)

    fun create{Feature}Response(
        results: List<{Entity}Dto> = create{Entity}DtoList(),
        count: Int = results.size,
        next: String? = null,
        previous: String? = null
    ) = {Feature}Response(results = results, count = count, next = next, previous = previous)

    fun createEmpty{Feature}Response() = create{Feature}Response(results = emptyList(), count = 0)

    // === ERROR HELPERS (Use ErrorConst) ===

    val networkError = ErrorConst.NoNetwork
    val unauthorizedError = ErrorConst.Unauthorized
    val notFoundError = ErrorModel.MessageCode("{Resource} not found", 404)
    val badRequestError = ErrorModel.MessageCode("Invalid request parameters", 4001)
    val timeoutError = ErrorConst.ServerUnknownError(408)
    val serverError = ErrorConst.ServerUnknownError(500)
    val serviceUnavailableError = ErrorConst.ServerUnknownError(503)
    val serializationError = ErrorConst.SerializationError

    // === EITHER HELPERS ===

    fun createSuccess{Entity}(entity: {Entity} = create{Entity}()) = Either.Success(entity)
    fun createSuccess{Entity}List(entities: List<{Entity}> = create{Entity}List()) = Either.Success(entities)
    fun createFailure{Entity}(error: ErrorModel = networkError) = Either.Failure(error)

    // === JSON RESPONSES (for MockEngine) ===
    // Format: {"detail": "...", "code": ...} matches NetworkErrorModel

    val valid{Entity}Json = """{"id": "test-id", "name": "Test Name"}"""
    val valid{Entity}ListJson = """[{"id": "id-1", "name": "Entity 1"}, {"id": "id-2", "name": "Entity 2"}, {"id": "id-3", "name": "Entity 3"}]"""
    val empty{Entity}ListJson = "[]"
    // Paginated JSON (ONLY if hasPagination == true):
    val valid{Feature}ResponseJson = """{"count": 3, "next": null, "previous": null, "results": $valid{Entity}ListJson}"""
    val empty{Feature}ResponseJson = """{"count": 0, "next": null, "previous": null, "results": []}"""

    // Error JSONs (NetworkErrorModel format)
    val error400Json = """{"detail": "Invalid request parameters", "code": 4001}"""
    val error401Json = """{"detail": "Unauthorized", "code": null}"""
    val error403Json = """{"detail": "Access denied", "code": 403}"""
    val error404Json = """{"detail": "{Resource} not found", "code": 404}"""
    val error500Json = """{"detail": "Internal Server Error", "code": 5001}"""
    val error503Json = """{"detail": null, "code": null}"""

    // Edge case JSONs
    val malformedJson = "{ invalid json"
    val incompleteJson = """{"id": "test-id"}"""
    val nullFieldsJson = """{"id": "test-id", "name": "Test", "description": null}"""
    val extraFieldsJson = """{"id": "test-id", "name": "Test", "unknownField": "ignored"}"""
    val emptyStringFieldsJson = """{"id": "", "name": ""}"""
    val specialCharsJson = """{"id": "test-id", "name": "Test's \"Name\" & <more>"}"""
    val unicodeJson = """{"id": "test-id", "name": "日本語 🎉"}"""
}
```

## Template: {Feature}UiFixtures.kt

```kotlin
package {PKG_PREFIX}.{featurename}.fixtures

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import {CORE_COMMON_PKG}.ErrorModel
import {CORE_COMMON_PKG}.UiState
import {PKG_PREFIX}.{featurename}.presentation.{Feature}UiModel

object {Feature}UiFixtures {

    // === 4 MANDATORY BASE STATES ===

    fun createUninitializedState() = {Feature}UiModel({state}State = UiState.Uninitialized)
    fun createLoadingState() = {Feature}UiModel({state}State = UiState.Loading)
    fun createSuccessState(entities: List<{Entity}> = {Feature}Fixtures.create{Entity}List()) =
        {Feature}UiModel({state}State = UiState.Success(entities.toImmutableList()))
    fun createEmptyState() = {Feature}UiModel({state}State = UiState.Success(persistentListOf()))
    fun createErrorState(message: String = "Something went wrong") =
        {Feature}UiModel({state}State = UiState.Failed(ErrorModel.Message(message)))

    // === SUCCESS VARIATIONS ===

    fun createSingleItemState() = createSuccessState({Feature}Fixtures.createSingle{Entity}List())
    fun createLargeListState(count: Int = 50) = createSuccessState({Feature}Fixtures.create{Entity}List(count))

    // === ERROR VARIATIONS (use ErrorConst from Fixtures) ===

    fun createNetworkErrorState() = {Feature}UiModel(
        {state}State = UiState.Failed({Feature}Fixtures.networkError)
    )
    fun createUnauthorizedErrorState() = {Feature}UiModel(
        {state}State = UiState.Failed({Feature}Fixtures.unauthorizedError)
    )
    fun createNotFoundErrorState() = {Feature}UiModel(
        {state}State = UiState.Failed({Feature}Fixtures.notFoundError)
    )
    fun createServerErrorState() = {Feature}UiModel(
        {state}State = UiState.Failed({Feature}Fixtures.serverError)
    )

    // === INPUT STATES (if applicable) ===

    fun createWithValidInput(inputValue: String = "valid input") = {Feature}UiModel(
        {state}State = UiState.Uninitialized,
        // inputField = inputValue
    )
    fun createWithEmptyInput() = {Feature}UiModel(
        {state}State = UiState.Uninitialized,
        // inputField = ""
    )

    // === DIALOG STATES (if applicable) ===

    fun createShowingConfirmDialog(entity: {Entity} = {Feature}Fixtures.create{Entity}()) = {Feature}UiModel(
        {state}State = UiState.Success({Feature}Fixtures.create{Entity}List().toImmutableList()),
        showConfirmDialog = true,
        selectedEntity = entity
    )
    fun createDialogActionInProgress(entity: {Entity} = {Feature}Fixtures.create{Entity}()) = {Feature}UiModel(
        {state}State = UiState.Success({Feature}Fixtures.create{Entity}List().toImmutableList()),
        showConfirmDialog = true,
        selectedEntity = entity,
        isActionInProgress = true
    )

    // === SELECTION/FILTER STATES (if applicable) ===

    fun createWithSelectedItem(selectedId: String = "id-1") = {Feature}UiModel(
        {state}State = UiState.Success({Feature}Fixtures.create{Entity}List().toImmutableList()),
        selectedItemId = selectedId
    )
    fun createWithSearchQuery(query: String = "search term") = {Feature}UiModel(
        {state}State = UiState.Success({Feature}Fixtures.create{Entity}List().toImmutableList()),
        searchQuery = query
    )
}
```

## Checklist

**{Feature}Fixtures.kt:**
- `create{Entity}()` with ALL fields | `create{Entity}List/Empty/Single/Large`
- Edge cases: `WithNullOptionals`, `WithEmptyStrings`, `WithBlankStrings`, `WithSpecialCharacters`, `WithUnicode`, `WithLongStrings`, `WithMaxValues`, `WithMinValues`
- DTO factories — **only if `hasDto == true`**
- Response wrappers & paginated JSON — **only if `hasPagination == true`**
- Error helpers using ErrorConst (8 types)
- Either helpers: `createSuccess{Entity}()`, `createFailure{Entity}()`
- JSON: valid, empty, error (400-503), malformed, incomplete, nullFields, extraFields, specialChars, unicode

**{Feature}UiFixtures.kt:**
- 4 base states: Uninitialized, Loading, Success, Empty, Error
- Success variations: Single, Large
- Error variations: Network, Unauthorized, NotFound, Server
- Dialog/Input/Selection states (if applicable)

## Verify
```bash
./gradlew :feature:{featurename}:compileTestKotlinDesktop
```
