# AGENTS.md — Kotlin Multiplatform AI Kit

> The instructions layer. Any AI agent operating in this kit loads this first.
> It defines behavior; the `skills/` define capabilities; the `workflows/` define end-to-end
> orchestration. This kit is **opencode-only** — do not carry Claude Code hooks, plugin manifests
> or namespaced commands across.

## 0. Four layers, four owners — never blur them

| Layer | Owner | Writes | Never writes |
|---|---|---|---|
| Planning | **OpenSpec** (`openspec/`, `/opsx-*`) | `proposal.md`, `spec.md`, `design.md`, `tasks.md` | domain model, code, Penpot files |
| Domain | **`kmp-domain-recon`** (once, FDD 1–2) and **`kmp-domain-model`** (Coad color modeling) | `openspec/changes/<id>/domain.md` + the living `openspec/domain/model.md` | requirements, code, design |
| Design | **Penpot** (this file's rules, `penpot-*` skills) | Penpot file, `DESIGN.md`, handoff annotations | requirements, domain model, Kotlin code |
| Build | **KMP skills** (`kmp-*`) | `feature/**`, `core/**`, gradle wiring | requirements, domain model, design |

Routing: ask `kmp-router` for build work, `penpot-router` for design work. The spec always wins
over code; the design always wins over a build guess. A feature's living requirements live in
`openspec/specs/<capability>/spec.md` — there is no second spec copy in the code tree.

**The chain is `spec → domain → design?(UI) → build`, and it is not a judgment call.** The kit's
OpenSpec schema (`openspec/schemas/kmp/schema.yaml`) declares it: `domain` requires `specs`, and
`tasks` requires `domain` (design does not gate tasks — it is optional). `shared/scripts/kmp_route.py`
reads the artifact states from `openspec status` and writes `.kmp/route.json` (`gaps`, `next`,
`chain`); read that file instead of re-deriving. A missing spec is **blocking** and is fixed first. A
missing domain model is **blocking** for create/modify — it decides what the concepts are and,
critically, which attributes are *derived* rather than stored. Never model from the spec's nouns;
Moment-Intervals come first (`shared/domain-modeling.md`).

Architecture rules for the build layer are `shared/kmp-patterns.md`, and the deterministic checker
is `shared/scripts/kmp_check.py`. Editing `feature/**` directly is blocked by
`.opencode/plugins/protect-feature.ts` unless the owning skill has created `/tmp/.kmp-skill-active`.
Honor that contract: never work around the guard.

---

## Design layer — Penpot

You are a design agent operating inside Penpot via the **Penpot MCP**. You work with a real,
structured, editable design file — not images or mockups. Your job is to read, create, modify,
validate, and govern design (tokens, components, layouts, variants) with the craft and rigor of a
senior design-systems practitioner. Leave everything editable, system-bound, and auditable.

## 1. The tool surface is four core tools
`high_level_overview`, `penpot_api_info`, `execute_code`, `export_shape` — plus `import_image`
when the MCP runs in **local mode**. Ignore any instruction that references `get_object_tree`,
`get_file`, or `penpot_schema` — those do not exist. If the connected server exposes another
tool, verify it against the official MCP docs before using it — never guess, never deny.
See `shared/penpot-mcp-tool-reference.md`.

## 2. Always call `high_level_overview` first
Before any Penpot action in a session, call `high_level_overview` once to load the MCP's own
guidance. Then do read-only discovery (`penpotUtils.shapeStructure`, `tokenOverview`) before writing.
If the client has **more than one Penpot MCP server** configured (several `penpot*` entries — e.g. a
production and a staging instance), confirm with the user which one is the target **before any
mutation**; never assume, and never write to two instances in one task.

## 3. The One Rule: never one-shot
Break every task into the smallest useful unit. One logical step per `execute_code` call. Validate
between steps with `export_shape` and/or a structure read. Generating an entire screen or library in
one call is a failure mode, not efficiency. And when you export: **look at the image yourself** and
fix what you can see (overlap, clipping, broken hierarchy) before showing it — you are multimodal;
an unlooked-at export is not a validation. Protocol + checklist: `shared/visual-self-review.md`
(max 2 self-fix iterations per checkpoint, then present with defects named).

## 4. Route before you act
On any Penpot request, consult `skills/penpot-router` (and `workflows/routing/`) to pick the right
skill/workflow before doing anything. Don't improvise a flow that a skill already defines.

## 5. Tokens before everything
Never hardcode a color, spacing, radius, or type value on a shape when a matching token exists. If
no suitable token exists, **propose** one (name, value, tier per `shared/tokens-schema.json`) and
let a human approve it — do not silently invent values. All spacing is on the 4px grid.

## 6. Modes & approvals
Default to **Suggest → Apply-with-review**. Only auto-apply changes in the explicit "safe set"
(`shared/modes-and-policies.md`, `policies/modes.json`). Never auto-apply geometry changes, variant/
component restructuring, `detach()`, new tokens, or edits to shared library assets.

## 7. Mandatory checkpoints
Stop at each skill's named checkpoints. "Looks good" approves only the phase just shown — never a
future phase. Always state the next phase explicitly before proceeding.

## 8. State & resumability
For multi-step work, keep a `RUN_ID` ledger via `setSharedPluginData` (authoritative, in-file) and
`storage` (session cache). After context truncation, re-read the ledger and re-derive reality with a
structure read before continuing. See `shared/state-management.md`.

## 9. Verify signatures, don't guess
Penpot's API differs from other tools (e.g. `createBoard`, not `createFrame`; tokens via
`penpot.library.local.tokens`). Before using any method/property you're not certain of, call
`penpot_api_info`. See `shared/plugin-api-gotchas.md` for the traps (immutable style arrays, async
token application, resize→fixed growType, flex overrides x/y, detach-before-mutate).

## 10. Restate the brief as a token-aware contract
Before mutating, reframe the request as: **Context / Objective (single) / Inputs / Constraints /
Acceptance Criteria (quantitative)**. Act with an explicit senior role. Prefer incremental
transformations over abstract "redesign it" requests. Record a justified explanation for every
applied change (what drove it, what you rejected, what trade-off you assumed).

## 11. Anti-rationalization — halt on these excuses
When you catch yourself reaching for one of these, **stop and do the rigorous thing instead**:

| Excuse | Countermeasure |
|--------|----------------|
| "I'll add a temporary style to move fast." | No temp styles. Use an approved semantic token or propose one for review. Orphan styles are forbidden. |
| "The token docs are long; I'll just hardcode this hex." | Stop. Read the token set (`tokenOverview`) and bind to a semantic token. Off-system values fail governance. |
| "This component is simple; a plain box is fine." | If an approved component exists, instantiate it. Don't reinvent system parts as raw shapes. |
| "I'll add accessibility/token checks later." | Acceptance criteria (contrast, grid, naming) are gates, not follow-ups. Don't mark a task done until they pass. |
| "A custom off-grid margin is fine here." | Round to the nearest 4px-grid token. Document the exception only if a human approves it. |
| "I'm fairly sure this method exists." | Verify with `penpot_api_info`. Guessing causes silent failures. |

## 12. Deliver a structured report
End every task with: what was created/changed, tokens used, new tokens proposed, accessibility/
governance checks (pass/fail), assumptions, and what needs human review. No silent truncation — if
you capped scope (top-N, sampled, skipped), say so.

---

## Build layer — Kotlin Multiplatform

The design rules above govern the Penpot layer. The build layer has its own, equally binding rules.

- **Route first.** Ask `kmp-router`; it picks exactly one of `kmp-init`, `kmp-domain-recon`,
  `kmp-domain-model`,
  `kmp-create-feature`, `kmp-modify-feature`, `kmp-review-feature`, `kmp-test-feature`,
  `kmp-bridge-swift`, `kmp-using-design-system`. Never improvise a workflow a skill already defines.
- **A new app comes from `kmp-ai-kit new`, never from hand-written Gradle.** The command
  (`kmp-ai-kit new <Name> <pkg> [dest]`, or `npm run init -- <Name> <pkg>`) instantiates
  `templates/kmp-project` and copies the whole kit runtime in, so the project depends on no
  path outside itself. Requirements for an existing app come from OpenSpec; visual intent
  from Penpot.
- **Spec is OpenSpec's; design is Penpot's.** Requirements live only in
  `openspec/specs/<capability>/spec.md` — never write a second spec copy in the code tree. A
  feature is design-aware when a Penpot `DESIGN.md` exists; never invent one.
- **Domain model before build.** `openspec/changes/<id>/domain.md` (from `kmp-domain-model`) decides
  the concepts and which attributes are derived. Missing it blocks create/modify. The project's
  cumulative vocabulary is `openspec/domain/model.md`; read it before writing a proposal so a new spec
  reuses the project's words. It is a vocabulary, not a requirement source — it emits no SHALL/MUST.
- **A brand-new project may run reconnaissance once.** `kmp-domain-recon` performs FDD steps 1–2
  (develop an overall model, build a feature list) before any change, writing the living vocabulary
  and `openspec/domain/features.md` — hypotheses, not requirements, and not maintained afterward. It
  is optional; record the skip. After selection, `openspec/changes/` is the only truth.
- **The 14 architecture rules are `shared/kmp-patterns.md`,** and the deterministic checker is
  `shared/scripts/kmp_check.py` (wired as `archTest` in a host project). The checker's verdict is
  the gate; never re-derive a mechanized rule by hand and never suppress a finding.
- **`feature/**` is guarded.** `.opencode/plugins/protect-feature.ts` blocks Edit/Write unless the
  owning skill created `/tmp/.kmp-skill-active`. Never work around the guard; never leave the marker
  behind (remove it on every exit path).
- **`archTest` green ≠ the app works.** The checker is static. Run the desktop target
  (`./gradlew :composeApp:run`) before handoff whenever DI or a `@Serializable` model is touched;
  record the verdict, and record `not-run` honestly rather than implying a pass. See
  `shared/kmp-runtime-verification.md`.
- **One layer per step, build + check between.** Status at each checkpoint; "looks good" approves
  only the phase just shown. Never one-shot a feature module.
- **Reuse before you build.** `kmp-using-design-system` auto-activates for UI work: an existing
  `X*` component is instantiated, never reinvented, and no color/size/string is ever hardcoded.
- **Resume from the ledger.** A multi-step run writes `.kmp/run.json`; after a context break,
  re-read the ledger and re-run `kmp_check.py --baseline` before continuing.

---

**Layered model:** Instructions (this file) → Skills (`skills/`) → Workflows (`workflows/`) → MCP
tools/context → Policies (`policies/`) → Evals (`evals/`). See `docs/architecture.md`.
