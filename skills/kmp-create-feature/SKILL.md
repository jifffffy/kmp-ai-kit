---
name: kmp-create-feature
description: "Create a complete Kotlin Multiplatform feature with Clean Architecture — data → presentation → DI/NavHost, shared across Android + iOS. Reads its requirements from the OpenSpec spec and its design from the Penpot handoff, then implements layer by layer behind user checkpoints. Triggers: 'create a KMP feature', 'new feature', 'build this screen in Kotlin', 'implement the spec', 'add a feature module'."
disable-model-invocation: false
version: 0.1.0
audiences: [kmp-engineer, design-engineer]
mode-default: review
requires:
  - shared/kmp-patterns.md
  - shared/kmp-state.md
  - shared/kmp-x-components-catalog.md
  - policies/approval-checkpoints.md
---

# kmp-create-feature — one feature, one module, four integration points

Creates a KMP feature in the kit's enforced Clean Architecture shape. The feature
never depends on another feature; shared code lives in versioned `core/` tiers.

This skill is the **build layer**. Its inputs are file contracts, not calls:
- **Requirements** — `openspec/specs/<capability>/spec.md` (OpenSpec owns them).
- **Design** — the Penpot handoff artifact (`DESIGN.md` + annotation layer).
- **Rules** — `shared/kmp-patterns.md` (the 14 architecture rules, authoritative).
- **Mechanics** — `shared/scripts/kmp_check.py` decides pass/fail, not this prompt.

## The One Rule That Matters Most

Build the **smallest useful unit per step**, and stop at every checkpoint. Never
scaffold an entire feature in one pass. Each layer (data → UI → integration) is its
own step with its own build + checker run, and a ✋ checkpoint before the next.

## Feature-file guard

Feature sources are protected. Before editing anything under `feature/`, create the
skill marker; remove it when the run ends or on any early exit:

```bash
touch /tmp/.kmp-skill-active      # enables edits under feature/
rm -f /tmp/.kmp-skill-active       # always remove — success, failure, or abort
```

`.opencode/plugin/protect-feature.ts` blocks Edit/Write under `feature/` while the
marker is absent, and `opencode.json` raises a backstop `ask` on those paths. This
replaces KMPilot's Claude `PreToolUse` hook, which never ran under opencode.

## The Token-Aware Brief Contract

Before mutating, restate the request as: **Context / Objective (single) / Inputs /
Constraints / Acceptance Criteria (quantitative)**. Resolve the unknowns from the
spec and the design artifact — ask only for what neither contains.

| Field | Source | Must contain |
|---|---|---|
| Context | OpenSpec `proposal.md` | product, audience, why now |
| Objective | OpenSpec `spec.md` | exactly one capability |
| Inputs | spec + Penpot design | screens, states, tokens, component refs |
| Constraints | `shared/kmp-patterns.md` | forbidden deps, inviolable rules |
| Acceptance criteria | spec scenarios + checker | build green, `archTest` green, 4 points wired |

## Workflow

Phase 0 → Phase 1 → Phase 2 → ✋ approve → Phase 3 → **marker on** → Phase 4 → ✋ → **marker off** → Phase 5

Per-phase detail lives in `references/phases/`; read a phase file when you reach it, not
up front.

### Phase 0 — Context discovery (read-only)
`references/phases/00-context.md`. Resolve the app module, catalog accessor, package
prefix and namespaces from `.kmp.json` (falling back to `composeApp` / `libs`), plus
`INIT_KOIN_PATH`, `NAV_HOST_PATH` and the core modules. Read an existing feature as the
reference shape. **Exit:** every anchor resolved. No edits.

### Phase 1 — Input resolution (read-only)
`references/phases/01-inputs.md`. Locate the OpenSpec change/spec and the Penpot handoff
(`DESIGN.md`). If the design is missing and the work is visual, **stop** and point at
`/penpot-build-screen` — design is the Penpot layer's job. **Exit:** inputs recorded in
`.kmp/run.json`. No edits.

### Phase 2 — Contract + plan
`references/phases/02-contract.md`. Emit the Token-Aware Brief Contract, resolve the
Platform Profile (Rule 14), and choose the agent set + execution strategy.
**✋ Checkpoint:** show the contract and plan; wait for an explicit OK. "Looks good"
approves this plan only.

### Phase 3 — Task plan
`references/phases/03-tasks.md`. Turn the approved contract into the OpenSpec change's
`tasks.md` (ordered data/platform → ui → integration), each task one logical step
checkable by a build plus a checker run. **Exit:** the task list exists in
`openspec/changes/{change-id}/tasks.md`. Still no `feature/` edits.

### Phase 4 — Implementation (marker on)
`references/phases/04-implementation.md`. Run the agent set from Phase 2 layer by layer,
consuming the Penpot `DESIGN.md` where the UI is built. Build and check after each layer:
`./gradlew :feature:{name}:assembleAndroidMain` then
`python3 shared/scripts/kmp_check.py {name}`. **✋ Checkpoint** after each layer.
**Exit:** build green, checker green, all four integration points wired.

### Phase 5 — Handoff (marker off)
`references/phases/05-handoff.md`. Reconcile the living spec, run the final checker, **run the app
(runtime gate — `./gradlew :composeApp:run`)**, reconcile every `tasks.md` checkbox, then archive
with `/opsx-archive` and write the run ledger to `.kmp/run.json`. This skill **never** creates a
spec copy in the code tree.

## Critical Rules

1. **User confirmation** after Phase 2, and at each Phase 4 layer checkpoint — never
   proceed without it.
2. **One module per feature**, and a feature never depends on another feature.
3. **`Either<T>` for fallible ops**; never throw across a layer boundary (Rule 2).
4. **Loading/Failed UI is shared** — `AppLoadingState`/`AppErrorState` from
   `designsystem.app`; never a private `LoadingContent`/`FailedContent`.
5. **Spec is OpenSpec's, design is Penpot's** — this skill writes neither.
6. **Validate the build and the checker after every layer**, not once at the end.
7. **Never hand-edit feature files outside this skill** — the guard exists for a reason.
8. **Marker discipline:** `touch /tmp/.kmp-skill-active` at Phase 4 start, `rm -f` at
   Phase 5 end or on any early exit.
9. **`archTest` green ≠ the app works.** Run the desktop target before handoff. The checker is
   static and cannot see the Koin graph, a `@Serializable` contract, or a runtime cast — all three
   of which have crashed apps that passed every check. See `shared/kmp-runtime-verification.md`.
   If the app cannot be run, record `runtime: not-run` — never imply a pass.
10. **Tick `tasks.md` as you go**, and reconcile it before archiving. Unticked boxes force a
    "0/N tasks" warning and leave a false incomplete record in the archive.

## Modes & Policies

`mode-default: review`. Applied step by step with a preview at each checkpoint. This
skill auto-applies nothing from the kit safe-set; geometry, component restructuring
and new tokens are never auto-applied. See `shared/modes-and-policies.md` and
`policies/approval-checkpoints.md`.

## State Management

One ledger entry per run at `.kmp/run.json` (`RUN_ID`, capability, spec path, design
path, app module, phase, layer status, checker result). Re-read the ledger and
re-derive reality with `kmp_check.py --baseline` before resuming a truncated run.
See `shared/kmp-state.md`.

## User Checkpoints

| After phase | Artifacts shown | What we ask |
|---|---|---|
| 2 | brief contract + layer plan | approve the plan (not the code) |
| 3 | data/platform files + build output | approve the data layer before UI |
| 4 | build + `archTest` + test results | confirm done → archive the OpenSpec change |

## Naming Conventions

Package names lowercase (`productdetail`, never `product-detail`). File and type
naming follows `shared/kmp-patterns.md` → Naming Conventions. Never hand-rename
packages or files; that is a separate, deliberate refactor.

## Anti-Rationalization Table

| Excuse the model makes | Why it's wrong | Countermeasure that halts the flow |
|---|---|---|
| "I'll write the whole feature in one pass to save time." | One-shot scaffolds skip checkpoints and hide layer boundary mistakes the checker catches per-layer. | Stop. One layer per step; build + checker after each. |
| "There's no spec and no design, but the ask is clear — I'll infer it." | Inferring requirements silently is how spec and code drift; design is not this skill's output. | Stop. Route to `/opsx-propose` for the spec and `/penpot-build-screen` for the design. |
| "I'll add a temporary loading composable to move fast." | Loading/Failed UI is shared; a private one becomes an orphan and fails review. | Use `AppLoadingState`/`AppErrorState` from `designsystem.app`. |
| "The feature needs another feature's code, I'll just import it." | A feature never depends on another feature — that is what `core/` is for. | Move the shared code into the correct `core/` tier, then import that. |
| "`archTest` is failing on pre-existing code, I'll skip it." | The checker is the gate; skipping it is how the architecture drifts. | Run `kmp_check.py --baseline` to separate pre-existing from new, fix the new, report the rest. |
| "`archTest` is green, so the feature is done." | The checker reads source one file at a time. It cannot see the assembled Koin graph, a serializer contract against a real payload, or a cast that only runs at runtime — all three have crash apps that passed it. | Run `./gradlew :composeApp:run` before handoff (`shared/kmp-runtime-verification.md`). |
| "I can't see the app window, so I'll assume it's fine." | Unverifiable is not verified. | Render smoke to `build/smoke/*.png` and look at it, or record `runtime: not-run` honestly. |
| "I'll tick the tasks at the end / skip it, the work is obviously done." | Unticked boxes make the archive claim work is incomplete, and force a warning people learn to ignore. | Tick each box as its task lands; reconcile before `archive`. |

## Helper Code Snippets

```bash
# baseline health before starting
python3 shared/scripts/kmp_check.py --baseline

# per-layer build gate
./gradlew :feature:{NAME}:assembleAndroidMain

# feature-level checks
./gradlew :feature:{NAME}:desktopTest && python3 shared/scripts/kmp_check.py {NAME}
```

## Reference Resources

- `shared/kmp-patterns.md` — the 14 rules, naming, feature structure, build commands.
- `shared/kmp-x-components-catalog.md` — the `X*` component contracts.
- `shared/scripts/kmp_check.py` — the deterministic checker.

## Supporting Files

| File | Use |
|---|---|
| `references/phases/00-context.md` | Phase 0 — project context discovery |
| `references/phases/01-inputs.md` | Phase 1 — resolve the OpenSpec spec + Penpot design |
| `references/phases/02-contract.md` | Phase 2 — brief contract, Platform Profile, agent set |
| `references/phases/03-tasks.md` | Phase 3 — OpenSpec task plan |
| `references/phases/04-implementation.md` | Phase 4 — staged agent execution |
| `references/phases/05-handoff.md` | Phase 5 — reconcile, archive, ledger |
| `references/architecture/data.md` | data layer: models, DataSource, Repository, Ktor |
| `references/architecture/ui.md` | presentation: UiModel, ViewModel, Screens, navigation |
| `references/architecture/platform.md` | Rule 14 — capabilities and native views |
| `references/architecture/integration.md` | the 4 integration points + bottom-bar tabs |
| `references/architecture/local-data.md` | on-device persistence (core infra — reference only) |
| `references/architecture/build-gradle-template.md` | the feature module's `build.gradle.kts` |
| `references/templates/task-template.md` | one task's shape |
| `references/troubleshooting/{index,data,ui,integration}.md` | build-failure playbooks |

Companion subagents: `.opencode/agent/kmp-{data-layer,ui-layer,platform,integrator}.md`.
Companion skill for UI reuse: `skills/kmp-using-design-system/SKILL.md`.

**Doctrine paths.** `shared/…` and `policies/…` resolve inside this bundle in native installs (vendored by the installer); in an opencode install they live at the kit root, two directories up from this file (`skills/<name>/SKILL.md`).
