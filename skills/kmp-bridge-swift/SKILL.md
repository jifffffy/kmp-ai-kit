---
name: kmp-bridge-swift
description: "Bridge the iOS side of a Kotlin Multiplatform feature when an `expect`/`actual` needs real Swift — a native framework wrapper (MapKit, biometrics, Apple Pay, HealthKit) or a UIKit view. Writes the Swift module behind a thin Objective-C-visible interface, never inlines Swift into Kotlin. Triggers: 'iOS SDK', 'call Swift', 'native framework', 'MapKit', 'biometrics', 'Apple Pay', 'iOS actual'."
disable-model-invocation: false
version: 0.1.0
audiences: [kmp-engineer]
mode-default: review
requires:
  - shared/kmp-patterns.md
  - shared/kmp-agent-base.md
  - policies/approval-checkpoints.md
---

# kmp-bridge-swift — the iOS-Swift leg of a native capability

Every KMP feature is written in shared `commonMain` Kotlin. When a platform
capability or native view genuinely needs iOS-only APIs, Kotlin declares the
interface and the iOS side supplies a Swift implementation. This skill owns **only
that Swift leg** — the Kotlin side is finished by `kmp-create-feature` /
`kmp-modify-feature` before this skill starts.

`references/workflow.md` holds the full procedure. This file is the contract.

## The One Rule That Matters Most

**The Kotlin side is finished before this skill runs.** Reach here only when an
`iosMain` `actual` cannot be written in Kotlin and needs Swift. The expected shape is
already fixed: a `commonMain` interface (usually a `DataSource` returning
`Either<T>`) plus an `expect` declaration; Swift implements the `actual` behind a
thin interop layer. Never change the shared interface to make Swift easier.

## Tool surface

No MCP. This skill uses the file tools, the Gradle wrapper, and Xcode's build. The
architecture rules are `shared/kmp-patterns.md`; the checker is
`shared/scripts/kmp_check.py`.

## The Token-Aware Brief Contract

Before writing Swift, restate: **Context** (feature, capability, why native) /
**Objective** (one interop surface) / **Inputs** (the `commonMain` interface, the
`expect` declaration, the platform APIs to call) / **Constraints** (no Kotlin
interface change, no Swift leaking into `commonMain`) / **Acceptance criteria**
(both platform builds green, the `actual` returns the declared type, the checker
passes). Resolve unknowns from the OpenSpec spec; ask only for gaps.

## Mandatory Workflow

`references/workflow.md` is authoritative. In outline:

- **Phase 0 — read-only discovery.** Confirm the shared interface and `expect`
  declaration exist and compile for Android/desktop. Locate the iOS entry points.
  **Exit:** the contract is known. No writes.
- **Phase 1 — scaffold the Swift module.** Create the iOS framework/Swift package
  and its Objective-C-visible surface. **✋ Checkpoint.**
- **Phase 2 — implement the `actual`.** Write the Swift that satisfies the Kotlin
  declaration, plus the thin bridge. **✋ Checkpoint.**
- **Phase 3 — validate.** Build iOS, run the Kotlin checks, confirm no Swift type
  appears in `commonMain`. **Exit:** both builds green.

## Critical Rules

1. **Kotlin first, Swift second.** The `commonMain` interface and `expect/actual`
   exist before this skill starts; this skill does not design them.
2. **No Swift in `commonMain`.** The bridge is invisible from shared code.
3. **One interop surface.** The Swift module exposes exactly what the `actual`
   needs — no general-purpose wrappers.
4. **Architecture rules apply to the Kotlin you touch** (usually none) — the
   checker and `shared/kmp-patterns.md` are authoritative.
5. **Never touch `feature/**` without the marker.** `touch /tmp/.kmp-skill-active`
   on entry, `rm -f` on exit.
6. **iOS builds are the gate.** A green Android build proves nothing here.

## Domain Architecture

```
commonMain/kotlin/…/data/datasource/{Capability}DataSource.kt   interface → Either<T>
commonMain/kotlin/…/platform/{Capability}.expect.kt             expect fun/val
iosMain/kotlin/…/platform/{Capability}.ios.kt                   actual → calls Swift
iosApp/…/{Capability}Bridge.swift                                the Swift implementation
```

## Modes & Policies

`mode-default: review`. Every Swift file is shown before it is written. Nothing is
auto-applied. See `policies/approval-checkpoints.md`.

## State Management

Write the run to `.kmp/run.json` (capability, phase, the interop surface names,
build status). Re-derive before resuming. See `shared/kmp-state.md`.

## User Checkpoints

| After phase | Artifacts shown | What we ask |
|---|---|---|
| 1 | Swift module skeleton + the interop surface | approve the boundary before implementing |
| 2 | Swift implementation + iOS build output | confirm the `actual` is satisfied |

## Naming Conventions

Swift types are `PascalCase`; the bridge type is `{Capability}Bridge`. Kotlin naming
follows `shared/kmp-patterns.md`. Never rename the shared interface or the Kotlin
package from this skill.

## Anti-Rationalization Table

| Excuse the model makes | Why it's wrong | Countermeasure that halts the flow |
|---|---|---|
| "I'll just change the `commonMain` interface so the Swift is simpler." | The interface is the contract the rest of the feature depends on; bending it to fit Swift leaks iOS into shared code. | Stop. Implement to the declared interface. If it genuinely cannot be met, raise it as a spec problem, not a code edit. |
| "The iOS build is slow, Android passing is good enough." | A Kotlin `actual` with no working Swift fails only at iOS, which is the one thing this skill exists to prevent. | Stop. Build iOS before reporting done. |
| "I'll expose a broad Swift API in case other features need it." | General-purpose wrappers become unowned shared surface and violate the one-surface rule. | Expose only what the `actual` needs. |
| "I'll write the Swift inline in the Kotlin file's `iosMain`." | `iosMain` can call Swift; it cannot contain it. Mixing the two defeats the interop boundary. | Put Swift in the iOS target, keep `iosMain` Kotlin thin. |

## Helper Code Snippets

```bash
touch /tmp/.kmp-skill-active
./gradlew :feature:{NAME}:assembleAndroidMain
# iOS: build the shared framework, then the app
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
rm -f /tmp/.kmp-skill-active
```

## Reference Resources

- `references/workflow.md` — the full bridge procedure (ported from the pipeline's
  Swift-bridge skill).
- `shared/kmp-patterns.md` — Rule 14 (platform capabilities & native views).
- `skills/kmp-create-feature/references/architecture/platform.md` — the Kotlin side.

## Supporting Files

| File | Use |
|---|---|
| `references/workflow.md` | the end-to-end Swift-bridge workflow — read before Phase 1 |

**Doctrine paths.** `shared/…` and `policies/…` resolve inside this bundle in native installs (vendored by the installer); in an opencode install they live at the kit root, two directories up from this file (`skills/<name>/SKILL.md`).
