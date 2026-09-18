# kmp-routing — dispatch a KMP build request

The build layer's router. It answers *"which skill builds this?"* and nothing else.

The kit has one router per layer, and they do not overlap:

| Layer | Router | Owns |
|---|---|---|
| Planning | OpenSpec (`/opsx-propose`, `/opsx-apply`) | proposal → spec → design → tasks |
| Design | `workflows/routing` → `penpot-router` | how it looks in Penpot |
| **Build** | **`kmp-router`** (this workflow) | how it ships in Kotlin |

## Preflight (read-only)

1. `openspec list` — an active change is the strongest signal of what is being built.
2. Read `openspec/specs/<capability>/spec.md` if it exists — a spec means *modify*, not create.
3. `python3 shared/scripts/kmp_check.py --baseline` — start from a known architecture baseline.
4. For design-driven work, confirm the Penpot handoff artifact exists before routing to a build skill.

## Routes

| You say | Target |
|---|---|
| new feature, create a KMP feature, build this screen in Kotlin, implement the spec | `kmp-create-feature` |
| add to this feature, change the dashboard, update this screen, fix this feature | `kmp-modify-feature` |
| review this feature, check the dashboard, audit feature, is this feature correct | `kmp-review-feature` |
| generate tests, test this feature, add tests, write tests for the dashboard | `kmp-test-feature` |
| iOS SDK, call Swift, native framework, MapKit, biometrics, Apple Pay | `kmp-bridge-swift` |
| use the design system, which component should I use, build this UI in Compose | `kmp-using-design-system` |

All six build-layer skills are routed. `kmp-router` is the entry point, never a route.

## Ambiguity policy

If the request could be design-first or build-first, ask exactly one question —
*design it in Penpot, or build it in Kotlin from an existing spec?* — then dispatch
to that layer's router.

## Mutation

`false`. Routing never edits code or design.
