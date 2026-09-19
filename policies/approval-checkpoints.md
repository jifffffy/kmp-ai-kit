# Approval checkpoints

Shared checkpoint rules across all skills/workflows.

## The core rule
**"Looks good" approves only the phase just shown — never a future phase.** Always name the next phase
explicitly before proceeding.

## At every checkpoint, show
1. **Evidence** — an `export_shape` (`'selection'` or `'page'`) of what changed, and/or a structured
   read (`shapeStructure` / `tokenOverview`).
2. **Summary** — what was created/changed, tokens used, anything proposed, assumptions.
3. **The ask** — a specific question ("Approve these tokens? Next I'll build components.").

## Checkpoint matrix (typical)
| Skill | Hard checkpoints (require approval) |
|-------|--------------------------------------|
| penpot-foundations | after primitives, after semantic, after themes, before applying to shapes |
| penpot-component-factory | after axis matrix, after base, after variants, before combining |
| penpot-build-screen | after direction, after frame, after each section, after assemble |
| penpot-build-from-code | after discovery, after each section |
| penpot-migrate | after scope/mapping, after IR, after tokens, after components, per screen |
| penpot-audit-* | after the report (before any fix) |
| penpot-rename-layers | none for safe-set renames; review for meaningful names |
| kmp-domain-recon | **R1** the frame · **R2** areas + concepts · **R3** the selectable feature list |
| kmp-domain-model | **C0** reconciliation vs the living model · **C1** Moment-Intervals · **C2** Roles/PPT/Descriptions · **C3** attributes (stored/derived) + links · **C4** KMP mapping + spec coverage |
| kmp-create-feature | **Phase 2** the brief contract + layer plan · **each Phase 4 layer** (data/platform, ui, integration) |
| kmp-modify-feature | **the spec delta** (before any edit) · each implementation layer |
| kmp-init | the two inputs + destination, then the dry run |
| kmp-test-feature | the extracted context + Gradle additions, then the run |
| kmp-review-feature | the checker report (before the judgment review) · the final report |
| kmp-bridge-swift | the interop surface, then the implementation |

**Not every stop is equal.** A skill's checkpoint may be *gated* (no file is written until it
passes) or *advisory* (work continues, the stop is a review). `kmp-domain-model`'s C0–C4 are gated:
nothing is written until C4 approves, and Phase 5 writes only what C4 approved. A silently re-opened
checkpoint — editing an already-approved section and carrying on — is a violation regardless of the
skill; say so and re-run that checkpoint.

## Destructive / irreversible actions
Always require explicit approval, regardless of mode: `detach()`, deleting/renaming shared assets,
deleting shapes, restructuring variants. Record the rationale in the run report.
