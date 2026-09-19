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
| `create` / `modify` | **required** before Phase 4 (build). A capability with genuinely no domain concepts records `Domain model: none` plus one sentence why — the explicit record is the point. |
| `review` | read, do not require: use it to check the code against the model (drift, not just layering) |
| `test` | read: fixtures and edge cases come from the Moment-Intervals and their multiplicities |
| `init` | not applicable |

It is **not** a design artifact (Penpot owns the visuals) and **not** a spec (OpenSpec owns
requirements). It sits between them, and the build reads it.

---

## 6. Anti-rationalization

| Excuse | Why it is wrong | Countermeasure |
|---|---|---|
| "The spec already lists the entities — modeling is redundant." | A spec lists *behaviours and outcomes*. Entities fall out of it incidentally, unordered, and usually missing the events. | Run the six steps; the MI list is what the spec never contains. |
| "I'll model it after the UI is built, from the screens." | Screens are one projection of the domain. Modeling from them bakes UI accidents into the data layer. | Moment-Intervals first, before any DTO is named. |
| "This moment-interval needs its own class — I'll add a UseCase." | Rule 9 forbids the UseCase layer in this kit. | An MI is a Repository method plus a ViewModel action. |
| "`rank` is a field on Contributor." | Rank is a *position in a sort*, not an attribute — storing it creates a value that goes stale the moment one contribution changes. | List it under **Derived, not stored** with its derivation. |
| "I'll skip `domain.md`; it's obvious." | "Obvious" is what produces a second entity that was really a Role, and a count that should have been an aggregation. | One paragraph per section, or an explicit `Domain model: none`. |
| "The change is a pure UI tweak, so no domain model." | Then say so — `Domain model: none (presentation-only change)`. Recording none is cheap; silently skipping is indistinguishable from forgetting. | Always record the verdict. |

---

## 7. Worked example — the leaderboard

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
