---
name: kmp-init
description: "Scaffold a new, runnable Kotlin Multiplatform app (core/{common,data,designsystem} + composeApp + androidApp + iosApp) and wire it for the kit: OpenSpec planning, a local architecture checker, opencode config, and the KMP subagents. Use for 'create a KMP app', 'new Kotlin Multiplatform project', 'scaffold a KMP starter', 'start a new KMP project'. Not for adding a feature to an existing app — that is kmp-create-feature."
disable-model-invocation: false
version: 0.1.0
audiences: [kmp-engineer]
mode-default: review
requires:
  - shared/kmp-patterns.md
  - shared/kmp-state.md
  - policies/approval-checkpoints.md
---

# kmp-init — a new KMP app, wired for the pipeline

Creates a **runnable** Kotlin Multiplatform application from the kit's vendored
template, then wires it so the rest of the pipeline works immediately: OpenSpec for
planning, a local copy of the architecture checker, an `opencode.json` pointing at
this kit, and the KMP subagents.

Scaffolding is **deterministic** — `scripts/scaffold/km-init.mjs` moves bytes and
rewrites identifiers. No model generates project code, so two runs with the same
inputs produce the same tree.

This is the one skill that runs **outside** a KMP project: it creates one.

## The One Rule That Matters Most

**The script scaffolds; the skill does not hand-write the skeleton.** Never author
Gradle files, module trees or Kotlin sources by hand here — the template is the
source of truth, and the script is the only thing that instantiates it. The skill's
job is to collect the two inputs, run the script, and verify the result.

## Tool surface

`node scripts/scaffold/km-init.mjs` (run from the kit root, or by absolute path).
The template is `templates/kmp-project/`. The verifier is
`scripts/dev/e2e-check.mjs`. No MCP.

## The Token-Aware Brief Contract

Collect exactly these before running anything:

| Field | Flag | Rule |
|---|---|---|
| Destination | `--dest` | must be outside the kit; must be empty unless `--force` |
| Project name | `--name` | PascalCase, `^[A-Za-z][A-Za-z0-9_.-]*$` |
| Package | `--pkg` | lowercase dotted, at least two segments, e.g. `com.acme.myapp` |

Objective: one runnable app. Acceptance criteria: the script reports success; the
checker passes; no template identifier (`dev.kmpapp`, `KmpApp`) remains anywhere.

## Mandatory Workflow

- **Phase 0 — preflight (read-only).** Confirm `node -v` ≥ 22 and that the
  destination is empty or absent. Resolve name and package. **✋ Checkpoint:** show
  the two inputs and the destination; wait for an explicit OK. Never scaffold into a
  non-empty directory without the user saying so.
- **Phase 1 — dry run.** `km-init.mjs --dry-run` and report the file count it would
  write. **Exit:** the user confirms.
- **Phase 2 — scaffold.** Run `km-init.mjs` with `--with-openspec`. Report the file
  count and rewrite count it prints.
- **Phase 3 — verify.** From the new project:
  `python3 shared/scripts/kmp_check.py --all` (must PASS with zero features), then
  `node <kit>/scripts/dev/e2e-check.mjs --root <dest>` (must be 10/10). Grep the new
  tree for `dev.kmpapp` / `KmpApp` — expect zero hits. **✋ Checkpoint:** show the
  verification output.
- **Phase 4 — hand off to the normal pipeline.** The next steps are OpenSpec's:
  `/opsx-propose` → Penpot design → `/kmp-create-feature`. Do **not** start building
  a feature in this run.

## Critical Rules

1. **The script scaffolds; this skill never hand-writes project files.**
2. **Never scaffold into the kit itself**, and never into a non-empty directory
   without explicit confirmation.
3. **`rootProject.name` and the package must both be right**, because Compose
   Multiplatform derives its generated `Res` package from `rootProject.name`
   lowercased. The script rewrites that import; do not "fix" it by hand afterwards.
4. **After scaffold, the project's own checker is authoritative** for that project:
   `<dest>/shared/scripts/kmp_check.py`, wired as `./gradlew archTest`.
5. **A fresh project has zero features.** `--all` passing with 0 features is the
   correct result, not a failure.
6. **Do not delete `core/{common,data,designsystem}`.** They supply `Either`,
   `UiState`, `setState`, `ErrorConst`, `X*` components and the `App*` state screens
   every feature imports.
7. **`.kmp.json` records `appModule`** (`composeApp`). It is what the checker and the
   skills read for the app module — never hardcode `composeApp` past this point.

## Domain Architecture

What the template produces:

```
settings.gradle.kts        :composeApp, :androidApp, :core:{common,data,designsystem}
build.gradle.kts           SDK/JVM config, ktlint, Kover, the archTest task
core/common/               Either, UiState, setState (FlowExt), ErrorModel, UiText, DI
core/data/                 ErrorConst, ApiClient (Ktor → Either), DataStore, repo infra
core/designsystem/         XTheme, X* components, App{Loading,Error}State, motion, locale
composeApp/                App.kt, BaseAppNavHost.kt, initKoin.kt, WelcomeScreen.kt
androidApp/                Android entry point (MainActivity, MyApplication)
iosApp/                    Xcode project + Swift entry point
feature/                   created later by kmp-create-feature, one module per feature
```

`WelcomeScreen.kt` and the Welcome-only `BaseAppNavHost.kt` exist so the empty shell
compiles and runs. The integrator replaces both when the first feature is wired in.

## Modes & Policies

`mode-default: review`. The inputs, the dry run, and the verification are each shown
before proceeding. The script is not run without an approved destination. See
`policies/approval-checkpoints.md`.

## State Management

Scaffolding writes no run ledger — a new project starts clean. After the first
`kmp-create-feature` run, that project's `.kmp/run.json` is the ledger. See
`shared/kmp-state.md`.

## User Checkpoints

| After phase | Artifacts shown | What we ask |
|---|---|---|
| 0 | name, package, destination | approve before anything is written |
| 1 | dry-run file count | confirm the scaffold |
| 3 | checker + e2e results, branding grep | confirm the project is ready |
| 4 | the next-step list | stop; OpenSpec proposes, this skill does not build features |

## Naming Conventions

Project name is PascalCase (`MyApp`); the package is lowercase dotted with at least
two segments (`com.acme.myapp`). Feature packages nest under it
(`com.acme.myapp.dashboard`), matching `shared/kmp-patterns.md`.

## Anti-Rationalization Table

| Excuse the model makes | Why it's wrong | Countermeasure that halts the flow |
|---|---|---|
| "I'll write the Gradle files myself — it's only a few." | A hand-written skeleton drifts from the template the contracts were written against, and the features will not compile against it. | Run `km-init.mjs`; never author project files here. |
| "The destination has files, but they're harmless." | Scaffolding over an existing project silently mixes two trees. | Stop and ask. Only `--force` with explicit consent. |
| "The Res import looks wrong, I'll fix it." | It is derived from `rootProject.name`; hand-editing it breaks the build in a way the script exists to prevent. | Fix the project name/package inputs and re-scaffold, or report it as a script bug. |
| "The checker reports 0 features — something must be wrong." | A fresh project is exactly 0 features; the shell compiles standalone. | Treat PASS as correct; the first feature comes from `kmp-create-feature`. |
| "While I'm here I'll add the first feature." | Creating a feature is a different skill with its own spec/design gates. | Stop. Hand off to `/opsx-propose` and `kmp-create-feature`. |

## Helper Code Snippets

```bash
# discover the kit root (this file's repo) and dry-run
node <kit>/scripts/scaffold/km-init.mjs --dest /path/to/MyApp --name MyApp \
  --pkg com.acme.myapp --dry-run

# scaffold for real
node <kit>/scripts/scaffold/km-init.mjs --dest /path/to/MyApp --name MyApp \
  --pkg com.acme.myapp --with-openspec

# verify (from the new project)
python3 shared/scripts/kmp_check.py --all
node <kit>/scripts/dev/e2e-check.mjs --root .
```

## Reference Resources

- `scripts/scaffold/km-init.mjs` — the scaffolder (deterministic; `--dry-run`, `--normalize`).
- `templates/kmp-project/` — the vendored app template.
- `docs/migration-from-kmpilot.md` — template provenance and license attribution.
- `shared/kmp-patterns.md` — the architecture the template satisfies.

## Supporting Files

No `references/` tree: the contract is this file plus the script. The template is the
detail.

**Doctrine paths.** `shared/…` and `policies/…` resolve inside this bundle in native installs (vendored by the installer); in an opencode install they live at the kit root, two directories up from this file (`skills/<name>/SKILL.md`).
