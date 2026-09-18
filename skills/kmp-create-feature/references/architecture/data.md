# Data Layer Architecture Principles

Principles for implementing the **remote** data layer in KMP features following Clean Architecture
(Ktor/`ApiClient`, `Either<T>`). For **on-device persistence** (theme, locale, token, cached data
via DataStore/Room) see [local-data.md](local-data.md) — local datasources/repositories do **not**
use `Either` and are core infra in `:core:data`, not per-feature.

**Note**: Uses `{PKG_PREFIX}` placeholder for package prefix (resolved via Context Discovery).

## Data Layer Structure

```
{PKG_PREFIX}.{featurename}/data/
├── model/              # DTOs: Request/Response models (@Serializable)
├── remote/             # Ktor Resources (type-safe API endpoint definitions)
├── datasource/         # Interface + Impl (handles API communication)
└── repository/         # Interface + Impl (coordinates data sources)
```

## Critical Rules (Data Layer)

1. **Interface + Impl Pairs**: Every DataSource and Repository MUST have both interface and implementation
2. **Either<T> Returns**: All operations that can fail return `Either<T>`, never throw exceptions
3. **Lowercase Packages**: `{PKG_PREFIX}.featurename.data.model` (never camelCase or hyphens)
4. **No Presentation Imports (Rule 11)**: Files under `data/` MUST NOT import from `{PKG_PREFIX}.{featurename}.presentation.*`. Repository returns `Either<DTO>` — the raw DTO from `data/model/`. The data layer does not know UI types exist.

## Shared remote data → `data.app` tier (DRY across features)

Remote data layers are **per-feature by default**. But when **two or more features hit the same
endpoint or parse the same wire model**, that endpoint is **cross-feature shared app data** and
belongs **once** in the `:core:data` app tier (`{PKG_PREFIX}.data.app`) — not copied into each
feature. Same convention as the local-persistence app tier ([local-data.md](local-data.md) →
"Generic vs app tier") and the `:core:designsystem` generic-vs-`app` split.

**Symptom it's needed**: identical `@Resource` classes, identical wire DTOs, and identical
`{Feature}RemoteDataSource` fetch methods duplicated across features — and silently **drifting**
(each copy adds/drops fields). That duplication is the smell.

**Where it goes** (`:core:data`):
```
data/app/
├── model/                 # canonical wire DTOs — the field UNION of all consumers (extras nullable)
├── remote/resources/      # the shared @Resource endpoints (declared once)
├── remote/datasource/     # {Shared}RemoteDataSource (+Impl) → Either<List<DTO>>, injects generic ApiClient
└── di/AppDataModule.kt    # binds the shared datasource (the data.app strip seam)
```

**Each feature then**: deletes its own Resource + wire model + `{Feature}RemoteDataSource`; its
`RepositoryImpl` **injects the shared `{Shared}RemoteDataSource`** and keeps only its own
**wire-DTO → presentation-DTO mapping** (that mapping stays per-feature — it's where features
legitimately differ). The feature's own `*Dto` presentation models + repository logic are untouched.

**Rules carried over**: generic `data.*` never imports `data.app` (boundary); the canonical DTO is
the **superset** of every consumer's fields (reconcile drift — never blind-copy one feature's); the
shared datasource injects the **generic** `ApiClient` (app→generic, allowed); it's bound in
`AppDataModule` (the one sanctioned generic→app reference is the `includes` in `DataModules.kt`).

**Stays per-feature**: an endpoint only **one** feature uses. Don't hoist speculatively — hoist when
the second consumer appears.

## Layer Responsibilities

### 1. Models (data/model/)

**Purpose**: Define API contracts and data transfer objects

**Pattern**:
- Create `@Serializable` data classes for API requests/responses
- Use Kotlin's built-in types (String, Int, Boolean, List, etc.)
- Optional fields use nullable types with defaults (e.g., `val avatar: String? = null`)
- Naming: `{Feature}Response`, `{Feature}Request`, `{Entity}`, etc.

**Key Points**:
- Models stay in feature module **unless shared across ≥2 features** — then they're canonical wire DTOs in the `data.app` tier (see "Shared remote data → `data.app` tier" above), not duplicated per feature
- Match server API structure exactly (use `@SerialName` if needed for field mapping)
- Keep models simple data containers (no business logic)

### 2. Ktor Resources (data/remote/)

**Purpose**: Define type-safe API endpoints using Ktor Resources

**Pattern**:
- Create parent resource class with `@Resource` and `@Serializable`
- Nested classes for each endpoint operation (GET, POST, PUT, DELETE)
- Parent resource defines base path (e.g., `/api/v1/products`)
- Each nested class represents an endpoint with optional path parameters

**Structure**:
```kotlin
@Resource("/api/v1/{basePath}")
@Serializable
class {Feature}Resources {
    @Resource("{specificPath}")
    @Serializable
    class GetOperation(val parent: {Feature}Resources = {Feature}Resources())

    @Resource("{specificPath}")
    @Serializable
    class PostOperation(val parent: {Feature}Resources = {Feature}Resources())
}
```

**Key Points**:
- File location: `{PKG_PREFIX}.{featurename}.data.remote.{Feature}Resources.kt`
- Use nested classes for all operations under same base path
- Path parameters go in class constructor (e.g., `GetProduct(val productId: Int, ...)`)
- Query parameters also go in constructor (e.g., `val page: Int? = null`)

### 3. Remote DataSource (data/datasource/)

> For **local** datasources (DataStore/Room persistence), see [local-data.md](local-data.md). They
> return value-or-null/`Flow`, never `Either`, and live in `:core:data` as core infra.

**Purpose**: Handle direct API communication using ApiClient and Ktor Resources

**Pattern**:
- **Interface**: Defines suspend functions returning `Either<T>`
- **Implementation**: Injects `ApiClient`, calls `apiClient.get/post/put/delete` with Resources
- Naming: `{Feature}RemoteDataSource` (interface) + `{Feature}RemoteDataSourceImpl` (impl)
- Location: Same package, separate files

**Key Rules**:
- Constructor injects `ApiClient` (from `:core:data`)
- Each function maps to one API endpoint
- Use `RequestConfig.build(userAuthRequired = true/false)` for auth requirements
- Return `Either<T>` where T is the response model or `Unit` for no-response operations

**ApiClient Methods**:
- `apiClient.get<ResponseType, ResourceType>(resource, requestConfigs)`
- `apiClient.post<ResponseType, ResourceType>(resource, body, requestConfigs)`
- `apiClient.put<ResponseType, ResourceType>(resource, body, requestConfigs)`
- `apiClient.delete<ResponseType, ResourceType>(resource, requestConfigs)`

**Key Points**:
- Never handle errors here (ApiClient returns Either, just pass through)
- No business logic (pure API communication)
- One function per endpoint operation

### 4. Repository (data/repository/)

**Purpose**: Coordinate data sources, provide clean interface to presentation layer

**Pattern**:
- **Interface**: Defines suspend functions returning `Either<T>`
- **Implementation**: Injects DataSource(s), delegates calls (thin wrapper)
- Naming: `{Feature}Repository` (interface) + `{Feature}RepositoryImpl` (impl)
- Location: Same package, separate files

**Key Rules**:
- Constructor injects DataSource interface(s) (not implementations)
- Simple delegation pattern: just call datasource methods and return
- Returns `Either<DTO>` — the raw data-layer model (Rule 11)
- No business logic (that belongs in ViewModel)
- No DTO → UI model mapping (that's a Rule 11 violation)
- Repository should be a thin layer

**Forbidden** (Rule 11):
```kotlin
// ❌ NEVER do this — data layer must not import from presentation
import {PKG_PREFIX}.{featurename}.presentation.{Feature}UiModel

class FeatureRepositoryImpl(...) : FeatureRepository {
    override suspend fun getData(): Either<FeatureUiModel> =
        dataSource.getData().mapSuccess { it.toUiModel() }  // ❌ violates dependency rule
}

// ✅ Correct — returns the raw DTO
class FeatureRepositoryImpl(...) : FeatureRepository {
    override suspend fun getData(): Either<FeatureResponse> = dataSource.getData()
}
```

**Key Points**:
- Repository is the public API for the data layer
- ViewModels depend on Repository interface (never DataSource directly)
- ViewModel stores `result.data` (the DTO) directly inside `UiState<DTO>` on `*UiModel`
- If you need caching or multiple data sources, coordination logic goes here
- For simple features, Repository just delegates to DataSource

## Data Flow

```
API Server
    ↓
Ktor Resources (type-safe endpoint definition)
    ↓
ApiClient (network layer, handles Either wrapping)
    ↓
RemoteDataSource (interface + impl, API communication)
    ↓
Repository (interface + impl, coordination)
    ↓
ViewModel (presentation layer)
```

## Common Patterns

### GET Request Pattern
```kotlin
// DataSource
suspend fun getEntity(id: Int): Either<EntityResponse> =
    apiClient.get(
        resource = EntityResources.GetEntity(id),
        requestConfigs = RequestConfig.build(userAuthRequired = true)
    )
```

### POST Request Pattern
```kotlin
// DataSource
suspend fun createEntity(request: CreateEntityRequest): Either<Unit> =
    apiClient.post(
        resource = EntityResources.CreateEntity(),
        body = request,
        requestConfigs = RequestConfig.build(userAuthRequired = true)
    )
```

### Repository Delegation Pattern
```kotlin
// Repository implementation
class EntityRepositoryImpl(
    private val remoteDataSource: EntityRemoteDataSource
) : EntityRepository {
    override suspend fun getEntity(id: Int) = remoteDataSource.getEntity(id)

    override suspend fun createEntity(request: CreateEntityRequest) =
        remoteDataSource.createEntity(request)
}
```

## Module Dependencies

**Data layer requires**:
- `:core:common` - For Either, ErrorModel
- `:core:data` - For ApiClient, RequestConfig, shared models (if needed)

**Standard libraries**:
- Ktor client (from `:core:data` transitive dependencies)
- Kotlinx Serialization
- Kotlinx Coroutines

## Validation Strategy

**Incremental build validation** (fast, per-feature):
```bash
./gradlew :feature:{featurename}:assembleAndroidMain
```

Run this after data layer implementation to verify:
- Compilation succeeds
- Serialization annotations correct
- Type-safe resource definitions valid
- Interface/implementation pairs properly defined

**Note**: Full `./gradlew assembleDebug` validation happens during integration phase.
