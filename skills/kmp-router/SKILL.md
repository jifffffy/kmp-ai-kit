---
name: kmp-router
description: "Thin dispatcher for Kotlin Multiplatform build work in this kit. Use FIRST on any KMP request to pick exactly one skill: create, modify, review, test, bridge Swift, or use the design system. Reads the plan from OpenSpec and the design from Penpot. Triggers: 'new KMP feature', 'add feature', 'build this screen in Kotlin', 'implement the spec', 'where do I start'."
disable-model-invocation: false
version: 0.1.0
audiences: [kmp-engineer, design-engineer, product-designer]
mode-default: suggest
requires:
  - shared/kmp-patterns.md
  - shared/kmp-state.md
  - policies/approval-checkpoints.md
---

# kmp-router — one request, one skill

The dispatcher for the KMP build layer. It never mutates code itself. It reads the
request plus the file state, then names **exactly one** target skill and stops.

The kit is layered: **OpenSpec plans** (what/why), **Penpot designs** (how it looks),
**KMP skills build** (how it ships). A request can arrive from any layer — a raw user
ask, an OpenSpec change's `tasks.md`, or a Penpot handoff. Routing is the same.

## The One Rule That Matters Most

Route to **one** skill, then stop. Never chain two builds in one call, and never
start the work the routed skill owns. This skill's only output is a decision.

## Tool surface

This kit is opencode-only. Skills are loaded from `skills/` (see `opencode.json` →
`skills.paths`). Planning artifacts come from the OpenSpec CLI in `.opencode/`
(`/opsx-propose`, `/opsx-apply`, `/opsx-archive`). Design artifacts come from the
Penpot skills. The KMP architecture checker is `shared/scripts/kmp_check.py`.

## Routing table

Read `openspec/changes/` first when a proposal exists — a change in flight is the
strongest signal of what is being built.

| Signal in the request | Route to | Status |
|---|---|---|
| new feature, new screen module, "build a feature" | `kmp-create-feature` | implemented |
| change/extend/fix an existing feature | `kmp-modify-feature` | implemented |
| generate tests for a feature | `kmp-test-feature` | implemented |
| review/audit a feature against the 14 rules | `kmp-review-feature` | implemented |
| an iOS `actual` needs Swift / native framework | `kmp-bridge-swift` | implemented |
| build or style feature UI in Compose | `kmp-using-design-system` | implemented |
| design-first: a screen that does not exist in Penpot yet | `penpot-build-screen`, then `kmp-create-feature` | design layer owns phase 1 |
| "where do I start" | this skill — ask one question, then route | — |

Create vs. modify is decided by one read-only check: does `feature/<name>/` exist? If it
does, route to `kmp-modify-feature`; if not, `kmp-create-feature`. A capability that
already has an OpenSpec spec is a modification, not a creation.

If a request names a skill that does not exist yet (a future capability), say so plainly:
name the missing skill, and stop. Do not improvise the workflow it would have owned.

## Preflight (read-only, before routing)

1. `openspec list` — any active change? If yes, read its `proposal.md` and `tasks.md`.
2. `openspec/specs/` — does a spec already cover this capability? A spec means the
   work is modify, not create.
3. `python3 shared/scripts/kmp_check.py --baseline` — repo architecture health, so
   the routed skill starts from a known baseline.
4. If a Penpot design is referenced, confirm the handoff artifact exists
   (`DESIGN.md` / annotation layer) before routing to a build skill.

## Modes & Policies

`mode-default: suggest`. Routing is always read-only; it applies nothing and asks
for nothing. Approval gates begin in the routed skill (see
`policies/approval-checkpoints.md`).

## Anti-Rationalization Table

| Excuse the model makes | Why it's wrong | Countermeasure that halts the flow |
|---|---|---|
| "The request is small, I'll just write the Kotlin directly." | Direct edits bypass the pipeline hooks and the architecture checker, which is the whole point of the kit. | Stop. Route to the owning skill. Feature files are guarded by `.opencode/plugin/protect-feature.ts`. |
| "Both create and test are needed, I'll do them together." | One request, one skill. Chaining hides a checkpoint the user is entitled to. | Route to the first skill only; the next is reached after its exit criterion. |
| "No spec exists, but the code is obvious enough to skip planning." | Planning is OpenSpec's job; inventing requirements silently is how specs and code drift. | Stop. Ask the user to `/opsx-propose` first, or route to `kmp-create-feature` whose Phase 0 raises the missing spec. |
| "I'll pick the skill that sounds closest and adapt it." | A skill's checkpoints and acceptance criteria are not interchangeable. | Name the missing skill and the phase. Do not substitute. |

## Reference Resources

- `shared/kmp-patterns.md` — the 14 architecture rules (single source of truth).
- `shared/kmp-state.md` — the `RUN_ID` ledger for resumable runs.
- `policies/approval-checkpoints.md` — where a run must stop.

## Supporting Files

No `references/` or `scripts/` yet — this is the Phase 0 skeleton. Phase 1 adds the
routed skills and their reference trees.

**Doctrine paths.** `shared/…` and `policies/…` resolve inside this bundle in native installs (vendored by the installer); in an opencode install they live at the kit root, two directories up from this file (`skills/<name>/SKILL.md`).
