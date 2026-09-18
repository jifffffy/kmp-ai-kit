# Phase 1: Input Resolution

**Purpose:** Resolve this build's two inputs — the **requirements** (OpenSpec) and the
**design** (Penpot) — before any contract is written or any code is touched. Read-only.

**When:** Immediately after Phase 0 context discovery.

**Exit:** both input paths are recorded in the run ledger, or the run stops with the
missing input named.

---

## Checklist

```
Input Resolution Progress:
- [ ] Step 1.1: Locate the OpenSpec change and its spec delta
- [ ] Step 1.2: Confirm the capability is create, not modify
- [ ] Step 1.3: Locate the Penpot handoff artifact, if the work is visual
- [ ] Step 1.4: Record inputs in `.kmp/run.json`
- [ ] Step 1.5: Assert the inputs exist (stop if they do not)
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

## Step 1.3: Locate the Penpot handoff (visual work only)

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

## Step 1.4: Record the inputs

Write to the capability's entry in `.kmp/run.json`:

```json
{
  "capability": "{featurename}",
  "change_id": "{change-id}",
  "spec_path": "openspec/specs/{featurename}/spec.md",
  "change_path": "openspec/changes/{change-id}/",
  "design_path": "{DESIGN.md path, or null}",
  "phase": 1
}
```

Schema and rules: `shared/kmp-state.md`.

---

## Step 1.5: Assert before proceeding

Both inputs must exist as files:

- a spec delta under `openspec/changes/<change-id>/specs/`, and
- either a `DESIGN.md` or an explicit `design: none`.

If either is missing, do not continue to Phase 2. Name the missing input and the command
that produces it.

---

## Output

- The change and its spec delta are located.
- The Penpot handoff is located, or design is explicitly out of scope.
- The inputs are recorded in `.kmp/run.json`.

Proceed to **Phase 2: Contract + Plan**.
