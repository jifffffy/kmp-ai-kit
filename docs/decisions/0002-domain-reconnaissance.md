# 0002 — Reconnaissance (FDD steps 1–2) is an optional skill, not a flag

**Status:** accepted
**Date:** 2026-09-19

## Context

The kit could model a domain, but only *after* a spec existed: `kmp-domain-model` requires the change's
scenarios, because Coad's step 1 is "read the scenarios as events". That left a gap at the very
beginning of a project —

**a project's first change named its concepts by accident.** The proposal is written from a
one-paragraph request, and whatever words it happens to use become the living vocabulary that every
later change inherits and reconciles against. If the first proposal says `User` where the domain means
`Account`, the project carries `User` indefinitely.

Coad's FDD does not have this gap: it starts with *Develop an Overall Model* and *Build a Feature
List*, both before any feature is selected. The kit was running FDD steps 3+ only.

The proposal was to add those two steps, triggered by a command-line argument (`--init`).

## Decision

Add them as **an ordinary skill**, `kmp-domain-recon`, and keep two boundaries.

**1. A skill, not a flag.** Skills trigger on intent — there is no argument surface in the agent
runtime, and a flag on `kmp-domain-model` would blur two very different entry points (one consumes a
spec, the other interviews the user). Recon is a separate skill sharing the same doctrine.

**2. Two granularities, selectable at the top.** FDD's *feature* is a few hours of work
(`<action> the <result> <object>`); this kit's feature is a **module** — data + presentation + DI +
four integration points + a spec capability, i.e. a change. Emitting an FDD-granular list would give
the user something they cannot act on: too small to be a change, and treating one as a change yields a
module with a single method. So reconnaissance emits **Features** (selectable, one change each) with
FDD-granular **Functions** nested beneath as raw material for that change's `tasks.md`.

**3. Hypotheses, never requirements — and the skip is recorded.** FDD step 1 assumes domain experts in
a room; an agent has only what the user says. Where the user is vague, the output records an **open
question with the assumption taken** rather than inventing a concept. Recon output carries no
SHALL/MUST, is expected to be wrong in places, and is reconciled by the first change's C0 — which is
where a hypothesis becomes a decision. It is optional; a single-purpose project skips it, and the skip
is recorded the same way a concept-free capability records `Domain model: none`.

**4. The candidate list is one-time and unmaintained.** After a feature is selected,
`openspec/changes/` and `openspec/specs/` are the only truth. A second backlog beside OpenSpec is
exactly the duplicate-source problem this kit removes elsewhere.

## Consequences

- The cold-start chain is complete: `kmp-ai-kit new` → (optional) `kmp-domain-recon` → user selects a
  feature → `/opsx-propose` → `specs` → `domain` (C0 reconciles against recon) → `tasks` → build.
- Three gating checkpoints (R1 the frame, R2 areas + concepts, R3 the list), because reconnaissance is
  entirely inference and each layer builds on the last.
- `openspec/domain/features.md` is the one artifact that is deliberately *not* maintained. That is
  stated in the file itself, in the doctrine, and in `config.yaml`'s context so the proposal step does
  not treat its entries as commitments.
- `kmp_route.py` reports it and surfaces an optional (never blocking) gap on a project with no changes
  and no vocabulary.
- Cost accepted: for a single-purpose app, recon is ceremony. Mitigated by being optional with a
  recorded skip.

## Alternatives considered

- **A `--init` flag on `kmp-domain-model`.** Rejected: no argument surface exists, and one skill with
  two input modes makes the trigger description ambiguous — the setup where a skill is silently not
  invoked.
- **FDD-granular output, as the book specifies.** Rejected on granularity: it does not map to this
  kit's unit of work, and the user would select something `kmp-create-feature` cannot consume.
- **Make recon mandatory on every new project.** Rejected: its value scales with domain complexity. A
  multi-area commercial app is where FDD earns its keep; a one-screen utility does not need a
  workshop. Mandatory ceremony is how a step gets skipped *without* being recorded.
- **Store recon output in the living model only, without `features.md`.** Rejected: they have
  different lifespans. The vocabulary is stable and progressively corrected; the candidate list is
  consumed once and then historical. Mixing them invites editing the vocabulary to tick off work.
- **Have recon write a spec.** Rejected: it is the strongest form of inventing requirements, and
  requirements are OpenSpec's.
