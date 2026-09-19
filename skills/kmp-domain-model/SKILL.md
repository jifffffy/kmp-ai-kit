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

**Who owns what — the artifact is OpenSpec's, the method is this skill's.**

| Concern | Owner |
|---|---|
| that `domain` exists as an artifact, its path (`domain.md`), its place in the order (`requires: specs`), and that `tasks` requires it | the **`kmp` OpenSpec schema** (`openspec/schemas/kmp/schema.yaml`) |
| the four archetypes, the six ordered steps, what goes in each section, the checkpoints | **this skill** + `shared/domain-modeling.md` |

So the order is not a convention the model must remember — OpenSpec blocks `domain` until `specs`
completes and blocks `tasks` until `domain` does. Start by reading the schema's own guidance for the
artifact, which carries the required-section list:

```bash
openspec instructions domain --change <change-id>
```

Do not restate the order in your output; run the commands and let the tooling enforce it.

## The Token-Aware Brief Contract

Restate before modeling: **Context** (capability, the change it belongs to) / **Objective** (one
domain model) / **Inputs** (the spec's requirements and scenarios) / **Constraints** (Rule 9: no
UseCase layer; the four archetypes map onto repository methods, DTOs, enums and ViewModel actions) /
**Acceptance criteria** (every Moment-Interval has participants and a persisted/not verdict; every
derived attribute is listed as derived; the mapping table has no empty KMP home).

## Mandatory Workflow

`shared/domain-modeling.md` §2 is the procedure. **Four checkpoints (C1–C4), one per irreversible
decision.** A model is built by deciding, and each of these four decisions is expensive to reverse
once code exists — so each is shown and approved on its own.

```
Phase 0  read (no checkpoint — nothing is decided)
   ▼
Phase 1  Moment-Intervals        ✋ C1   what happens
   ▼
Phase 2  Roles/PPT/Descriptions  ✋ C2   what participates
   ▼
Phase 3  Attributes + Links      ✋ C3   what it knows, and how it relates
   ▼
Phase 4  Map onto the kit        ✋ C4   where it lands
   ▼
Phase 5  write domain.md         (no checkpoint — writes only what C4 approved)
```

- **Phase 0 — read (read-only).** Locate the spec via `.kmp/route.json` (`artifacts.spec`) or the
  change directory, and read the schema's own guidance for this artifact
  (`openspec instructions domain --change <id>`). Read the spec's requirements and scenarios. If no
  spec exists, **stop** — OpenSpec blocks `domain` until `specs` completes, and there is nothing to
  model from. **Exit:** the spec is loaded.
- **Phase 1 — Moment-Intervals.** Read the scenarios as *events*: what happens, what starts it, who
  participates and how many. **✋ C1.**
- **Phase 2 — Roles → Parties/Places/Things → Descriptions.** Derive, in that order, each from the
  previous. **✋ C2.**
- **Phase 3 — Attributes, then Links.** Only now add fields. For each attribute decide stored vs
  **derived**; for each link compute multiplicity and togetherness. **✋ C3.**
- **Phase 4 — Map onto the kit.** Fill the mapping table and check the model against the spec.
  **✋ C4.**
- **Phase 5 — Write `domain.md`.** To `openspec/changes/<change-id>/domain.md`, using the template
  in `shared/domain-modeling.md` §4. Then re-run the router so `.kmp/route.json` sees it:
  `python3 shared/scripts/kmp_route.py --capability <slug>`. **Writes exactly what C4 approved and
  nothing more** — no new concepts, no fields that appeared while writing.

## The checkpoint protocol

Every checkpoint follows `policies/approval-checkpoints.md` — **Evidence / Summary / The ask** —
filled in as follows. Never present a checkpoint without all three.

| | Evidence (show) | The ask (question) | If rejected |
|---|---|---|---|
| **C1** | the MI table: event · trigger · participants (multiplicity) · persisted? | "Is this the complete set of things that *happen*? Anything missing or misfiled?" | re-read the scenarios; a missing MI is usually an unread Scenario |
| **C2** | the three lists: Roles · Parties/Places/Things · Descriptions, each annotated with what derived it | "Is every concept the right archetype — and is anything *not* here that should be?" | return to Phase 2 only; do **not** restart at MIs — C1 is still approved |
| **C3** | the attributes table **with the stored/derived column**, and the links table with multiplicity + togetherness | "Are these the right *derived* calls? Do the multiplicities match how the UI behaves?" | return to Phase 3 only |
| **C4** | the archetype → KMP mapping table + the spec-coverage check (below) | "Approve what the build will consume?" | re-map; if a concept has no KMP home, the model is wrong, not the mapping |

**Never re-open an approved checkpoint silently.** If C3 reveals that an MI from C1 was wrong, say
so and re-run C1 — do not quietly edit the MI list and present C3 as if C1 still held.

## Self-review before every checkpoint (mandatory)

The analogue of the design layer's `visual-self-review.md`: run these four scans **before** showing
a checkpoint, and fix what you find. Presenting a model with a known defect wastes the user's review
on something you could have caught.

| Scan | Catches | Fix |
|---|---|---|
| Any MI with no participants, or one whose "trigger" is really a state change of a thing | an event confused with an attribute | re-read the scenario; MI or attribute, not both |
| Two concepts sharing most of their attributes | a **Role disguised as a Party** | collapse to a field/nested object on the shared party |
| Any numeric or positional attribute not marked derived (count, total, sum, average, percentage, rate, rank, position, index, balance) | a stored value that goes stale | move it to **Derived, not stored** with its derivation |
| A Description with exactly two values and no rules | over-modeling | make it a boolean/enum field on the party instead |

**Two self-fix iterations, then present with defects named** — same discipline as the design layer.
If a scan finds something you cannot resolve, present it as an open question at the checkpoint
rather than hiding it.

## Spec coverage — the gate inside C4

A domain model is only correct relative to the spec it came from. Before C4 may be approved, check
coverage mechanically:

1. **Every Requirement** maps to at least one MI or PPT. A requirement with nothing behind it means
   a concept is missing.
2. **Every Scenario's actor** is a Role. An actor that appears in scenarios but in no Role means a
   participant was dropped.
3. **Every failure-path Scenario** is representable — the model has somewhere for that failure to
   live (usually an `Either.Failure` on the MI's repository method). A model that can only express
   the happy path will not survive Phase 4 of the build.

Report the coverage as a table (`Requirement/Scenario → concept or MI → covered?`). **Uncovered is a
finding, not a footnote** — either the model gains the concept, or the spec loses the requirement,
and that is the user's call, not a silent edit to either.

## Critical Rules

1. **MIs before entities.** The ordering is the method; skipping to attributes is what it prevents.
2. **A Moment-Interval is never a UseCase class.** Rule 9 forbids that layer — it becomes a
   Repository method plus a ViewModel action, and a DTO only if the user can see or re-open it.
3. **Mark every derived attribute.** A count is usually a sum over MIs; a rank is a position in a
   sort. Persisting either creates a value that goes stale. This is the single highest-value output —
   which is why it has its own checkpoint (C3) and must never be folded into a casual mention.
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

`mode-default: review`. Four checkpoints (C1–C4) gate the four irreversible decisions; each shows
Evidence / Summary / The ask. Nothing is written until Phase 5, and Phase 5 writes only what C4
approved. See `policies/approval-checkpoints.md` and the checkpoint protocol above.

## State Management

Record the domain verdict in the capability's `.kmp/run.json` entry
(`domain: <path> | none`, plus `checkpoints_passed: [C1..C4]` so a resumed run does not re-ask what
was already approved), and re-run `kmp_route.py` after writing so `.kmp/route.json` reflects it. See
`shared/kmp-state.md`.

## User Checkpoints

| # | After phase | Artifacts shown | What we ask |
|---|---|---|---|
| **C1** | 1 | the Moment-Interval table (event · trigger · participants · persisted?) | is this everything that *happens*? |
| **C2** | 2 | Roles / Parties-Places-Things / Descriptions, each annotated with its derivation | is every concept the right archetype? |
| **C3** | 3 | attributes **with the stored/derived column** + links with multiplicity & togetherness | are the *derived* calls right, and do the multiplicities match the UI? |
| **C4** | 4 | the archetype → KMP mapping table + the spec-coverage table | approve what the build will consume? |

Two additional gates that are **not** user stops but must pass before the checkpoint is shown: the
four self-review scans, and (inside C4) the spec-coverage check.

## Naming Conventions

Concept names are domain words, not table names (`Contribution`, not `ContribRepoRow`). DTOs follow
the kit: `{Concept}Response`. A Moment-Interval maps to a verb-named repository method
(`getRecentContributions()`), not a `DoSomethingUseCase`.

## Anti-Rationalization Table

| Excuse the model makes | Why it's wrong | Countermeasure that halts the flow |
|---|---|---|
| "Attributes and links are mechanical — I'll fold them into the mapping checkpoint." | C3 holds the highest-value decision (derived vs stored), and Coad's step 6 (multiplicity/togetherness) is the one everyone skips. Folding them into C4 turns two decisions into a footnote. | Present C3 on its own, with the stored/derived column visible. |
| "The user approved C2, so I'll adjust the MI list silently and carry on." | A silently re-opened checkpoint means the remainder was built on an unapproved spine. | Say so and re-run C1. Never edit an approved checkpoint in place. |
| "The self-review found a Role-as-entity, but the user will probably spot it." | Spending the user's review on a defect you can name yourself is the waste the self-review exists to prevent. | Fix it (max two iterations), then present; name anything unresolved. |
| "Coverage is close — most Requirements map." | An uncovered Requirement is a missing concept, which is the exact failure this layer exists to catch. | Report coverage as a table; every gap is a finding the user decides on. |
| "I'll skip the checkpoint; the model is small and obviously right." | "Obviously right" is where the stored count and the duplicated entity come from. | Small models present faster, not never. C1–C4 are unconditional. |
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
