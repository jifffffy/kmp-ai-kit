---
name: kmp-domain-recon
description: "Reconnaissance for a brand-new project: run Coad's FDD steps 1–2 (develop an overall model, build a feature list) once, before any change exists, so the project's domain vocabulary is chosen deliberately instead of appearing by accident in its first proposal. Interviews the user, splits the domain into areas, and writes openspec/domain/model.md plus openspec/domain/features.md for the user to pick from. Triggers: 'recon the domain', '新项目领域侦察', 'what should this app do', 'break the app into features', 'where do I start on a new project', 'overall domain model'."
disable-model-invocation: false
version: 0.1.0
audiences: [product-designer, kmp-engineer, design-engineer]
mode-default: review
requires:
  - shared/domain-modeling.md
  - shared/kmp-state.md
  - policies/approval-checkpoints.md
---

# kmp-domain-recon — FDD steps 1–2, once, on a new project

The kit could already model a domain, but only *after* a spec existed — so a project's first
proposal named its concepts by accident, and the living model inherited those accident-names
forever. `.opencode/commands/opsx-propose` cannot help: there is nothing to propose from yet.

This skill runs **before any change**: Coad's FDD step 1 (*Develop an Overall Model*) and step 2
(*Build a Feature List*). Its output is a coarse model and a candidate list the user chooses from,
and it is explicitly **hypotheses, not requirements** — see `shared/domain-modeling.md` §9.

## The One Rule That Matters Most

**Reconnaissance produces hypotheses, never requirements.** FDD's step 1 assumes domain experts in a
room; an agent has only what the user says. Where the user is vague, record an **open question with
the assumption taken** — do not invent. Everything written here is expected to be wrong in places,
and the first real change's `domain` artifact (C0) corrects it.

## Tool surface

No MCP, no code, no specs (there are none yet). Two files out:
`openspec/domain/model.md` (the coarse living vocabulary) and `openspec/domain/features.md` (the
candidate list). The method is `shared/domain-modeling.md`; the routing verdict is
`.kmp/route.json` from `shared/scripts/kmp_route.py`.

## The Token-Aware Brief Contract

Restate before starting: **Context** (the product, in the user's words) / **Objective** (one
reconnaissance, not a plan) / **Inputs** (the interview — there is no spec) / **Constraints** (no
SHALL/MUST; no architecture; no UI; no task breakdown) / **Acceptance criteria** (every feature in
the list is selectable as one change; every concept has an archetype; every gap is an open question).

## Granularity — read this before producing anything

FDD's feature is hours of work. **This kit's feature is a module** (data + presentation + DI + four
integration points + a spec capability) — a change, not a task. Reconnaissance therefore emits two
levels, and the user selects the upper one:

| Level | Unit | Chosen as | Becomes |
|---|---|---|---|
| upper | **Feature** — an area of the domain | **yes** | one `/opsx-propose` change → one `feature/` module |
| lower | **Function** — `<action> the <result> <object>` | no | raw material for that change's `tasks.md` |

A list at the wrong level is unusable: FDD-granular items are too small to be a change; treating one
as a change yields a module with a single method.

## Mandatory Workflow

`shared/domain-modeling.md` §9 is the procedure. Three checkpoints, because reconnaissance is all
inference and each layer builds on the last.

```
Phase 0  frame the product         ✋ R1   what are we even in?
   ▼
Phase 1  develop an overall model  ✋ R2   areas + key concepts + archetypes
   ▼
Phase 2  build the feature list    ✋ R3   selectable features, ordered
   ▼
Phase 3  write both files          (writes only what R3 approved)
```

- **Phase 0 — frame (interview).** Establish the subject area in the domain's own words: what the
  product is for, who uses it, what the user already knows about the nouns. Ask; do not infer from
  the app name. **✋ R1** — show the frame; the user corrects the words before anything is modeled.
- **Phase 1 — overall model.** Split the domain into **areas**, and for each, the key concepts with
  their Coad archetype (`shared/domain-modeling.md` §1). Coarse on purpose: concepts and roles, not
  attributes. **✋ R2.**
- **Phase 2 — feature list.** For each area, a **feature** (selectable) and, nested under it, its
  candidate **functions** in FDD form. Order by dependency and value. **✋ R3.**
- **Phase 3 — write.** `openspec/domain/model.md` and `openspec/domain/features.md` from the
  approved result, then `python3 shared/scripts/kmp_route.py` so `.kmp/route.json` reflects them.

## What reconnaissance must not do

| It must not | Because |
|---|---|
| emit SHALL/MUST, or accept/reject criteria | it is not a spec; requirements come only from a spec |
| pick architecture, modules, DTOs, or screens | recon precedes design and build; that is the change's job |
| produce a `tasks.md` or a schedule | FDD step 2 is a **feature** list, not a plan; tasks belong to a change |
| keep the list updated afterwards | after selection, `openspec/changes/` and `openspec/specs/` are the only truth |
| resolve a vague answer by guessing | record an open question with the assumption taken |

## Critical Rules

1. **Interview, don't infer.** The input is the user, not the app name and not the repo.
2. **Two granularities, selectable at the top.** A feature is a change; a function is a task.
3. **Non-normative output.** No SHALL/MUST anywhere. `openspec validate` never sees these files.
4. **It is optional, and the skip is recorded.** A single-purpose project skips it — with an explicit
   note, the same way a concept-free capability records `Domain model: none`.
5. **It runs once.** Not per change; the per-change model is `kmp-domain-model`, whose C0 reconciles
   against what this wrote.
6. **It writes exactly two files**, both under `openspec/domain/`. It never writes a spec, a change,
   or code.
7. **Open questions are output, not failure.** A recon with no open questions on a vague brief means
   the vagueness was papered over.

## Domain Architecture

```
openspec/
├── domain/
│   ├── model.md      ← coarse living vocabulary   (this skill creates it)
│   └── features.md   ← candidate list             (this skill creates it)
├── specs/            ← empty until the first change is archived
└── changes/          ← empty until the first /opsx-propose
```

`model.md` is later *extended* by each change's `domain` artifact (C0 reconciles against it), and is
the vocabulary the proposal step reads.

## Modes & Policies

`mode-default: review`. Three checkpoints, all gating: nothing is written until R3 approves. See
`policies/approval-checkpoints.md`.

## State Management

Record the recon in `.kmp/run.json` (`recon: done | skipped`, with the two paths). No per-change
ledger — reconnaissance is not a change. See `shared/kmp-state.md`.

## User Checkpoints

| # | After phase | Artifacts shown | What we ask |
|---|---|---|---|
| **R1** | 0 | the frame: subject area, users, words the user already uses | is this the right domain, in your words? |
| **R2** | 1 | the areas with their key concepts and archetypes | are these the right areas, and the right kinds of concept? |
| **R3** | 2 | the feature list: features (selectable) with functions nested | which do you want first, and is the level right? |

## Naming Conventions

Concepts are domain words, not table names (`Contribution`, not `ContribRepoRow`). A feature is
named for the capability (`leaderboard`, `checkout`) — it becomes the `feature/<name>` module and the
spec capability path. A function follows FDD's template exactly: `<action> the <result> <object>`
("Calculate the total of a sale"), which is what makes it usable as a task later.

## Anti-Rationalization Table

| Excuse the model makes | Why it's wrong | Countermeasure that halts the flow |
|---|---|---|
| "The app name tells me the domain — I'll skip the interview." | The name gives the subject area at best; the concepts are the user's, and a wrong frame propagates into every later change. | Run R1 and show the frame; ask. |
| "I'll make the list FDD-granular so it's thorough." | FDD features are hours; a kit feature is a module. A list at the wrong level cannot be acted on. | Two levels, selectable at the top. |
| "The brief is vague; I'll assume the obvious concepts." | Recon exists to surface the vagueness, not to bury it. A confident wrong model is undone by the first change. | Record an open question with the assumption taken. |
| "I'll write acceptance criteria so the list is actionable." | That is a spec, and requirements belong to OpenSpec. It also makes hypotheses look authoritative. | No SHALL/MUST; R3 asks the user to choose, not to sign off on behaviour. |
| "I'll also sketch the modules and screens while I'm here." | Recon precedes design and build; guessing architecture here bypasses both. | Stops at concepts and the candidate list. |
| "I'll keep features.md updated as work completes." | A second backlog beside OpenSpec drifts and competes with `changes/` as the truth. | State in `features.md` that it is a one-time artifact; after selection it is historical. |

## Helper Code Snippets

```bash
# is recon needed? (a new project has no change and no living model)
python3 shared/scripts/kmp_route.py --json-only | python3 -m json.tool

# after writing, confirm the router sees the living model
python3 shared/scripts/kmp_route.py --capability <first-feature>
```

## Reference Resources

- `shared/domain-modeling.md` §9 — reconnaissance, the granularity rule, and the boundary.
- `shared/domain-modeling.md` §1–2 — the archetypes and the ordering, which Phase 1 uses coarsely.
- `skills/kmp-domain-model/SKILL.md` — the per-change model whose C0 reconciles against recon.
- `shared/domain-modeling.md` §9 — the `features.md` template and the two-level rule.

## Supporting Files

No `references/` tree: the method lives in `shared/domain-modeling.md`, which is the single source of
truth for both this skill and `kmp-domain-model`.

**Doctrine paths.** `shared/…` and `policies/…` resolve inside this bundle in native installs (vendored by the installer); in an opencode install they live at the kit root, two directories up from this file (`skills/<name>/SKILL.md`).
