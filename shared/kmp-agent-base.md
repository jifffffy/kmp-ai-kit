# Agent Common Instructions

All agents read this file for shared patterns.

## Architecture Reference

Load on demand: read `shared/kmp-patterns.md`

## Context Variables

The orchestrator provides a subset of the variables below depending on the
agent role. **Trust only what your specific invocation passes** — do not
assume a variable is present.

Always provided to every agent:

- `{featurename}` - Feature name (lowercase)
- `{PKG_PREFIX}` - Package prefix (e.g., `com.example`)
- `{PKG_PATH}` - Package as path (e.g., `com/example`)
- `{CORE_COMMON_PKG}` - Core common package

Provided when relevant to the agent's layer (see table):

| Variable | Data agent | UI agent | Integration agent |
|----------|------------|----------|-------------------|
| `{CORE_DATA_PKG}` | ✅ | — | ✅ |
| `{CORE_DESIGNSYSTEM_PKG}` | ✅ | ✅ | ✅ |
| `{CORE_MODULES}` | ✅ | — | ✅ |
| `{INIT_KOIN_PATH}` | — | — | ✅ |
| `{NAV_HOST_PATH}` | — | — | ✅ |

Source of truth for what the orchestrator passes:
read `skills/kmp-create-feature/references/phases/04-implementation.md`.

## Build Validation

After implementation:
```bash
./gradlew :feature:{featurename}:assembleAndroidMain
```

## On Build Failure

1. Load layer-specific troubleshooting (max 3 retries)
2. Identify error pattern
3. Fix and retry
4. Report if still failing

Troubleshooting files:
- Data: `skills/kmp-create-feature/references/troubleshooting/data.md`
- UI: `skills/kmp-create-feature/references/troubleshooting/ui.md`
- Integration: `skills/kmp-create-feature/references/troubleshooting/integration.md`

## Error Handling Pattern

Use `ErrorConst` from `{CORE_DATA_PKG}.ErrorConst`:
- `ErrorConst.NoNetwork` - Connection errors
- `ErrorConst.Unauthorized` - HTTP 401
- `ErrorConst.SerializationError` - JSON parsing
- `ErrorConst.ServerUnknownError(httpCode)` - Unknown errors
