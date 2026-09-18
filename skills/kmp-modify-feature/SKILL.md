---
name: kmp-modify-feature
description: "Change an existing Kotlin Multiplatform feature the spec-first way: read the OpenSpec spec, draft the spec delta, get approval, then implement behind the feature guard. Handles added screens/states, new platform capabilities, and bottom-bar tab changes. Triggers: 'add to this feature', 'change the dashboard', 'update this screen', 'fix this feature', 'modify feature'."
disable-model-invocation: false
version: 0.1.0
audiences: [kmp-engineer, design-engineer]
mode-default: review
requires:
  - shared/kmp-patterns.md
  - shared/kmp-agent-base.md
  - shared/kmp-state.md
  - policies/approval-checkpoints.md
---

# kmp-modify-feature — spec delta first, code second

Changes an existing feature without letting the spec and the code drift apart. The
spec is OpenSpec's; this skill proposes a **delta**, waits for approval, then
implements it and reconciles the spec.

Where `kmp-create-feature` builds a module from nothing, this skill changes one
that already exists — so the read-only discovery phase and the approval gate matter
more, not less.

## The One Rule That Matters Most

**Never edit before the spec delta is approved.** Read the spec, draft the change in
diff form, get an explicit OK, and only then touch `feature/**`. A modification that
starts in the editor instead of the spec is how a codebase stops matching its spec.

## Feature-file guard

Feature sources are protected. Create the marker before editing, remove it on any
exit, success or failure:

```bash
touch /tmp/.kmp-skill-active
rm -f /tmp/.kmp-skill-active
```

`.opencode/plugins/protect-feature.ts` blocks Edit/Write under `feature/` while the
marker is absent; `opencode.json` adds a backstop `ask`.

## The Token-Aware Brief Contract

Restate: **Context** (feature, current spec version) / **Objective** (the single
change) / **Inputs** (the spec's relevant sections, the Penpot `DESIGN.md` if the
change is visual) / **Constraints** (inviolable architecture rules, components that
must not change) / **Acceptance criteria** (approved delta, build green, checker
green, spec updated).

## Mandatory Workflow

`references/workflow.md` is authoritative; in outline:

- **Phase 0 — parse + locate (read-only).** Extract the feature name; confirm
  `feature/{name}/` exists. **Exit:** the feature is identified.
- **Phase 1 — spec check (read-only).** Read
  `openspec/specs/{featurename}/spec.md`. If there is no spec, **stop** and tell the
  user to create one with `/opsx-propose` — do not infer requirements. **Exit:** the
  current spec is loaded.
- **Phase 2 — input resolution.** If the change is visual, confirm a Penpot
  `DESIGN.md` exists for the capability; otherwise note that it is design-independent.
  Read the Platform Profile field (`network` / `platform-capability` / `native-view` /
  `mixed`). **Exit:** inputs known.
- **Phase 3 — understand + plan.** Read the affected layers; load only the matching
  references. Classify the Platform Profile change if any. **Exit:** an affected-layer
  list.
- **Phase 4 — draft the spec delta.** Propose the change in diff form against the
  living spec. **✋ Checkpoint — never skip:** Approve / Modify / Reject.
- **Phase 5 — implement (marker on).** Apply the approved delta layer by layer.
  **✋ Checkpoint** after each layer.
- **Phase 6 — validate + reconcile (marker off).** Build, format, run the checker,
  update the spec with the approved change, bump its version, archive via
  `/opsx-archive` when the change is complete.

## Critical Rules

1. **Spec delta approved before any edit.** No exceptions.
2. **No spec → stop.** Route the user to `/opsx-propose`; never infer requirements.
3. **A feature never depends on another feature.** Shared code goes to `core/`.
4. **`Either<T>` for fallible operations**; never throw across a layer boundary.
5. **Reuse the design system** — UI changes activate `kmp-using-design-system`.
6. **New platform capability changes the Platform Profile** and routes through
   `references/architecture/platform.md`.
7. **A new component needs a `@Preview`** in the same file.
8. **Update the spec, don't regenerate it.** Apply the approved delta and add a
   changelog entry; bump patch for fixes, minor for features.

## Domain Architecture

Unchanged from create: `feature/{name}/…/{data,presentation,di}` with the four
integration points already wired. This skill adds to an existing shape — it never
re-scaffolds the module or renames its packages.

## Modes & Policies

`mode-default: review`. The spec delta and each layer are shown at a checkpoint
before continuing. Nothing is auto-applied. See
`policies/approval-checkpoints.md`.

## State Management

Extend the capability's existing entry in `.kmp/run.json` with the modification
phase and the spec delta status. Re-derive with
`python3 shared/scripts/kmp_check.py {name}` before resuming. See
`shared/kmp-state.md`.

## User Checkpoints

| After phase | Artifacts shown | What we ask |
|---|---|---|
| 4 | the spec delta in diff form | Approve / Modify / Reject |
| 5 | changed files + build + checker output | approve the layer before the next |

## Naming Conventions

Package names stay lowercase and unchanged. Follow
`shared/kmp-patterns.md`; never rename packages or files from this skill — that is a
separate, deliberate refactor.

## Anti-Rationalization Table

| Excuse the model makes | Why it's wrong | Countermeasure that halts the flow |
|---|---|---|
| "The change is small; I'll skip the spec delta and edit directly." | Unrecorded changes are exactly how spec and code drift. The delta is the point. | Stop. Draft the delta and get approval first. |
| "There's no spec, but I can reconstruct one from the code." | Reconstructing requirements invents intent; the user must own the spec. | Stop. Route to `/opsx-propose`. |
| "I'll regenerate the whole spec so it's clean." | Regeneration discards the approved history and changelog. | Apply the approved delta; append a changelog entry; bump the version. |
| "I'll just import the helper from the other feature." | Features never depend on features; that's what `core/` is for. | Hoist the shared code into the correct `core/` tier. |
| "The checker is failing on old code; I'll ignore it." | The gate is what keeps the architecture from drifting. | Use `--baseline` to separate pre-existing from new; fix the new, report the rest. |

## Helper Code Snippets

```bash
touch /tmp/.kmp-skill-active
./gradlew :feature:{NAME}:assembleAndroidMain
./gradlew :feature:{NAME}:ktlintFormat
python3 shared/scripts/kmp_check.py {NAME}
rm -f /tmp/.kmp-skill-active
```

## Reference Resources

- `references/workflow.md` — the full modify workflow (ported and re-homed).
- `shared/kmp-patterns.md` — the 14 rules.
- `skills/kmp-create-feature/references/architecture/` — layer deep dives.
- `skills/kmp-using-design-system/SKILL.md` — the UI reuse guard.

## Supporting Files

| File | Use |
|---|---|
| `references/workflow.md` | the end-to-end modify workflow — read before Phase 3 |

**Doctrine paths.** `shared/…` and `policies/…` resolve inside this bundle in native installs (vendored by the installer); in an opencode install they live at the kit root, two directories up from this file (`skills/<name>/SKILL.md`).
