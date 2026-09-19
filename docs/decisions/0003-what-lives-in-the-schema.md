# 0003 — The OpenSpec schema carries planning artifacts only; the build stays in skills

**Status:** accepted
**Date:** 2026-09-19

## Context

OpenSpec supports project-local workflow schemas natively (`openspec/schemas/`, with
`schema fork|init|which|validate`). Once that was known, the question followed: could the **build
layer** — `kmp-create-feature`, `kmp-test-feature`, `kmp-review-feature`, … — also be expressed as
schema artifacts, so OpenSpec drives everything?

The schema's expressiveness was checked against the tool's own types:

```ts
Artifact = { id, generates, description, template, instruction?, requires }
Apply    = { requires, tracks?, instruction? }
```

There is **no `verify`, `command`, or `run` field**, and the parser is `$strip` — an unknown key is
silently discarded rather than rejected. That was confirmed live: adding `verify:` and `run:` to an
artifact passes `openspec schema validate`, then never appears in any output. It is a trap worth
recording on its own: a schema can look like it runs something when nothing reads the field.

Completion is decided by **file existence** (`artifact-graph/state.js`: *"Detects which artifacts are
completed by checking file existence"*). There is no hook that runs a command and records its verdict.

## Decision

**Keep the split.** The schema carries the planning artifacts that have document semantics —
`proposal → specs → domain → design? → tasks`. The build layer stays in skills, executed by the agent,
gated by commands whose exit codes are the verdict.

The deciding principle is that **the two layers have different completion semantics**:

| | Completion is | Strength |
|---|---|---|
| OpenSpec artifact | the file exists | weak — correct for a document ("the proposal is written") |
| Build layer | a command passes (build, checker, app launch) | strong — required for code |

Moving the build into the schema would downgrade a **strong** verdict to a **weak** one. A feature
whose files all exist but which does not compile, fails the architecture checker, or crashes on launch
would report `[x] data-layer done`. That is precisely the failure class the runtime gate was added to
close (`shared/kmp-runtime-verification.md`: `archTest PASS ≠ the app works`), so the change would be a
regression, not a unification.

The bridge between the layers is `tasks.md` — an OpenSpec-owned artifact, executed by KMP skills.

## Consequences

- The boundary is honest about each side's guarantees. A green `openspec status` means the **planning
  documents** are complete; it never means the code works. That must be stated wherever the two are
  shown together (the README, `AGENTS.md`).
- The `domain` artifact is the correct kind of thing to be in the schema: it is a document, produced
  once per change, whose existence is its completion.
- Skills keep the capability that schema instructions do not have: they are loaded by the agent on
  intent, across projects. A schema instruction is only reachable via
  `openspec instructions <artifact>` for a change already in flight — it cannot be triggered from a
  standing request.
- `kmp-route.json` reads artifact states from `openspec status` rather than probing filenames, so the
  schema owns artifact ids, paths, and order; the kit hard-codes none of them.

## Alternatives considered

- **Move the build layers into the schema** (`data-layer`, `ui-layer`, `integration` artifacts).
  Rejected: it replaces command-verified completion with file existence, losing the gate that the
  runtime-verification work exists to provide.
- **Move skill instructions into schema `instruction` fields.** Rejected on two grounds: instructions
  are reachable only through `openspec instructions` for a change (losing intent-based triggering and
  cross-project reuse), and most skills — router, review, test, bridge, using-design-system — are not
  artifacts at all, so they have nowhere to go.
- **Add document-type artifacts for the build's outputs** (e.g. a review report). Feasible but thin:
  a re-run overwrites it, and under existence-based completion it would read `done` forever while going
  stale. Not adopted; revisit only if a specific need appears.
- **Record verification evidence as a `verification` artifact** (`generates: verification.md`,
  `requires: [tasks]`). A real gap exists here — after archiving, the build evidence in `.kmp/run.json`
  is gone (it is git-ignored), so an archived change cannot answer "was this verified?". Deferred on
  the same principle applied elsewhere in the kit: do not build for an assumed need. If it is added it
  must be an **evidence carrier**, never a **gate** — the gate is the command's exit code.
