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
Penpot skills. The KMP architecture checker is `shared/scripts/kmp_check.py`, and the
routing computation is `shared/scripts/kmp_route.py`.

## Routing table

Read `.kmp/route.json` first — it is the persisted verdict. Regenerate it whenever the
file system changed since it was written (a spec was proposed, a module was created):

```bash
python3 shared/scripts/kmp_route.py --capability <slug> [--ui]
```

| Signal in the request | Route to | Status |
|---|---|---|
| new KMP app / scaffold a KMP project / new Kotlin Multiplatform starter | `kmp-init` | implemented |
| new feature, new screen module, "build a feature" | `kmp-create-feature` | implemented |
| change/extend/fix an existing feature | `kmp-modify-feature` | implemented |
| generate tests for a feature | `kmp-test-feature` | implemented |
| review/audit a feature against the 14 rules | `kmp-review-feature` | implemented |
| model the domain / "what are the entities" | `kmp-domain-model` | implemented |
| an iOS `actual` needs Swift / native framework | `kmp-bridge-swift` | implemented |
| build or style feature UI in Compose | `kmp-using-design-system` | implemented |
| design-first: a screen that does not exist in Penpot yet | `penpot-build-screen`, then `kmp-create-feature` | design layer owns phase 1 |
| "where do I start" | this skill — ask one question, then route | — |

Create vs. modify is decided by one read-only check: does `feature/<name>/` exist? If it
does, route to `kmp-modify-feature`; if not, `kmp-create-feature`. A capability that
already has an OpenSpec spec is a modification, not a creation.

"New app" vs. "new feature" is decided by another: does `core/common` exist? If it does,
the project already exists and the request is a feature; if not, it is `kmp-init`.

If a request names a skill that does not exist yet (a future capability), say so plainly:
name the missing skill, and stop. Do not improvise the workflow it would have owned.

## Preflight — run it, don't re-derive it

```bash
python3 shared/scripts/kmp_route.py --capability <slug> [--action create|modify|review|test] [--ui]
```

The script computes the mechanical facts and writes `.kmp/route.json`: whether the project is
managed, whether it is a create or a modify, which artifacts exist (`spec`, `domain`, `design`),
the in-flight OpenSpec changes with their task progress and age, the **ordered** gap list, and the
single next command. It never mutates anything but `.kmp/route.json`.

Your job is the half it cannot do: read the request, choose the capability slug, decide whether the
work has UI, and **name exactly one target skill**.

## Ordering — spec first, always

The order between missing artifacts is **not** a judgment call, and the router does not make it:

```
spec (required) → domain (required for create/modify) → design (optional, UI only) → build
```

`route.json` carries this as `gaps` (each with a `blocking` flag), `next`, and `chain`. When a gap
is blocking, the router's answer is that gap's fix — **not** the build skill. Specifically:

- **No spec → `/opsx-propose` first.** Requirements are OpenSpec's and only OpenSpec's. The build
  skills refuse to infer them, so routing to a build skill here only moves the refusal later.
- **No domain model → `kmp-domain-model` next** (for create/modify). It is the layer between spec
  and build; without it, DTO shapes and derived fields are guessed during implementation.
- **No design, and the work has UI → `/penpot-build-screen`.** Non-blocking: the design layer can
  run in parallel, and the build may proceed on the design system's defaults if the user prefers.

If `route.json` reports a stale in-flight change (0 tasks done, untouched for a day), surface it
and ask the user to **resume or archive** it before starting new work — do not silently pile on.

## Modes & Policies

`mode-default: suggest`. Routing is always read-only; it applies nothing and asks
for nothing. Approval gates begin in the routed skill (see
`policies/approval-checkpoints.md`).

## Anti-Rationalization Table

| Excuse the model makes | Why it's wrong | Countermeasure that halts the flow |
|---|---|---|
| "The request is small, I'll just write the Kotlin directly." | Direct edits bypass the pipeline hooks and the architecture checker, which is the whole point of the kit. | Stop. Route to the owning skill. Feature files are guarded by `.opencode/plugin/protect-feature.ts`. |
| "Both create and test are needed, I'll do them together." | One request, one skill. Chaining hides a checkpoint the user is entitled to. | Route to the first skill only; the next is reached after its exit criterion. |
| "No spec exists, but the code is obvious enough to skip planning." | Planning is OpenSpec's job; inventing requirements silently is how specs and code drift. | Stop. `route.json` marks the spec gap blocking: route to `/opsx-propose` first. |
| "I'll pick the skill that sounds closest and adapt it." | A skill's checkpoints and acceptance criteria are not interchangeable. | Name the missing skill and the phase. Do not substitute. |
| "The domain model is optional, I'll build straight from the spec." | The spec says what the system must do, not what the concepts are. Skipping it is how a count becomes a stale stored field and a Role becomes a duplicate entity. | `route.json` marks it blocking for create/modify — route to `kmp-domain-model`. |
| "I'll re-derive the routing by reading the repo myself." | A second derivation will eventually disagree with the first, and leave no record of what was decided. | Run `kmp_route.py`; read `.kmp/route.json`. |

## Reference Resources

- `shared/kmp-patterns.md` — the 14 architecture rules (single source of truth).
- `shared/domain-modeling.md` — Coad's Color Modeling, and where it sits in the chain.
- `shared/kmp-state.md` — the `RUN_ID` ledger; `.kmp/route.json` is its routing sibling.
- `policies/approval-checkpoints.md` — where a run must stop.

## Supporting Files

No `references/` tree: the routing computation is `shared/scripts/kmp_route.py` and the verdict is
`.kmp/route.json`. Read the JSON, not this file, to know the current decision.

**Doctrine paths.** `shared/…` and `policies/…` resolve inside this bundle in native installs (vendored by the installer); in an opencode install they live at the kit root, two directories up from this file (`skills/<name>/SKILL.md`).
