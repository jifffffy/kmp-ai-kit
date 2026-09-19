---
name: kmp-domain-model
description: "Model a capability's domain with Coad's Color Modeling before it is built: identify the Moment-Intervals first, then Roles, Parties/Places/Things and Descriptions, then attributes and links — and map each archetype onto this kit's Clean Architecture. Writes openspec/changes/<id>/domain.md. Triggers: 'model the domain', 'what are the entities', 'domain model', '领域建模', 'what should the data model be', 'add a domain model'."
disable-model-invocation: false
version: 0.1.0
audiences: [kmp-engineer, design-engineer, product-designer]
mode-default: review
requires:
  - shared/domain-modeling.md
  - shared/kmp-patterns.md
  - shared/kmp-state.md
  - policies/approval-checkpoints.md
---

# kmp-domain-model — the layer between spec and build

The kit's spec says **what the system must do**. Its `DESIGN.md` says **what it looks like**. The
build skills say **how it ships**. None of them says **what the domain is** — the concepts, their
relationships, and which of their attributes are stored versus derived. That gap is why a request
like "add a leaderboard" can be perfectly architected and still model the wrong thing.

This skill fills it, using **Peter Coad's Color Modeling** (`shared/domain-modeling.md` is
authoritative). Its output is one artifact: `openspec/changes/<change-id>/domain.md`.

## The One Rule That Matters Most

**Moment-Intervals first.** Ask *"what happens in this capability?"* before *"what are the
things?"*. Every other archetype is derived from that list. Starting from entities — which is what
a spec's incidental nouns invite — produces a model that does not survive the UI.

## Tool surface

No MCP, no code. This skill reads the OpenSpec spec and writes one markdown file. The routing
verdict comes from `shared/scripts/kmp_route.py` (`.kmp/route.json`); the architecture rules it must
respect are `shared/kmp-patterns.md`.

## The Token-Aware Brief Contract

Restate before modeling: **Context** (capability, the change it belongs to) / **Objective** (one
domain model) / **Inputs** (the spec's requirements and scenarios) / **Constraints** (Rule 9: no
UseCase layer; the four archetypes map onto repository methods, DTOs, enums and ViewModel actions) /
**Acceptance criteria** (every Moment-Interval has participants and a persisted/not verdict; every
derived attribute is listed as derived; the mapping table has no empty KMP home).

## Mandatory Workflow

`shared/domain-modeling.md` §2 is the procedure. In outline:

- **Phase 0 — read (read-only).** Locate the spec via `.kmp/route.json` (`artifacts.spec`) or the
  change directory. Read its requirements and scenarios. If no spec exists, **stop** — the router
  orders spec before domain, and there is nothing to model from. **Exit:** the spec is loaded.
- **Phase 1 — Moment-Intervals.** Read the scenarios as *events*: what happens, what starts it, who
  participates and how many. List the MIs. **✋ Checkpoint** — this list is the model's spine; get it
  agreed before anything else is derived.
- **Phase 2 — Roles → Parties/Places/Things → Descriptions.** Derive, in that order, each from the
  previous. **✋ Checkpoint.**
- **Phase 3 — Attributes, then Links.** Only now add fields. For each attribute decide stored vs
  **derived**, and compute each link's multiplicity and togetherness.
- **Phase 4 — Map onto the kit.** Fill the mapping table; then verify it against the code
  constraints in `shared/kmp-patterns.md` (Rule 9, `Either<T>`, the `data/model/` DTO boundary).
  **✋ Checkpoint** on the mapping — it is what the build will actually consume.
- **Phase 5 — Write `domain.md`.** To `openspec/changes/<change-id>/domain.md`, using the template
  in `shared/domain-modeling.md` §4. Then re-run the router so `.kmp/route.json` sees it:
  `python3 shared/scripts/kmp_route.py --capability <slug>`.

## Critical Rules

1. **MIs before entities.** The ordering is the method; skipping to attributes is what it prevents.
2. **A Moment-Interval is never a UseCase class.** Rule 9 forbids that layer — it becomes a
   Repository method plus a ViewModel action, and a DTO only if the user can see or re-open it.
3. **Mark every derived attribute.** A count is usually a sum over MIs; a rank is a position in a
   sort. Persisting either creates a value that goes stale. This is the single highest-value output.
4. **A Role is not a new entity.** If a "new" concept shares most of its attributes with another,
   it is a Role — model it as a field or nested object, not a parallel `@Serializable` class.
5. **The domain model is not a second spec** and not a design artifact. It never restates
   requirements and never specifies visuals. If the spec and the model disagree, that is a finding
   for the user, not a silent edit to either.
6. **Never invent requirements to fill a table.** A gap is recorded under *Open questions* with the
   default taken, not resolved by invention.
7. **Record `Domain model: none` explicitly** when a capability genuinely has no domain concepts
   (a copy change, a styling tweak). Silence is indistinguishable from forgetting.
8. **This skill writes exactly one file** — `domain.md`. It never edits `feature/**`, the spec, or
   the design.

## Domain Architecture

```
openspec/changes/<change-id>/
├── proposal.md            planning   (OpenSpec)
├── specs/<cap>/spec.md    planning   (OpenSpec)   ← read by this skill
├── domain.md              planning   (this skill)  ← the layer between
├── design.md              planning   (OpenSpec)
└── tasks.md               planning   (OpenSpec)
        │
        └─→ consumed by: the build skills (DTO shapes, repository operations, derived fields)
            and by Penpot when a Description drives visible states (tiers, windows)
```

## Modes & Policies

`mode-default: review`. The MI list and the mapping table are each shown before proceeding; nothing
is written until Phase 5, and the user approves the model before the build consumes it. See
`policies/approval-checkpoints.md`.

## State Management

Record the domain verdict in the capability's `.kmp/run.json` entry (`domain: <path> | none`), and
re-run `kmp_route.py` after writing so `.kmp/route.json` reflects it. See `shared/kmp-state.md`.

## User Checkpoints

| After phase | Artifacts shown | What we ask |
|---|---|---|
| 1 | the Moment-Interval list with participants | approve the spine before deriving |
| 2 | Roles / Parties / Descriptions | approve the concept set |
| 4 | the archetype → KMP mapping table + derived attributes | approve what the build will consume |

## Naming Conventions

Concept names are domain words, not table names (`Contribution`, not `ContribRepoRow`). DTOs follow
the kit: `{Concept}Response`. A Moment-Interval maps to a verb-named repository method
(`getRecentContributions()`), not a `DoSomethingUseCase`.

## Anti-Rationalization Table

| Excuse the model makes | Why it's wrong | Countermeasure that halts the flow |
|---|---|---|
| "The spec already lists the entities, so modeling is redundant." | A spec lists behaviours; entities fall out incidentally and the events are missing entirely. | Run the six steps; produce the MI list the spec does not contain. |
| "I'll model from the screens after the UI exists." | Screens are one projection; modeling from them bakes UI accidents into the data layer. | MIs first, before any DTO is named. |
| "This MI needs its own class — a UseCase." | Rule 9 forbids the UseCase layer. | Repository method + ViewModel action; a DTO only if it is user-visible. |
| "`rank`/`count` looks like a field, I'll put it on the DTO." | Deriving it in the model is what stops the value going stale. | List it under **Derived, not stored** with its derivation, and decide in the mapping where it is computed. |
| "`Contributor` is a new entity, I'll add `ContributorResponse`." | It is a Role on `Account`; a parallel class duplicates every attribute and drifts. | Model the Role as a field/nested object on the party. |
| "No spec yet, but I can model the domain from the request." | Requirements are OpenSpec's; modeling from a request silently invents them. | Stop. The router orders spec first — route to `/opsx-propose`. |
| "It's a simple change, I'll skip the model." | "Simple" is where the second-entity-that-was-a-role and the count-that-should-be-aggregated appear. | Write the model, or record `Domain model: none` with one sentence why. |

## Helper Code Snippets

```bash
# what the model must respect
python3 shared/scripts/kmp_check.py --baseline        # architecture baseline

# where the spec is, and whether the domain model exists yet
python3 shared/scripts/kmp_route.py --capability {slug} --json-only | python3 -m json.tool

# after writing domain.md — confirm the router now sees it
python3 shared/scripts/kmp_route.py --capability {slug}
```

## Reference Resources

- `shared/domain-modeling.md` — the method, the template, and the worked leaderboard example.
- `shared/kmp-patterns.md` — Rule 9 (no UseCase layer), the DI/DTO conventions the mapping targets.
- `skills/kmp-create-feature/references/architecture/data.md` — where the DTOs land.

## Supporting Files

No `references/` or `scripts/`: the doctrine and the template both live in
`shared/domain-modeling.md`, which is the single source of truth.

**Doctrine paths.** `shared/…` and `policies/…` resolve inside this bundle in native installs (vendored by the installer); in an opencode install they live at the kit root, two directories up from this file (`skills/<name>/SKILL.md`).
