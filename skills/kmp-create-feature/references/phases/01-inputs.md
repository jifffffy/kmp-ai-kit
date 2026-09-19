# Phase 1: Input Resolution

**Purpose:** Resolve this build's three inputs — the **requirements** (OpenSpec), the **domain
model** (Coad color modeling), and the **design** (Penpot) — before any contract is written or any
code is touched. Read-only.

**When:** Immediately after Phase 0 context discovery.

**Exit:** each input's path is recorded in the run ledger, or the run stops with the missing input
named.

---

## Checklist

```
Input Resolution Progress:
- [ ] Step 1.1: Locate the OpenSpec change and its spec delta
- [ ] Step 1.2: Confirm the capability is create, not modify
- [ ] Step 1.3: Confirm the domain model exists (or is explicitly `none`)
- [ ] Step 1.4: Locate the Penpot handoff artifact, if the work is visual
- [ ] Step 1.5: Record inputs in `.kmp/run.json`
- [ ] Step 1.6: Assert the inputs exist (stop if they do not)
```

**Shortcut:** run the router's computation and read the verdict instead of re-deriving any of this:

```bash
python3 shared/scripts/kmp_route.py --capability {featurename} [--ui]
# → .kmp/route.json: artifacts.{spec,domain,design}, gaps[], next
```

---

## Step 1.1: Locate the OpenSpec change

```bash
openspec list                      # active changes
openspec show <change-id>          # proposal + spec delta
```

The change directory is `openspec/changes/<change-id>/`. Read:

| File | What it gives |
|---|---|
| `proposal.md` | Context — product, audience, why now |
| `specs/<capability>/spec.md` | the Requirement + Scenario delta |
| `tasks.md` | the implementation checklist (consumed in Phase 3) |

The living spec (once archived) is `openspec/specs/<capability>/spec.md`. **There is
no second spec copy in the code tree** — never write one.

---

## Step 1.2: Confirm create, not modify

```
Glob: feature/{featurename}/**
```

- No module → this is a **create**. Continue.
- Module already exists → **stop.** This is a modify; route the user to
  `/kmp-modify-feature`. Do not scaffold over an existing module.

Also check `openspec/specs/{featurename}/spec.md`: an existing spec means the
capability is already owned, so the work is a modification.

---

## Step 1.3: Confirm the domain model

The domain model is the layer between the spec and the build — it decides what the concepts are,
which attributes are stored vs. derived, and which archetype each maps to in the feature. Without
it, the DTO shapes and derived fields get guessed during implementation.

Look for `openspec/changes/<change-id>/domain.md` (the router reports it as `artifacts.domain`).

| Situation | Action |
|---|---|
| `domain.md` exists | read it; it drives the DTO shapes and the repository operations in Phase 4 |
| missing, and the capability has domain concepts | **stop** — run `/kmp-domain-model <capability>`, then re-run the router |
| missing, and the capability is genuinely concept-free | require an explicit `Domain model: none` with a one-sentence reason, written by `kmp-domain-model`; then continue |

**Do not model the domain here.** This phase consumes the model; `kmp-domain-model` produces it.
Inferring entity shapes mid-implementation is exactly what that skill exists to prevent.

---

## Step 1.4: Locate the Penpot handoff (visual work only)

The design layer owns design. Its output is:

- `DESIGN.md` — produced by the `penpot-design-md` skill (tokens, type scale, component
  inventory).
- the annotation layer — produced by the `penpot-document-handoff` skill (context card,
  numbered pins, notes).

If `DESIGN.md` does not exist and the feature has a UI, **stop and say so**: the design
layer runs first (`/penpot-build-screen` → `/penpot-design-md`). Do not invent a design
or hand-transcribe one from a screenshot.

If the feature is genuinely UI-less (a pure data/platform change), record
`design: none` and continue.

---

## Step 1.5: Record the inputs

Write to the capability's entry in `.kmp/run.json`:

```json
{
  "capability": "{featurename}",
  "change_id": "{change-id}",
  "spec_path": "openspec/specs/{featurename}/spec.md",
  "change_path": "openspec/changes/{change-id}/",
  "domain_path": "openspec/changes/{change-id}/domain.md",
  "design_path": "{DESIGN.md path, or null}",
  "phase": 1
}
```

Schema and rules: `shared/kmp-state.md`.

---

## Step 1.6: Assert before proceeding

Every input must resolve:

- a spec delta under `openspec/changes/<change-id>/specs/`,
- a `domain.md` (or a recorded `Domain model: none`), and
- either a `DESIGN.md` or an explicit `design: none`.

If one is missing, do not continue to Phase 2. Name the missing input and the command that produces
it — `.kmp/route.json` already lists it under `gaps`, with `blocking`.

---

## Output

- The change and its spec delta are located.
- The domain model is located and read.
- The Penpot handoff is located, or design is explicitly out of scope.
- The inputs are recorded in `.kmp/run.json`.

Proceed to **Phase 2: Contract + Plan**.
