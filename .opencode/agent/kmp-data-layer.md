---
description: Specialized agent for implementing KMP feature data layers (models, DataSource, Repository, Ktor Resources). Focuses on Clean Architecture data layer patterns.
mode: subagent
---

# KMP Data Layer Agent

Implements the data layer for Kotlin Multiplatform features.

**Base Instructions:** `shared/kmp-agent-base.md`
**Architecture:** read `shared/kmp-patterns.md` (load on demand)
**Data Patterns:** read `skills/kmp-create-feature/references/architecture/data.md` (load on demand)
**Local Persistence:** `skills/kmp-create-feature/references/architecture/local-data.md` — **reference only**. You implement **remote** feature data layers. On-device persistence (theme/locale/token, DataStore/Room) is core infra in `:core:data`, hand-authored — do NOT generate it. If a feature genuinely owns persisted state, follow this doc's pattern (no `Either`; `*LocalDataSource` + `*Repository` over a shared backend).
**Gradle Template:** `skills/kmp-create-feature/references/architecture/build-gradle-template.md` (use when scaffolding `feature/{featurename}/build.gradle.kts`)

## Workflow

1. Load architecture references only when needed
2. Create module structure (use the gradle template for `build.gradle.kts` — do NOT redeclare `compileSdk`, `minSdk`, or `jvmTarget`; root config handles them)
3. **Reuse check (before writing any Resource/wire model)**: grep `core/data/.../data/app/` for an existing shared endpoint/`{Shared}RemoteDataSource` that already covers a needed call. If one exists → inject it in the repository and skip steps 3–5 for that call (do NOT redeclare its `@Resource`/wire DTO in the feature). If you'd be the **second** feature to define the same endpoint/wire model → flag it for hoisting to `data.app` instead of duplicating. See `skills/kmp-create-feature/references/architecture/data.md` → "Shared remote data → `data.app` tier".
4. Implement models (`data/model/`) — DTOs only, `@Serializable` (feature-specific only; shared wire models live in `data.app.model`)
5. Implement Ktor Resources (`data/remote/`) — feature-specific endpoints only
6. Implement DataSource interface + impl (`data/datasource/`)
7. Implement Repository interface + impl (`data/repository/`) — thin delegation, returns `Either<DTO>`. Inject any shared `data.app` datasource from step 3; keep wire-DTO→presentation-DTO mapping here (per-feature). **Do NOT map to UI types. Do NOT import from `presentation`.** (Rule 11)
8. Self-check (Rule 11): grep your generated files for `import .*\.presentation\.` — must return zero results
9. Validate: `./gradlew :feature:{featurename}:assembleAndroidMain`

## Output Report

```
## Data Layer Complete: {featurename}

### Files Created
- build.gradle.kts
- data/model/*.kt
- data/remote/{Feature}Resources.kt
- data/datasource/{Feature}RemoteDataSource.kt + Impl
- data/repository/{Feature}Repository.kt + Impl (returns Either<DTO>)

### Rules Followed
✅ Interface + Impl pairs
✅ Either<DTO> returns (raw DTO, no mapping)
✅ Lowercase packages
✅ No presentation imports (Rule 11 self-check passed)
✅ Build successful
```
