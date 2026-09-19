# Domain Model — <change>

> Built with Coad's Color Modeling. See `shared/domain-modeling.md` for the method and the
> `kmp-domain-model` skill for the workflow. Moment-Intervals first, always.

## Frame
<!-- One paragraph: the capability's subject area, in the domain's own words. -->

## Moment-Intervals
<!-- What HAPPENS. Read the spec's scenarios as events. -->

| MI | Trigger | Participants (multiplicity) | Persisted? |
|---|---|---|---|

## Roles
<!-- How something participates — not a separate entity. -->

| Role | Plays in (MI) | Party it attaches to | Multiplicity |
|---|---|---|---|

## Parties / Places / Things
<!-- The actors and objects the roles attach to. -->

| Concept | What it is | Identity |
|---|---|---|

## Descriptions
<!-- Catalog-like value sets. If it has exactly two values and no rules, prefer a field. -->

| Description | Values | Rules |
|---|---|---|

## Attributes
<!-- Only now. Mark every derived attribute. -->

| Concept | Attribute | Type | Notes |
|---|---|---|---|

**Derived, not stored:**
<!-- e.g. `contributionCount` = sum of contributions in the window; `rank` = position in the sort. -->

## Links
<!-- How many, and must they be created/destroyed together? -->

| From | To | Multiplicity | Togetherness |
|---|---|---|---|

## Mapping to the feature

| Archetype | Concept | KMP home |
|---|---|---|
<!-- MI -> Repository method + ViewModel action (never a UseCase class) · PPT -> data/model/*.kt · Role -> field/nested object · Description -> enum -->

## Open questions
<!-- Questions the spec leaves open, with the default taken. Never invent a requirement to fill a table. -->
