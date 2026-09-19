# Kotlin Multiplatform AI Kit

An **opencode-only** agent kit for building Kotlin Multiplatform apps. Four layers, four
owners, and they never overlap:

| Layer | Owner | Owns | Produces |
|---|---|---|---|
| **Planning** | OpenSpec (`/opsx-*`) | *what* and *why* | `openspec/changes/**` → `openspec/specs/**` |
| **Domain** | `kmp-domain-model` (Coad color modeling) | *what the domain is* | `openspec/changes/<id>/domain.md` + the living `openspec/domain.md` |
| **Design** | Penpot (`penpot-*` skills) | *how it looks* | a `DESIGN.md` handoff + annoted Penpot file |
| **Build** | KMP skills (`kmp-*`) | *how it ships* | `feature/**`, `core/**`, Gradle wiring |

The spec always wins over code. The design always wins over a build guess. A feature's
requirements live in exactly one place — `openspec/specs/<capability>/spec.md` — never in
the code tree.

The chain is **`spec → domain → design?(UI) → build`**, and the order is not a judgment call: the
kit's OpenSpec schema (`openspec/schemas/kmp/schema.yaml`) declares it, so OpenSpec itself blocks
`domain` until the spec is done and blocks `tasks` until the domain model is. `shared/scripts/kmp_route.py`
reads those artifact states and writes `.kmp/route.json`; every step reads that file instead of
re-deriving. A missing spec is always blocking and is fixed first.

The domain layer keeps **two** models: the change's delta (`openspec/changes/<id>/domain.md`) and the
project's cumulative vocabulary (`openspec/domain.md`). The living one is read at the **proposal**
step, so a new spec says `Account` rather than inventing `User` for a concept the project already
has. It is a vocabulary, not a requirement source — it emits no SHALL/MUST.

> **Requirements:** this kit targets [opencode](https://opencode.ai) only. It does not carry
> Claude Code hooks, plugin manifests or namespaced commands, and none are needed.

---

## 1. Requirements

| Tool | Why | Check |
|---|---|---|
| **Node ≥ 22** | runs the scaffolder and the kit's dev scripts | `node -v` |
| **Python 3** | runs the deterministic architecture checker | `python3 -V` |
| **[OpenSpec](https://github.com/Fission-AI/OpenSpec) CLI** | the planning layer | `openspec --version` |
| **opencode** | the agent runtime | `opencode --version` |
| **JDK 21 + Android SDK** | to compile the app | `./gradlew --version` |

Install OpenSpec if you don't have it:

```bash
npm install -g @fission-ai/openspec
```

---

## 2. Install the kit

```bash
git clone <this-repo> ~/IdeaProjects/kmp-ai-kit
cd ~/IdeaProjects/kmp-ai-kit
npm link          # puts `kmp-ai-kit` on your PATH
```

Verify:

```bash
kmp-ai-kit help
```

---

## 3. Getting started — the whole flow

### Step 1 — Create the app

```bash
kmp-ai-kit new GithubLeaderboard com.example.demo
```

This copies a complete, runnable KMP app into `~/IdeaProjects/GithubLeaderboard`:

- `core/{common,data,designsystem}` — `Either`, `UiState`, `setState`, `XTheme`, `X*` components
- `composeApp/` — `App.kt`, `initKoin.kt`, `BaseAppNavHost.kt`, a placeholder `WelcomeScreen`
- `androidApp/`, `iosApp/`, the Gradle wrapper and version catalog
- **every skill, rule and the architecture guard** — copied in, so the project is
  self-contained and depends on no path outside itself
- an initialized `openspec/` and a `git` repo with its first commit

Flags: `--dry-run` (write nothing), `--linked` (thin refs to the kit instead of copying),
`--no-openspec`, `--no-git`, `--force`.

### Step 2 — Restart opencode

opencode loads its config, skills and plugins **once at startup** and does not hot-reload.

```bash
cd ~/IdeaProjects/GithubLeaderboard
opencode
```

Restarting matters: without it the agent has no `kmp-*` skills and every prompt below will
look like it was ignored.

Optional sanity check that the project is wired:

```bash
ls skills/ | grep kmp        # the 7 build skills live here (kmp-init is kit-only)
ls .opencode/agent/          # 11 subagents
ls .opencode/commands/       # the /opsx-* commands
```

Inside opencode, ask *"what KMP skills do you have?"* if you want the agent to confirm it
loaded them.

### Step 3 — Plan (OpenSpec owns this)

Ask your agent to plan the change. **Exact prompt:**

```
Propose a change called add-github-leaderboard: a GitHub contributor
leaderboard screen. It shows a ranked list of contributors — rank, display
name, avatar, contribution count — ordered by contribution count descending.
Use mock data for now, no network call, but keep the standard data layer so a
real API can replace it later. Make it the app's start destination, replacing
the placeholder Welcome screen. Handle loading, success and failure states.
```

The agent runs `/opsx-propose`, which creates:

```
openspec/changes/add-github-leaderboard/
├── proposal.md      what & why
├── specs/leaderboard/spec.md   the Requirement + Scenario delta
├── design.md        the technical decisions
└── tasks.md         the ordered checklist
```

Review the artifacts. Then **archive nothing yet** — implementation comes first.

> **Planning boundary.** `/opsx-propose` writes planning artifacts only. If the agent starts
> editing Kotlin in the same turn, stop it: the planning step is meant to end with the
> artifacts presented to you.

### Step 4 — Model the domain (Coad color modeling)

The spec says what the system must **do**. It does not say what the domain **is** — and that gap is
where a feature gets a stale stored count, or a duplicate entity that was really a Role.

**Exact prompt:**

```
Model the domain for the leaderboard change.
```

`kmp-domain-model` applies **Peter Coad's Color Modeling**: identify the **Moment-Intervals first**
(what *happens* — a contribution, not a contributor), then Roles, then Parties/Places/Things, then
Descriptions; then attributes and links. It writes
`openspec/changes/add-github-leaderboard/domain.md`, and maps each archetype onto this kit:

| Archetype | Lands in |
|---|---|
| Moment-Interval | a Repository method + a ViewModel action — **never** a UseCase class (Rule 9 forbids that layer) |
| Role | a field / nested object on the party (not a parallel DTO) |
| Party / Place / Thing | `data/model/*Response.kt` |
| Description | an `enum class` / constants |

For the leaderboard it settles three things the spec left open: **`contributionCount` is derived**
(a sum over the contribution interval, not a column), **`rank` is derived** (a position in a sort),
and **`Contributor` is a Role** on `Account` — so there is no `ContributorResponse` duplicating
`AccountResponse`. Full method + template: `shared/domain-modeling.md`.

It stops at five checkpoints: **C0** reconciles against the living vocabulary, then C1 the
Moment-Interval list — the model's spine. It writes two files: the change's `domain.md` and the living
`openspec/domain.md`.

### Step 5 — Design (optional; Penpot owns this)

Skip this if the feature has no visual work or you are happy with the design system's
defaults. When you want a real design:

```
Design the leaderboard screen in Penpot: a ranked list with an avatar, name and
contribution count per row, following the existing design tokens.
```

The `penpot-router` skill picks `penpot-build-screen` → `penpot-design-md`, and the result
is a `DESIGN.md` the build layer reads. Requirements: a Penpot MCP connection and an open
Penpot file (see `docs/setup-remote.md` / `docs/setup-local.md`).

A feature is **design-aware** exactly when a `DESIGN.md` exists for it. Never invent one.

### Step 6 — Build (the KMP layer owns this)

**Exact prompt:**

```
/kmp-create-feature leaderboard
```

Or in plain language: *"implement the leaderboard feature from the OpenSpec change."*

`kmp-router` routes to `kmp-create-feature`, which works **layer by layer with a checkpoint
at each step** — it will not one-shot the module:

1. **Phase 0** — resolves the app module, package prefix, `initKoin`, NavHost and core
   modules from the project (read-only).
2. **Phase 1** — reads the OpenSpec spec, the **domain model** (`domain.md`), and the Penpot
   `DESIGN.md` if any. Missing a blocking input here stops the run: spec → `/opsx-propose`,
   domain → `/kmp-domain-model`.
3. **Phase 2 — ✋ your first checkpoint.** It restates the request as a token-aware
   contract (context / objective / inputs / constraints / acceptance criteria), resolves the
   Platform Profile, and proposes the layer plan. **Approve it here.**
4. **Phase 3** — turns the plan into the change's `tasks.md`.
5. **Phase 4 — ✋ a checkpoint per layer.** Data layer → build + check → approve. UI layer →
   build + check → approve. Integration → build + check.
6. **Phase 5** — runs the app (runtime gate), reconciles the spec, ticks the tasks, and hands
   back to OpenSpec.

At every layer it runs:

```bash
python3 shared/scripts/kmp_check.py leaderboard
```

### Step 7 — Verify, run, and archive

```bash
# static gate
python3 shared/scripts/kmp_check.py --all
./gradlew archTest

# RUNTIME gate — the static checks cannot see the Koin graph, serialization
# contracts, or a cast that only executes at runtime. Run the app.
./gradlew :composeApp:run          # NOT :composeApp:desktopRun
```

A green checker is **not** "the app works". Three bugs that shipped through a fully green
`archTest` and passing unit tests — a `@Serializable` model missing defaults, an unsafe cast in
`equals()`, and `viewModelOf` with a defaulted parameter — each crashed the app on **all**
platforms at launch. Run the target. If you genuinely cannot (headless, no screen-recording
permission), render the screen off-screen to `build/smoke/*.png`, look at it, and record
`runtime: render-smoke` — never imply a pass you did not make. Details:
`shared/kmp-runtime-verification.md`.

Then close the loop:

```
/opsx-archive add-github-leaderboard
```

The change is archived and the living spec lands at
`openspec/specs/leaderboard/spec.md`. That file is the single source of truth from now on —
the next change to the leaderboard is a *modify*, not a *create*.

> Before archiving, every checkbox in the change's `tasks.md` must be ticked or explicitly struck
> through. An archive with unticked boxes warns "0/N tasks" and leaves a false *incomplete* record
> behind — don't force past it.

---

## 4. What the kit guarantees

### The architecture is checked, not trusted

`shared/scripts/kmp_check.py` mechanizes 19 of the architecture rules and is the gate:
imports across layer boundaries, direct `MutableStateFlow` writes, Material3 components
where an `X*` component exists, hardcoded display strings, the `*Screen.kt` allowlist,
`components/` placement, the DI module shape, and all four integration points.

```
19 checks · 3 feature(s) · 0 error(s) · 0 warning(s)
PASS
```

Exit code 1 on any error. `--baseline` reports errors as warnings (a scan tier for code that
predates the rules); it is unrelated to any "adopt" mode — there isn't one.

### ...and the app is actually run

The checker is **static**. It reads source; it cannot see the assembled Koin graph, a
`@Serializable` contract against a real payload, or a cast that only executes at runtime. Every
bug in this class passes the checker and the unit tests and then crashes on launch for **every**
platform. So the handoff gate is a launch, not a lint:

```bash
./gradlew :composeApp:run      # the desktop target shares the same Koin graph + serialization
```

`shared/kmp-runtime-verification.md` records the three real bugs that motivated this, when the run
is mandatory (any DI change, any `@Serializable` change, any shared `core/` type), and how to
record an honest `not-run`.

### Capabilities are probed before they are trusted

"Configured" ≠ "available". `npm run preflight` verifies the Penpot MCP chain end to end — server,
token, **and the in-app plugin**, which only a real tool call can confirm. In multi-user mode the
plugin's token and your client's token must match byte-for-byte.

### `feature/**` is guarded

`.opencode/plugins/protect-feature.ts` blocks edits under `feature/` unless the owning skill
created its marker. Test sources and `build.gradle.kts` are exempt. The marker expires after
2 hours, so a crashed run cannot leave the project permanently writable.

This replaces KMPilot's Claude Code `PreToolUse` hook, which never ran under opencode.

### The spec has one home

Requirements live only under `openspec/`. A build skill that cannot find a spec **stops and
sends you to `/opsx-propose`** rather than inferring requirements from the code.

### Tests never codify a bug

A test generator that finds wrong behaviour reports it and leaves the test **red**. It must not
"characterize" the bug with `assertFailsWith<…>` around it — that turns a regression into a
specification and has to be undone before the fix can land.

---

## 5. Day-to-day commands

| You want | Prompt / command |
|---|---|
| A new app | `kmp-ai-kit new MyApp com.acme.myapp` |
| Plan a change | *"Propose a change called … : <what it does>"* → `/opsx-propose` |
| Model the domain | *"Model the domain for the <x> change"* → `/kmp-domain-model` |
| Implement | `/kmp-create-feature <feature>` |
| Change an existing feature | `/kmp-modify-feature <feature>` — drafts a spec delta first |
| Audit a feature | `/kmp-review-feature <feature>` — read-only; reports, never edits |
| Generate tests | `/kmp-test-feature <feature>` |
| iOS Swift bridge | `/kmp-bridge-swift` — when an `expect`/`actual` needs real Swift |
| Design-system reuse | `kmp-using-design-system` — auto-activates on UI work |
| Review design vs code | `/penpot-design-to-code-review` |
| Close a change | `/opsx-archive <change>` |
| Architecture gate (static) | `./gradlew archTest` |
| Run the app (runtime gate) | `./gradlew :composeApp:run` — **not** `desktopRun` |
| Probe a capability | `npm run preflight` (Penpot MCP: server + token + plugin) |
| Run the checker directly | `python3 shared/scripts/kmp_check.py [feature \| --all]` |

`kmp-router` is the dispatcher: for any KMP request, ask it first and it names exactly one
target skill. It never edits code itself.

---

## 6. The catalog

### Build layer — 9 KMP skills

| Skill | Mode | Does |
|---|---|---|
| `kmp-init` | review | Scaffold a new app *(kit-only; not copied into projects)* |
| `kmp-router` | suggest | Compute `.kmp/route.json`; name one skill |
| `kmp-domain-model` | review | Coad color modeling → `domain.md` |
| `kmp-create-feature` | review | Build a feature from spec + domain + design, layer by layer |
| `kmp-modify-feature` | review | Change a feature, spec-delta first |
| `kmp-review-feature` | suggest | Audit against the rules; consumes the checker report |
| `kmp-test-feature` | review | Staged test generation (fixtures → data → ui/integration) |
| `kmp-bridge-swift` | review | The iOS Swift leg of a native capability |
| `kmp-using-design-system` | review | Reuse `X*` components; no hardcoded values |

### Design layer — 13 Penpot skills

`penpot-router` dispatches these: `penpot-foundations`, `penpot-component-factory`,
`penpot-build-screen`, `penpot-build-deck`, `penpot-build-from-code`, `penpot-design-md`,
`penpot-document-handoff`, `penpot-audit-tokens`, `penpot-audit-accessibility`,
`penpot-design-to-code-review`, `penpot-migrate`, `penpot-rename-layers`, `penpot-router`.

### Shared doctrine (single source of truth)

| File | What |
|---|---|
| `shared/kmp-patterns.md` | the 14 architecture rules |
| `shared/domain-modeling.md` | Coad's Color Modeling: archetypes, the six steps, archetype→KMP mapping, the `domain.md` template |
| `shared/kmp-x-components-catalog.md` | the `X*` component contracts |
| `shared/kmp-motion.md`, `shared/kmp-agent-base.md` | motion primitives; agent context |
| `shared/scripts/kmp_check.py` | the deterministic checker |
| `shared/scripts/kmp_route.py` | computes the routing decision → `.kmp/route.json` |
| `AGENTS.md` | the instructions layer every skill obeys |

---

## 7. What a scaffolded project looks like

```
GithubLeaderboard/
├── opencode.json          project-local config (no external paths)
├── AGENTS.md              the instructions layer
├── .kmp.json              appModule (read by the skills and the checker)
├── openspec/domain.md     the living domain vocabulary (created by the first domain model)
├── .kmp/route.json        the routing verdict (tooling output, git-ignored)
├── skills/                20 skills (kmp-init excluded — it builds new apps, not features)
├── shared/  policies/  prompts/  workflows/  docs/
├── .opencode/
│   ├── agent/             11 KMP subagents
│   ├── commands/opsx-*    OpenSpec commands
│   ├── skills/openspec-*  OpenSpec skills
│   └── plugins/protect-feature.ts
├── openspec/              specs/ + changes/ + config.yaml + schemas/kmp/
├── feature/               created by kmp-create-feature, one module per feature
├── core/{common,data,designsystem}
├── composeApp/  androidApp/  iosApp/
└── gradle/  gradlew  settings.gradle.kts  build.gradle.kts
```

---

## 8. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `kmp-*` skills missing in the agent | opencode hadn't loaded the project config | Restart opencode **from the project directory** |
| `archTest` fails with a python error | `python3` not on PATH | Install Python 3 (macOS: `brew install python`) |
| `./gradlew …` tries to download forever | no network / proxy | First build needs the Gradle distribution and AGP; allow it or use an offline mirror |
| Agent edits `feature/` and gets blocked | the feature guard | That's intended — go through `/kmp-create-feature` |
| A run died mid-way | a stale 2h marker may remain | The guard expires it automatically; remove `/tmp/.kmp-skill-active` to be sure |
| Checker reports errors on old code | code predating the rules | `--baseline` reports them without failing; fix new code first |
| App crashed on launch but `archTest` was green | the checker is static; Koin/serialization/cast failures need a run | `./gradlew :composeApp:run` and read the stack trace (`shared/kmp-runtime-verification.md`) |
| `:composeApp:desktopRun` says "No main class specified" | that task ignores `compose.desktop { application { mainClass } }` | use `./gradlew :composeApp:run` |
| iOS release link throws `OutOfMemoryError` | the Gradle/Kotlin daemon heap is too small for Kotlin/Native | raise `org.gradle.jvmargs` / `kotlin.daemon.jvmargs` in `gradle.properties` (4 GB ships by default) |
| Penpot tool call says "No Penpot instance connected for user token" | the in-app plugin is not connected, or its token differs from your client's by even one character | open Penpot → a design file → **File → MCP server → Connect**; regenerate the key under **Your account → Integrations** and update the client config; run `npm run preflight` |
| A Penpot text exists in the structure but renders nothing | `lineHeight` is a **multiplier**, not pixels — `34` means 34× the font size | set `lineHeight = 1.4`; see `shared/plugin-api-gotchas.md` #19 |

---

## 9. Repository layout (the kit itself)

```
AGENTS.md                    the instructions layer
skills/                      the skills (build + design)
workflows/                   routing tables and multi-step recipes
policies/                    mode defaults, approval checkpoints
shared/                      doctrine + the checker + schemas
prompts/                     brief templates
evals/                       golden evals per skill
templates/kmp-project/       the app template the scaffolder instantiates
scripts/cli.mjs              `kmp-ai-kit` — the CLI
scripts/scaffold/km-init.mjs the scaffolder
scripts/install/             kit lifecycle (install/update/uninstall)
scripts/dev/                  validators (`validate-kit`, `e2e-check`, lock)
openspec/                    the kit's own planning
docs/migration-from-kmpilot.md
```

Dev checks for the kit itself:

```bash
npm run validate        # kit content consistency
npm run lock:check      # content hashes
npm run e2e -- --root /path/to/a/kmp/project   # end-to-end against a real project
python3 shared/scripts/kmp_check_test.py       # the checker's self-test
```

---

## 10. License

Kit content is licensed CC-BY-4.0 upstream. The build layer ports domain knowledge, a
deterministic checker, and the app template from **KMPilot** (MIT); those parts retain their
MIT attribution (see `shared/scripts/kmp_check.py` and `docs/migration-from-kmpilot.md`).
Because the kit now contains substantial MIT code, MIT would be the better fit overall —
this is a pending decision, not a settled one.
