# Generate Feature Tests

**Usage:** `/kmp-test-feature {featurename}`

## Phase 1: Discovery (Direct Execution)

### 1.1 Detect Namespaces

```bash
grep "namespace" feature/{featurename}/build.gradle.kts     # PKG_PREFIX
grep "namespace" core/common/build.gradle.kts        # CORE_COMMON_PKG
grep "namespace" core/data/build.gradle.kts          # CORE_DATA_PKG
```

### 1.2 Extract Context (YAML Only)

```
Glob: feature/{featurename}/src/commonMain/kotlin/**/*.kt
```

**Extract 5-line YAML summaries** (not full code):

```yaml
entities: [{name, fields}]
dataSource: {interface, implementation, methods}
repository: {interface, implementation, dependencies}
viewModel: {class, dependencies, actions, flow_name, state_slot}  # see detection rules
screen: {composable, rootComposable, callbacks}

# Capability flags (drive conditional template sections):
hasDto: true|false              # true if data/model/*Dto.kt exists OR DTO class found alongside entities
hasPagination: true|false       # true if {Feature}Response with results/count/next/previous fields exists
successValueShape: list|single  # list = repository returns Either<List<T>>; single = Either<T> (non-list)
```

**Detection rules:**
- `flow_name`: read the ViewModel's public flow property — match `val (\w+)\s*=\s*_\w+\.asStateFlow\(\)`. Under Rule 11 convention, this is `uiModel`. Pass the matched name (e.g. `uiModel`, or `uiState` for legacy features) to all presentation agents.
- `state_slot`: read `{Feature}UiModel.kt` for `val (\w+)State:\s*UiState<` — the slot prefix (e.g. `data`, `submit`, `fetch`) is what test agents substitute for `{state}`. If multiple slots exist, pass the primary one; default `data`.
- `hasDto`: glob `feature/{featurename}/src/commonMain/kotlin/**/model/*Dto.kt` → if any match, true.
- `hasPagination`: grep `data class .*Response.*results.*count` in `data/model/*.kt`.
- `successValueShape`: read the repository interface return type. `Either<List<...>>` → `list`; `Either<...>` (anything else) → `single`. (Repository returns `Either<DTO>` directly — Rule 11.)

### 1.3 Add Test Dependencies (Conditional)

A feature's `build.gradle.kts` may already include some or all test dependencies. Only add what is **actually missing** — do not blindly emit the full template.

**Step 1 — read the file:**

```bash
cat feature/{featurename}/build.gradle.kts
```

**Step 2 — check each dep individually before adding:**

| Dep | Required when | Skip if grep matches |
|-----|---------------|----------------------|
| `alias(libs.plugins.kover)` | Always (coverage) | `kover` in `plugins {}` |
| `alias(libs.plugins.mokkery)` | Always (mocking) | `mokkery` in `plugins {}` |
| `implementation(libs.bundles.testing.common)` | Always | `bundles.testing.common` |
| `implementation(libs.compose.ui.test)` | Always (UI tests) | `compose.ui.test` |
| `implementation(libs.ktor.client.mock)` | Only when feature has a **Remote DataSource** (e.g. `*RemoteDataSourceImpl.kt` using `ApiClient`) | `ktor.client.mock` |
| `implementation(libs.ktor.client.content.negotiation)` | Remote only | `ktor.client.content.negotiation` |
| `implementation(libs.ktor.serialization.kotlinx.json)` | Remote only | `ktor.serialization.kotlinx.json` |
| `implementation(libs.ktor.client.resources)` | Remote only (already in commonMain if used) | `ktor.client.resources` |
| `implementation(compose.desktop.currentOs)` in `desktopTest` | Always (UI tests on desktop) | `compose.desktop.currentOs` |

**Step 3 — only edit gradle if at least one dep is missing.** If everything is present, skip Phase 2 (sync) entirely and go straight to Phase 3.

**Reference snippet (only emit the lines you actually need to add):**

```kotlin
// In plugins block — only add what's missing:
alias(libs.plugins.kover)
alias(libs.plugins.mokkery)

// In sourceSets — only add what's missing:
commonTest {
    dependencies {
        implementation(libs.bundles.testing.common) // kotlin-test, kotlinx-coroutines-test, turbine
        implementation(libs.compose.ui.test)
        // Ktor block — ONLY for features with a Remote DataSource:
        implementation(libs.ktor.client.mock)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.kotlinx.json)
        implementation(libs.ktor.client.resources)
    }
}

val desktopTest by getting {
    dependencies {
        implementation(compose.desktop.currentOs)
    }
}
```

## Phase 2: Project Sync (Conditional)

**Only run this phase if Phase 1.3 actually modified `feature/{featurename}/build.gradle.kts`.** If all deps were already present and gradle was untouched, **skip to Phase 3 immediately** — no sync needed.

When gradle did change, request manual sync via `AskUserQuestion`:
```
question: "Test dependencies were added to {featurename}'s build.gradle.kts. Please sync the project, then confirm."
header: "Sync Required"
options:
  - label: "Sync Complete"
    description: "Project synced; new dependencies resolved"
```

**Wait for user confirmation before proceeding to Phase 3.**

## Phase 3: Spawn Agents

### 3.1 Fixtures (Sequential)

**Spawn the `kmp-test-fixtures` subagent, WAIT for completion:**

```
Subagent: kmp-test-fixtures
Prompt: "Feature: {featurename}
Package: {PKG_PREFIX}.{featurename}
CORE_COMMON_PKG: {CORE_COMMON_PKG}
CORE_DATA_PKG: {CORE_DATA_PKG}

Entities: {yaml}
UiState: {yaml}

hasDto: {true|false}
hasPagination: {true|false}"
```

### 3.2 Data Layer (Parallel - 2 agents)

**Spawn both in the SAME message:**

```
Subagent: kmp-test-datasource
Prompt: "Feature: {featurename}, Package: {PKG_PREFIX}.{featurename}
Fixtures: {PKG_PREFIX}.{featurename}.fixtures.{Feature}Fixtures
CORE_COMMON_PKG: {CORE_COMMON_PKG}, CORE_DATA_PKG: {CORE_DATA_PKG}
DataSource: {yaml}"

Subagent: kmp-test-repository
Prompt: "Feature: {featurename}, Package: {PKG_PREFIX}.{featurename}
Fixtures: {PKG_PREFIX}.{featurename}.fixtures.{Feature}Fixtures
CORE_COMMON_PKG: {CORE_COMMON_PKG}
Repository: {yaml}"
```

### 3.3 Presentation + Integration (Parallel - 3 agents)

**Spawn all THREE in the SAME message:**

```
Subagent: kmp-test-viewmodel
Prompt: "Feature: {featurename}, Package: {PKG_PREFIX}.{featurename}
Fixtures: {PKG_PREFIX}.{featurename}.fixtures.{Feature}Fixtures
CORE_COMMON_PKG: {CORE_COMMON_PKG}
ViewModel: {yaml}
flow_name: {detected flow_name, default uiModel under Rule 11}
state: {detected state_slot, default data}"

Subagent: kmp-test-ui
Prompt: "Feature: {featurename}, Package: {PKG_PREFIX}.{featurename}
Fixtures: {PKG_PREFIX}.{featurename}.fixtures.{Feature}Fixtures
UiFixtures: {PKG_PREFIX}.{featurename}.fixtures.{Feature}UiFixtures
CORE_COMMON_PKG: {CORE_COMMON_PKG}
Screen: {yaml}
Test ScreenRoot, NOT Screen."

Subagent: kmp-test-integration
Prompt: "Feature: {featurename}, Package: {PKG_PREFIX}.{featurename}
Fixtures: {PKG_PREFIX}.{featurename}.fixtures.{Feature}Fixtures
CORE_COMMON_PKG: {CORE_COMMON_PKG}, CORE_DATA_PKG: {CORE_DATA_PKG}
Stack: {yaml}
flow_name: {detected flow_name, default uiModel under Rule 11}
state: {detected state_slot, default data}
successValueShape: {list|single}"
```

## Phase 4: Run Tests

```bash
./gradlew :feature:{featurename}:cleanDesktopTest :feature:{featurename}:desktopTest
```

## Phase 5: Coverage

```bash
./gradlew :feature:{featurename}:koverHtmlReport
```

Parse `feature/{featurename}/build/reports/kover/report.xml` for line/branch coverage.

## Phase 6: Summary

```markdown
## Test Generation: {feature}

| Test | File |
|------|------|
| Fixtures | .../fixtures/{Feature}Fixtures.kt |
| UiFixtures | .../fixtures/{Feature}UiFixtures.kt |
| DataSource | .../data/datasource/{Feature}RemoteDataSourceTest.kt |
| Repository | .../data/{Feature}RepositoryImplTest.kt |
| ViewModel | .../presentation/{Feature}ViewModelTest.kt |
| UI | .../presentation/ui/{Feature}ScreenTest.kt |
| Integration | .../integration/{Feature}IntegrationTest.kt |

**Tests:** PASSED/FAILED | **Coverage:** Line X% / Branch Y%

View: `open feature/{featurename}/build/reports/kover/html/index.html`
```

## Execution Flow

```
Phase 1: Read files → extract YAML
Phase 2: AskUserQuestion(sync) → WAIT for confirmation
Phase 3.1: spawn kmp-test-fixtures → WAIT
Phase 3.2: spawn kmp-test-datasource + kmp-test-repository → WAIT (parallel)
Phase 3.3: spawn kmp-test-viewmodel + kmp-test-ui + kmp-test-integration → WAIT (parallel)
Phase 4-6: Run tests → coverage → report
```

**Max parallel: 3 agents** (memory-safe)

## Rules

1. YOU do discovery - no agent for Phase 1
2. Add test dependencies to gradle if missing (Phase 1.3)
3. Request sync and wait for user confirmation (Phase 2)
4. YAML summaries only - never full file contents
5. Fixtures first - wait before spawning others
6. Parallel batches - multiple subagent spawns in same message
7. UI tests target ScreenRoot - not Screen wrapper
