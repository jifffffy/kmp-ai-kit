---
name: kmp-using-design-system
description: "Guard for UI work in a KMP feature: reuse an existing `X*` component and its tokens before authoring anything new, and never hardcode a color, size, or string. Auto-applies to any feature UI change. Triggers: 'use the design system', 'which component should I use', 'build this UI in Compose', 'add a screen', 'style this'."
disable-model-invocation: false
version: 0.1.0
audiences: [kmp-engineer, design-engineer]
mode-default: review
requires:
  - shared/kmp-patterns.md
  - shared/kmp-x-components-catalog.md
  - shared/kmp-motion.md
  - policies/approval-checkpoints.md
---

# kmp-using-design-system — reuse before you build

Before any UI is written in a feature, this skill decides *what to reuse*. Its job
is to stop the two failure modes that make a KMP codebase drift: reinventing a
component the design system already ships, and hardcoding a value a token owns.

This skill **auto-activates** for UI work and does not need an explicit call. It
never mutates on its own — it constrains the skill doing the mutating.

`references/workflow.md`, `references/component-mappings.md` and
`references/usage-examples.md` carry the detail; this file is the contract.

## The One Rule That Matters Most

**If an approved `X*` component exists, instantiate it — do not build a raw shape.**
Only when nothing matches do you build new, and then it must be built *from*
design-system primitives and tokens, in the feature's `components/` directory.

## Tool surface

No MCP. File tools plus the Gradle wrapper. The component inventory is
`shared/kmp-x-components-catalog.md`; motion primitives are `shared/kmp-motion.md`;
the architecture rules are `shared/kmp-patterns.md`.

## The Token-Aware Brief Contract

Before approving UI work, restate: **Context** (feature, screen, audience) /
**Objective** (the single UI change) / **Inputs** (the Penpot `DESIGN.md` role →
component mapping, the component catalog) / **Constraints** (only existing
`X*` components, no raw hex/size, all user-facing text as string resources) /
**Acceptance criteria** (every node uses a theme role, no hardcoded literal, the
checker passes).

## Mandatory Workflow

`references/workflow.md` is authoritative. In outline:

- **Phase 0 — inventory (read-only).** Map each element of the UI being built to a
  catalog component. Record matches and genuine gaps. **Exit:** a mapping table.
  No writes.
- **Phase 1 — resolve gaps.** For each gap, decide: reuse with parameter/modifier
  overrides, or author a new component from primitives. **✋ Checkpoint.**
- **Phase 2 — hand the constraint set to the building skill.** Emit the mapping and
  the rules the build must satisfy; the build skill (create/modify) does the writing.

## Critical Rules

1. **Reuse first.** An `X*` component that exists is used, not re-created.
2. **No raw color, size, radius, or type values.** Every value is a theme role or a
   design token; `MaterialTheme.colorScheme.{role}` / `MaterialTheme.typography.{role}`.
3. **No hardcoded user-facing text.** Strings are resources (Rule 12).
4. **New components live in the feature's `components/`**, one per file, built from
   design-system primitives — placed per `shared/kmp-patterns.md`'s UI file layout.
5. **Respect component constraints.** Read a component's full source before
   overriding it — its `defaultMinSize`, hardcoded padding, and non-overridable
   modifiers are architectural limits, not bugs to hack around.
6. **Motion uses shipped primitives** and gates every animation with
   `rememberReducedMotion()`. See `shared/kmp-motion.md`.
7. **This skill never writes code.** It constrains; the owning build skill writes.

## Domain Architecture

```
:core:designsystem          X* components + XTheme (colors, typography, motion primitives)
:core:designsystem/<pkg>/app  App* state screens (AppLoadingState / AppErrorState)
feature/<name>/…/ui/components/   feature-specific composables (one per file)
```

## Modes & Policies

`mode-default: review`. The mapping and the constraint set are shown before the
building skill writes. Nothing is auto-applied. See
`policies/approval-checkpoints.md`.

## State Management

No ledger of its own — it runs inside a create/modify run and writes its mapping
into that run's `.kmp/run.json` entry. See `shared/kmp-state.md`.

## User Checkpoints

| After phase | Artifacts shown | What we ask |
|---|---|---|
| 1 | element → component mapping table + gap list | approve reuse decisions and any new component |
| 2 | the constraint set handed to the build skill | confirm before the build writes |

## Naming Conventions

Design-system components are `X*` (`XButton`); app-state screens are `App*`
(`AppLoadingState`); feature components are `{Feature}{Thing}`. Follow
`shared/kmp-x-components-catalog.md` and `shared/kmp-patterns.md` exactly.

## Anti-Rationalization Table

| Excuse the model makes | Why it's wrong | Countermeasure that halts the flow |
|---|---|---|
| "This component is simple; a plain `Box` is fine." | Reinventing a shipped `X*` component is the primary cause of design drift. | Check the catalog first; instantiate the matching `X*` component. |
| "I'll hardcode this hex just for now." | Orphan values never get cleaned up and break theming. | Bind to a semantic token; if none fits, propose one for approval. |
| "I'll add a private `LoadingContent` — it's only this screen." | Loading/Failed UI is shared (`AppLoadingState`/`AppErrorState`); a private one is an orphan. | Use the shared `App*` state screen. |
| "The component's min-size is in my way, I'll wrap it and force the size." | Component constraints are deliberate; fighting them with hacks is an architecture violation. | Read the component source, then override through the supported parameter or record it as a limitation. |
| "The text is short, a literal is fine." | Rule 12: user-facing text is a string resource. | Add a key to the feature's `strings.xml` and reference it. |

## Helper Code Snippets

```bash
# find an existing component before building one
grep -rn "fun X" core/designsystem/src/commonMain --include=*.kt
# confirm the checker still passes after UI work
python3 shared/scripts/kmp_check.py {NAME}
```

## Reference Resources

- `shared/kmp-x-components-catalog.md` — the `X*` component contracts.
- `shared/kmp-motion.md` — motion primitives and `rememberReducedMotion()`.
- `shared/kmp-patterns.md` — UI file organization and Rule 12.

## Supporting Files

| File | Use |
|---|---|
| `references/workflow.md` | the reuse-first workflow and Design-Aware mode (Penpot `DESIGN.md`) |
| `references/component-mappings.md` | element → `X*` component mapping table |
| `references/usage-examples.md` | worked Compose usage examples |

**Doctrine paths.** `shared/…` and `policies/…` resolve inside this bundle in native installs (vendored by the installer); in an opencode install they live at the kit root, two directories up from this file (`skills/<name>/SKILL.md`).
