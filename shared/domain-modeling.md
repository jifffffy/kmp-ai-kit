# Domain Modeling — Coad's Color Modeling, adapted to this kit

The kit has a layer for **what users need** (OpenSpec spec), **what it looks like** (Penpot
`DESIGN.md`), and **how it is built** (KMP skills). It was missing the layer between spec and
build: **what the domain actually is**. That gap is what this file closes.

The method is **Peter Coad's Color Modeling** (*Java Modeling in Color with UML*; the modeling
approach behind Feature-Driven Development). It is deliberately small: four archetypes, each
identified by one question, plus an ordering discipline that is the whole point.

> The architecture checker enforces **where** code goes. It cannot tell you whether you modeled
> the domain correctly. A feature can be perfectly layered and still model the wrong thing.

---

## 1. The four archetypes

| Archetype | Color | One question that identifies it | Examples |
|---|---|---|---|
| **Moment-Interval** | pink | *Does it happen at a moment in time, or over an interval?* | Sale, Payment, Booking, Reservation, Contribution, Login |
| **Role** | yellow | *How does something participate?* | Customer, Contributor, Owner, Reviewer |
| **Party / Place / Thing** | green | *Who, what, or where?* | Person, Account, Repository, Store, Product |
| **Description** | blue | *Is it a catalog-entry-like description of something?* | Tier, Category, Window, Plan, Size |

Two clarifications that prevent most mistakes:

- **A Role is a way of participating, not a separate person.** The same `Account` is a
  *Contributor* in one repository and an *Owner* in another. If you find yourself duplicating every
  attribute, you have a Role, not a new Party.
- **A Description is a *set of values*, not a classification someone assigns.** "Tier: Gold" is a
  Description when Gold is a catalogue entry with rules; it is a plain attribute when it is just
  one of two strings.

---

## 2. The ordering discipline (do not skip it)

Coad's process is ordered for a reason — starting anywhere else produces entity lists that do not
survive contact with the UI:

1. **Moment-Intervals first.** Ask *"what happens in this capability?"* before *"what are the
   things?"*. This is the discipline. Entities derived from events are the ones that actually
   appear in the UI.
2. **Roles.** For each MI, ask *who or what participates, and in what capacity?* — and **how many**
   (multiplicity).
3. **Parties / Places / Things.** The actors and objects the roles attach to.
4. **Descriptions.** The catalog-like value sets those things point at.
5. **Attributes.** Now — and only now — add fields, to the shape the first four steps produced.
6. **Links and multiplicity.** For every pair of related concepts, two questions:
   **how many** (1, 0..1, 1..*, 0..*) and **togetherness** (must they be created/destroyed
   together?).

Steps 1 and 6 are the ones teams skip, and they are the two that prevent rework.

### Where the checkpoints fall

The method has five decisions, and `kmp-domain-model` stops for approval at each — no model is
written until C4 passes, and nothing is written that C4 did not approve:

| Checkpoint | The decision | Method steps |
|---|---|---|
| **C0** | what *already exists* (reconcile against the living model) | §6 |
| **C1** | what *happens* (the spine) | 1 |
| **C2** | what *participates* (the concept set) | 2–4 |
| **C3** | what it *knows*, and how it *relates* | 5–6 |
| **C4** | where it *lands* in the code | the mapping table (§3) |

**C3 is the one most likely to be skipped and the one that matters most**: it holds both the
derived-vs-stored call and Coad's step 6. Folding it into C4 turns two decisions into a footnote.
Re-opening an approved checkpoint (editing the MI list after C1) is never silent — say so and re-run
C1.

Before each checkpoint the skill runs a **self-review** (four scans for the classic defects: an MI
with no participants, a Role disguised as a Party, an unmarked derived numeric/positional attribute,
a two-valued Description), and C4 additionally requires a **spec-coverage table** — every
Requirement behind a concept or MI, every Scenario's actor present as a Role. See
`skills/kmp-domain-model/SKILL.md`.

---

## 3. Mapping onto this kit's Clean Architecture

This mapping is what makes the method operational here rather than academic. It also encodes one
hard rule: **this kit has no UseCase layer (Rule 9).**

| Coad archetype | Where it lands in a feature |
|---|---|
| **Moment-Interval** | A **Repository method** (the operation) and the **ViewModel action** that invokes it. An MI that must be *stored* or *returned as a thing* becomes a `@Serializable` DTO in `data/model/` too. **Never a `UseCase` class** — Rule 9 forbids that layer. |
| **Role** | A nested DTO, an `enum class`, or a field on the party's DTO that is only meaningful in context (`role: ContributorRole`). |
| **Party / Place / Thing** | The main `@Serializable` DTOs in `feature/<name>/.../data/model/*Response.kt` — what `Either<…>` carries. |
| **Description** | `enum class` / constants / a small `{X}Description` DTO. Usually not from the network at all. |

### The three questions the mapping forces

Applying the mapping surfaces decisions the spec usually leaves ambiguous. Resolve them **in
`domain.md`**, before building:

1. **Is this a stored field or a derivation?** A count is often a *sum of Moment-Intervals*, not a
   column. A rank is a *position in a sort*, not an attribute. Marking the difference here decides
   whether it is a DTO field or a repository computation.
2. **Is this a new archetype or a Role?** A second `@Serializable` class that shares 90% of another
   one is a Role — model it as a field/nested object, not a parallel entity.
3. **Does a Moment-Interval need to exist as data?** If the user can see, list, or re-open it, yes —
   it needs a DTO. If it only describes something that *happened*, its effects live on the parties
   it touched.

---

## 4. The artifact — `openspec/changes/<change-id>/domain.md`

One per change, alongside `proposal.md` / `design.md` / `tasks.md`. **Not** a second spec: the spec
says what the system must do; this says what the concepts are.

Its existence, path and place in the order are declared by the kit's OpenSpec schema
(`openspec/schemas/kmp/schema.yaml`), which makes `domain` require `specs` and makes `tasks` require
`domain`. That is why the order is enforced rather than remembered: `openspec status` reports
`domain (blocked by: specs)` until the spec is done. The schema carries the required-section list
(`openspec instructions domain`); this file carries the method.

```markdown
# Domain Model — <change>

## Frame
One paragraph: the capability's subject area, in the domain's own words.

## Moment-Intervals
| MI | Trigger | Participants (multiplicity) | Persisted? |
|---|---|---|---|
| <event> | <what starts it> | <role> (1..*) | yes / no |

## Roles
| Role | Plays in (MI) | Party it attaches to | Multiplicity |
|---|---|---|---|

## Parties / Places / Things
| Concept | What it is | Identity |
|---|---|---|

## Descriptions
| Description | Values | Rules |
|---|---|---|

## Attributes
| Concept | Attribute | Type | Notes |
|---|---|---|---|
**Derived, not stored:** `<attr>` = `<derivation>`   ← list every one, or write "none"
**Deferred:** `<attr>` — <why it can wait>

## Links
| From | To | Multiplicity | Togetherness |
|---|---|---|---|

## Mapping to the feature
| Archetype | Concept | KMP home |
|---|---|---|
| MI | <event> | `Repository.<method>()` + `ViewModel.<action>()` |
| PPT | <concept> | `data/model/<Concept>Response.kt` |
| Description | <enum> | `presentation`/`data` enum |

## Open questions
- <question the spec leaves open, with the default taken>
```

Fill it in the domain's language first; the **Mapping** table last. If the mapping table forces a
decision the Frame contradicts, the Frame is wrong.

---

## 5. When this runs

| Action | Domain model |
|---|---|
| `recon` (new project) | **optional, once.** FDD steps 1–2: a coarse model and a candidate list, before any change exists. See §9 — it produces hypotheses, not requirements. |
| `create` / `modify` | **required** before Phase 4 (build). A capability with genuinely no domain concepts records `Domain model: none` plus one sentence why — the explicit record is the point. |
| `review` | read, do not require: use it to check the code against the model (drift, not just layering) |
| `test` | read: fixtures and edge cases come from the Moment-Intervals and their multiplicities |
| `init` | not applicable to modeling (scaffolding has no domain yet) |

It is **not** a design artifact (Penpot owns the visuals) and **not** a spec (OpenSpec owns
requirements). It sits between them, and the build reads it.

---

## 6. The living model and reconciliation (C0)

A per-change model is not enough. The same concept appearing in two changes will be modeled twice —
once as `Account`, once as `User`; once correctly as a Role, once wrongly as a new entity — and the
two versions drift in code. So the project keeps a **living model**, in a directory of its own — parallel to `specs/`, both
living and project-wide:

```
openspec/
├── specs/          ← living specs (one per capability)
├── domain/         ← the project's domain material (kit-owned; OpenSpec does not see it)
│   ├── model.md    ← the living VOCABULARY — concepts, archetypes, what is derived
│   └── features.md ← the reconnaissance candidate list (§9)
└── changes/<id>/
    └── domain.md   ← this change's DELTA (the OpenSpec artifact), archived with it
```

The directory/file split is deliberate: `domain/` is **where the project's domain lives**, while
`changes/<id>/domain.md` is one change's delta. Same word, different shapes and different trees —
a bare `openspec/domain.md` beside `specs/` read like a peer artifact and was genuinely ambiguous.

Both models are written once, at the end of the change, from the same approved model. The living one
is read **twice**: by the proposal step (so a new spec reuses the project's words rather than
inventing `User` for an existing `Account`), and here at C0.

**It is a vocabulary, not a requirement source.** No SHALL/MUST, no behaviour, invisible to
`openspec validate`. Requirements come only from the spec; when a spec needs a concept the model
lacks, the spec decides and the model follows — never the reverse.

### Reconciliation: classify before you model

At C0, every concept this change needs is classified against the living model. This is not generic
de-duplication; Coad's archetypes give it teeth:

| Verdict | When | Action |
|---|---|---|
| **NEW** | no existing concept matches | model it fully |
| **EXTENDS** | the same concept, and this change adds attributes or links | read the existing definition, then write only the addition — never restate the whole concept |
| **REUSES** | the same concept, unchanged by this change | reference it by name; **never redefine it** |
| **CONFLICT** | the same name, a different meaning (or two names for one concept) | **stop and ask the user.** Never silently create a second one |

### The matching keys, strongest first

| Key | Why it is decisive |
|---|---|
| **The Moment-Interval** | The strongest signal. Two changes that need the same event usually hit the same endpoint → one `data.app` DTO, not two. Modeling one MI twice is the origin of drift. |
| **Role vs Party** | The most common false NEW. If a "new" concept shares most of its attributes with an existing one, it is a **Role on that party** (REUSES + a role field), not a new entity. |
| **Description** | Never fork a value set. Two `ContributionWindow` enums is a bug, not a coincidence. |
| **Party / Place / Thing** | Match on identity, not on name spelling — `Account` and `User` may be one concept under two names (a CONFLICT worth resolving, since the code will otherwise carry both). |

A **CONFLICT** is the one verdict that stops the run. It is exactly where the human must decide
which name wins and what the concept means; guessing produces two models and two DTOs that drift.

---

## 7. Anti-rationalization

| Excuse | Why it is wrong | Countermeasure |
|---|---|---|
| "The spec already lists the entities — modeling is redundant." | A spec lists *behaviours and outcomes*. Entities fall out of it incidentally, unordered, and usually missing the events. | Run the six steps; the MI list is what the spec never contains. |
| "I'll model it after the UI is built, from the screens." | Screens are one projection of the domain. Modeling from them bakes UI accidents into the data layer. | Moment-Intervals first, before any DTO is named. |
| "This moment-interval needs its own class — I'll add a UseCase." | Rule 9 forbids the UseCase layer in this kit. | An MI is a Repository method plus a ViewModel action. |
| "`rank` is a field on Contributor." | Rank is a *position in a sort*, not an attribute — storing it creates a value that goes stale the moment one contribution changes. | List it under **Derived, not stored** with its derivation. |
| "I'll skip `domain.md`; it's obvious." | "Obvious" is what produces a second entity that was really a Role, and a count that should have been an aggregation. | One paragraph per section, or an explicit `Domain model: none`. |
| "The change is a pure UI tweak, so no domain model." | Then say so — `Domain model: none (presentation-only change)`. Recording none is cheap; silently skipping is indistinguishable from forgetting. | Always record the verdict. |
| "There is no living model yet, so C0 does not apply." | On the first change there is nothing to reconcile — C0 is then simply "everything is NEW", and the change *creates* the living model. Skipping it means the second change has nothing to read. | Run C0 regardless; its output on an empty project is the whole concept set as NEW. |
| "The concept is named differently, but it's basically the same — I'll extend it quietly." | A same-name/different-meaning (or different-name/same-meaning) pair is a CONFLICT precisely because the code will end up carrying both. Deciding it silently is how `Account` and `User` both ship. | CONFLICT stops the run and goes to the user. |
| "I'll copy the existing concept into this change's model so it's self-contained." | Restating a REUSED concept creates the second source the living model exists to prevent — and the two copies drift. | Reference it by name; leave it defined once, in `openspec/domain/model.md`. |
| "Recon is done, so the change's `domain` artifact can be skipped." | Recon is coarse and hypothetical; the change's model is where a hypothesis becomes a decision with real attributes and multiplicities. C0 reconciles against recon — it does not replace it. | Always run the change-level model, with C0 reading `openspec/domain/model.md`. |
| "The feature list is the roadmap; I'll keep it updated." | A second backlog beside OpenSpec drifts, and its stale entries compete with `changes/` as the truth. | Recon runs once. After selection, `openspec/changes/` and `openspec/specs/` are the only truth. |
| "I'll make the recon list FDD-granular so it's thorough." | FDD features are hours of work; this kit's feature is a module. A list at the wrong level cannot be acted on. | Two levels: features (selectable, = a change) with functions nested under them. |
| "The user was vague, so I'll fill in the missing concepts." | An agent has no domain expert; inventing here produces a confident-looking wrong model the first real change has to undo. | Record it as an Open question with the assumption taken, and let the change reconcile it. |

---

## 8. Worked example — the leaderboard

The capability: *"a ranked list of GitHub contributors, with their contribution counts"*.

- **Moment-Interval:** *a contribution* — `Account` contributes to `Repository` over a time
  interval. This is the concept the whole feature is about, and the spec never names it: the spec
  says "contribution count", which is the *aggregation* of this event.
- **Role:** *Contributor* — an `Account` participating in a `Repository`. Not a separate entity.
- **Party / Place / Thing:** `Account`, `Repository`.
- **Description:** *Contribution Window* (last 7 days / all time), *Tier* (if the UI badges tiers).

What the model forces that the spec did not:

- **`contributionCount` is derived**, not stored — it is a sum over the contribution interval
  within the window. Consequence: it is a field the *repository* computes, and changing the window
  changes the number without any new entity.
- **`rank` is derived** — a position in the sort, never an attribute on `Contributor`.
- **`Contributor` is a Role**, so there is no `ContributorResponse` duplicating `AccountResponse` —
  it is `AccountResponse` in the contributor role, with the window's aggregate attached.

Three decisions settled before a single DTO was named. That is the layer the kit was missing.

---

## 9. Reconnaissance — FDD steps 1–2 (a new project)

A per-change model has one gap: **a project's first change names its concepts by accident.** The
proposal is written from a one-paragraph request, and whatever words it happens to use become the
living vocabulary every later change inherits. Coad's process does not have this gap — FDD starts
with *Develop an Overall Model*, before any feature is selected.

`kmp-domain-recon` performs those two steps once, on a project that has no change yet:

1. **Develop an overall model** — the domain split into areas, with the key concepts and their
   archetypes, at a deliberately coarse resolution.
2. **Build a feature list** — candidate work, ordered, for the user to choose from.

### Granularity — the trap, and the two levels

FDD's *feature* is small: `<action> the <result> <object>`, a few hours, dozens per project. **This
kit's feature is not that.** One feature here is a Gradle module with a data layer, a presentation
layer, DI, four integration points, a spec capability — a change that takes a session, not an hour.

So reconnaissance produces **two levels**, and the user selects the **upper** one:

| Level | Unit | Becomes | Named |
|---|---|---|---|
| Upper | **Feature** (an area of the domain) | one `/opsx-propose` change → one `feature/` module | the top-level list |
| Lower | **Function** (`<action> the <result> <object>`) | raw material for that change's `tasks.md` | nested under a feature |

Confusing the two produces a list the user cannot act on: FDD-granular items are far too small to be
a change, and treating one as a change yields a module with one method.

### The boundary — hypotheses, not requirements

FDD step 1 assumes **domain experts in a room**. An agent has no domain expert; its only input is
what the user says. Where the user is vague, the model can only guess — and guessing requirements is
what this kit forbids everywhere else.

Therefore reconnaissance output is explicitly **non-normative**:

- `openspec/domain/model.md` from reconnaissance is a **hypothesis**. It carries no SHALL/MUST — the
  same rule as §6 — and it is expected to be wrong in places.
- `openspec/domain/features.md` is a **candidate list**. It is not a plan, not a backlog, and not a
  commitment.
- The first real change's `domain` artifact (C0) **reconciles against it**, correcting it. That is
  where a hypothesis becomes a decision.

### The list does not stay in sync

After the user selects a feature, `features.md` is **historical**. The truth about what exists and
what is next is `openspec/changes/` and `openspec/specs/`. Recon ran once; nothing maintains the
list afterward, and nothing should try — a second backlog beside OpenSpec is exactly the kind of
duplicate source this kit removes elsewhere.

Skip reconnaissance entirely when the project is single-purpose (one obvious area, no ambiguity) —
and record the skip explicitly, the same way a concept-free capability records
`Domain model: none`.

### The candidate list — `openspec/domain/features.md`

```markdown
# Feature Candidates

> One-time reconnaissance output (FDD step 2). **Hypotheses, not requirements** — no SHALL/MUST.
> After a feature is selected, this list is historical: `openspec/changes/` and `openspec/specs/`
> are the truth. Nothing maintains it.

## <Feature name>
*<one line: the capability, in the domain's words>* · **selectable as one change**

- Function: <action> the <result> <object>
- Function: <action> the <result> <object>

## <Feature name>
...

## Open questions
- <question the brief left open> — assumption taken: <what was assumed>
```

The **feature** heading is what the user picks; its `Function:` lines are FDD-granular and become
input for that change's `tasks.md`, never a change of their own.
