# Phase 0: Context Discovery

**Purpose**: Auto-detect project configuration before analyzing user's prompt. This enables the skill and agents to work across different KMP projects without hardcoded values.

**When**: Run automatically as the first step, before input resolution.

---

## Checklist

```
Context Discovery Progress:
- [ ] Step 0.0: Read the project config (APP_MODULE; catalog accessor is `libs`)
- [ ] Step 0.1: Detect package prefix (PKG_PREFIX)
- [ ] Step 0.2: Detect integration paths (INIT_KOIN_PATH, NAV_HOST_PATH)
- [ ] Step 0.3: Detect core modules (CORE_MODULES)
- [ ] Step 0.4: Detect core module namespaces (CORE_COMMON_PKG, CORE_DATA_PKG, CORE_DESIGNSYSTEM_PKG)
- [ ] Step 0.5: Store context for agent invocation
```

---

## Step 0.0: Read the project config

`.kmp.json` at the repo root is an optional project config. Read it for the app
module name only; everything else has a fixed kit default.

```
Read: .kmp.json   (absent → use the defaults below)
```

| Value | Field | Default when absent |
|-------|-------|---------------------|
| `{CATALOG}` | — | `libs` (the kit's catalog accessor) |
| `{APP_MODULE}` | `appModule` | `composeApp` |

**Never hardcode `composeApp`.** Substitute `{APP_MODULE}` everywhere. The catalog
accessor is always `{CATALOG}` = `libs`.

---

## Step 0.1: Detect Package Prefix

1. **Find a feature's build.gradle.kts**:
   ```
   Glob: feature/*/build.gradle.kts (take first match)
   ```

2. **Extract namespace**:
   ```
   Grep for: namespace = "..."
   Example: namespace = "com.example.login" → extract "com.example.login"
   ```

3. **Derive package prefix**:
   ```
   Remove the last segment: "com.example.login" → "com.example"
   Store as: {PKG_PREFIX}
   ```

**Fallback**: If no features exist yet (fresh install via the kit installer), grep for `package ` declarations in `core/common/src/**/*.kt`. Take the first match (e.g., `package thisissadeghi.common`) and apply the **same strip-last-segment rule** to derive `{PKG_PREFIX}`:

```
"thisissadeghi.common" → strip "common" → "thisissadeghi"
"com.example.common"   → strip "common" → "com.example"
```

Single-segment results (e.g. `thisissadeghi`) are valid — store as-is.

---

## Step 0.2: Detect Integration Paths

1. **Find initKoin file**:
   ```
   Grep "startKoin" in {APP_MODULE}/src/**/*.kt
   Store path as: {INIT_KOIN_PATH}
   Example: composeApp/src/commonMain/kotlin/com/example/myapp/initKoin.kt
   ```

2. **Find navigation host file**:
   ```
   Grep "XNavHost|NavHost" in {APP_MODULE}/src/**/*.kt (exclude designsystem)
   Store path as: {NAV_HOST_PATH}
   Example: composeApp/src/commonMain/kotlin/com/example/myapp/BaseAppNavHost.kt
   ```

---

## Step 0.3: Detect Core Modules

```
Glob: core/*/build.gradle.kts
Extract module names from paths
Store as: {CORE_MODULES} (e.g., common, data, designsystem)
```

---

## Step 0.4: Detect Core Module Namespaces

For portability across projects, detect each core module's namespace:

1. **Read core module build.gradle.kts files**:
   ```
   core/common/build.gradle.kts → grep `namespace = "..."` → {CORE_COMMON_PKG}
   core/data/build.gradle.kts → grep `namespace = "..."` → {CORE_DATA_PKG}
   core/designsystem/build.gradle.kts → grep `namespace = "..."` → {CORE_DESIGNSYSTEM_PKG}
   ```

2. **Derive PKG_PATH** (for file paths):
   ```
   Convert {PKG_PREFIX} dots to slashes:
   - "acme" → "acme"
   - "com.example" → "com/example"
   Store as: {PKG_PATH}
   ```

3. **Fallback**: If namespace not found, derive as `{PKG_PREFIX}.{module}`

---

## Step 0.5: Store Context for Agent Invocation

Store all detected values to pass to specialized agents:

```
PROJECT_CONTEXT:
  CATALOG: libs
  APP_MODULE: {detected appModule, default composeApp}
  PKG_PREFIX: {detected value}
  PKG_PATH: {PKG_PREFIX with dots → slashes}
  CORE_COMMON_PKG: {detected from core/common}
  CORE_DATA_PKG: {detected from core/data}
  CORE_DESIGNSYSTEM_PKG: {detected from core/designsystem}
  INIT_KOIN_PATH: {detected path}
  NAV_HOST_PATH: {detected path}
  CORE_MODULES: {list of modules}
```

**Important**: Pass this context when invoking any specialized agent in Phase 4.

---

## Output

After completing context discovery, you should have all values needed for:
- Generating correct package names for the implementation
- Passing context to specialized agents
- Knowing exact paths for integration points

Proceed to **Phase 1 (input resolution)**.
