# Migration from KMPilot

The kit reimplements KMPilot's value and **drops KMPilot entirely** — it is not a
dependency, not a submodule, not a copied tree. This file records, item by item,
what was replaced, what was ported, and what was discarded, so the decision is
auditable and nothing gets silently reintroduced.

Source: `~/IdeaProjects/KMPilot` @ v0.2.1 (MIT). Target: this kit, opencode-only.

## Rule of engagement

Where a KMPilot capability **overlaps** the kit's own infrastructure, the kit wins:
KMPilot's version is discarded, not merged. KMPilot's non-overlapping **domain
knowledge** (KMP architecture) is ported and re-homed under the kit's layout. The
kit's existing Penpot layer is untouched — it is the design layer, and Penpot is
what owns design in the merged model.

## A. Overlapping infrastructure — kit wins, KMPilot discarded

| # | Capability | KMPilot's implementation | The kit's implementation (wins) | Disposition |
|---|---|---|---|---|
| 1 | Skill container & discovery | `.claude/skills/<n>/SKILL.md` | `skills/<n>/SKILL.md` + `skills.json` + `skills.lock` + `.well-known/` | discarded; KMP skills authored fresh in kit layout |
| 2 | Shared knowledge home | `.claude/skills/_shared/` | `shared/` + per-skill `references/` (progressive disclosure) | discarded; content re-homed |
| 3 | Routing / dispatch | `CLAUDE.md` trigger-keyword table | `kmp-router` + `penpot-router`, `workflows/*/pipeline.json` | discarded; two routers, one per layer |
| 4 | Slash commands & brief templates | `.claude/commands/*.md`, `_shared/spec-template.md` | `prompts/` templates + OpenSpec commands | discarded; OpenSpec is the only spec source |
| 5 | Approval gates & safe set | `.claude/hooks/*.sh`, `.claude/settings.json` | `policies/modes.json`, `shared/modes-and-policies.md` | discarded; policy semantics re-expressed |
| 6 | Cross-session state | `reinject-on-compact.sh`, memory index | `shared/state-management.md` (design), `shared/kmp-state.md` (build) | discarded; kit ledger model |
| 7 | Install / update / uninstall | `install.sh`, `update.sh`, `rename.sh`, `.kmpilot.json` | `scripts/install/*` + install manifest **for the kit**; `scripts/scaffold/km-init.mjs` + `templates/kmp-project` **for a new app** | discarded; the kit ships its own scaffolder |
| 8 | Version & release drift check | `VERSION`, `release.sh` | `package.json`, `skills.lock`, `validate-kit.mjs` | discarded; checks folded into kit validation |
| 9 | Test / CI gate framework | `kmpilot_check_test.py`, `adopt-matrix.sh` | `evals/` + `run-eval.mjs` + `validate-kit.mjs` | KMPilot's tests become kit fixtures, not a second framework |
| 10 | Design pipeline | `design-ui`, `verify-ui`, Stitch MCP, `stitch-project.json` | `penpot-*` skill set, `visual-self-review.md`, `design-quality.md` | **Stitch discarded**; Penpot is the design layer |
| 11 | Visual QA / token audit | `verify-ui` (code ↔ HTML) | `penpot-design-to-code-review`, drift/design-quality report schemas | superseded; becomes code ↔ Penpot/DESIGN.md |
| 12 | Spec template & storage | `_shared/spec-template.md`, `.claude/docs/{name}/spec.md` | **OpenSpec** `openspec/changes/` + `openspec/specs/` | **discarded**; one spec source, no code-tree copy |
| 13 | Plugin packaging | Claude Code plugin tree (parked, unpublished) | opencode-only: `.opencode/` config + `.opencode/plugins/` | discarded; Claude packaging is out of scope |
| 14 | Project scaffolding & rename | `install.sh` template mode + `scripts/rename.sh` | `scripts/scaffold/km-init.mjs` + `templates/kmp-project/` (vendored, normalized) | reimplemented — KMPilot's template is no longer cloned at install time |

## B. Non-overlapping domain knowledge — ported

| Asset | Ported to | Status |
|---|---|---|
| `patterns.md` (14 architecture rules) | `shared/kmp-patterns.md` | **done** (de-branded, Stitch section replaced by the Penpot `DESIGN.md` contract) |
| `kmpilot_check.py` (19 deterministic checks) | `shared/scripts/kmp_check.py` | **done** (paths de-branded, self-test passes) |
| `kmpilot_check_test.py` | `shared/scripts/kmp_check_test.py` | **done** (paths re-pointed; `PASS — every check fires`) |
| `X_COMPONENTS_CATALOG.md` | `shared/kmp-x-components-catalog.md` | **done** |
| `motion.md` | `shared/kmp-motion.md` | **done** |
| `agents/_base/common.md` | `shared/kmp-agent-base.md` | **done** |
| `create-feature` 6 phases | `skills/kmp-create-feature/` | **done** — `SKILL.md` + `references/{phases,architecture,troubleshooting,templates}/`; the Stitch design phase is replaced by `01-inputs.md` (OpenSpec + Penpot) |
| `modify-feature` | `skills/kmp-modify-feature/` | **done** — spec-delta-first workflow, Stitch branches removed |
| `review-feature` + `code-reviewer` | `skills/kmp-review-feature/` + `.opencode/agent/kmp-code-reviewer.md` | **done** — checker-first, verbatim reporting |
| `test-feature` + 6 test agents | `skills/kmp-test-feature/` + `.opencode/agent/kmp-test-*.md` | **done** |
| `bridge-swift` | `skills/kmp-bridge-swift/` | **done** |
| `using-design-system` (+ references) | `skills/kmp-using-design-system/` | **done** — Stitch Design-Aware Mode rewritten to the Penpot handoff |
| 11 agents (`feature-development/*`, `feature-testing/*`, `code-quality/*`) | `.opencode/agent/kmp-*.md` (flat, `mode: subagent`) | **done** |
| template-mode project skeleton (200 files: `core/*`, `composeApp`, `androidApp`, `iosApp`, gradle) | `templates/kmp-project/` + `scripts/scaffold/km-init.mjs` | **done** — vendored, de-branded to `dev.kmpapp`/`KmpApp`, `archTest` rewired to the kit's checker |
| `scripts/rename.sh` substitution semantics | the `renameTree()` in `scripts/scaffold/km-init.mjs` | **done** — same two-phase sentinel rewrite and package-dir move |
| `spec-template.md` | **not ported** — superseded by OpenSpec | discarded by design |
| `design-ui`, `verify-ui`, Stitch references | **not ported** — Penpot owns design | discarded by design |

## C. Deliberately out of scope

- `.claude/docs/_roadmap/**` — KMPilot's internal phase plan; irrelevant here.
- KMPilot's Claude Code plugin build (`pipeline/`, `gen-surfaces.py`, `release.sh`).
- KMPilot's two-remote GitHub/Bitbucket branch conventions.
- Stitch MCP and any Stitch API key handling.

## D. What exists now

| Artifact | Purpose |
|---|---|
| `openspec/` + `openspec/config.yaml` | planning layer; KMP-aware context + per-artifact rules |
| `.opencode/commands/opsx-*.md`, `.opencode/skills/openspec-*` | OpenSpec's opencode surface (generated, do not hand-edit) |
| `opencode.json` | skills path, `feature/**` edit backstop, instructions |
| `.opencode/plugins/protect-feature.ts` | opencode port of KMPilot's feature-file guard |
| `.opencode/agent/kmp-*.md` (11) | flattened subagents: data-layer, ui-layer, platform, integrator, 6 test generators, code-reviewer |
| `shared/kmp-patterns.md` | the 14 rules (authoritative) |
| `shared/kmp-x-components-catalog.md`, `shared/kmp-motion.md`, `shared/kmp-agent-base.md` | design-system catalog, motion contract, agent context contract |
| `shared/scripts/kmp_check.py` + `kmp_check_test.py` | deterministic checker (19 checks) + its self-test |
| `shared/kmp-state.md` | build-layer run ledger (`.kmp/run.json`) |
| `skills/kmp-router/` | build-layer dispatcher (one target, no mutation) |
| `skills/kmp-create-feature/` | flagship build skill: SKILL.md + phases/architecture/troubleshooting/templates references |
| `skills/kmp-{modify,review,test,bridge-swift,using-design-system}/` | the rest of the build-layer skill set |
| `workflows/kmp-routing/` | build-layer routing table (all 6 skills routed) |
| `evals/golden/kmp-*.eval.json` | golden evals for the router, create-feature guard, and checker-first review |

## E. Open decisions carried into Phase 2

1. **Kit rename.** `package.json` still says `penpot-ai-kit`, and several `$id`/descriptions
   are Penpot-branded. Rename the package and manifests to the merged kit identity — one
   coordinated pass, not piecemeal.
2. **Spec migration for existing KMP features.** Decide whether pre-existing
   `.claude/docs/*/spec.md` files (if any downstream repo has them) get imported into
   `openspec/specs/` or regenerated.
3. **Penpot ↔ KMP token bridge.** The highest-value, highest-effort piece: make
   `penpot-design-to-code-review` audit code against a Penpot `DESIGN.md`/tokens instead of
   Stitch HTML. This is the piece that makes "code ↔ design" drift detection real.

## F. Phase 2 — end-to-end validation (done)

Validated against a real KMP repo (`feature/` + `core/{common,data,designsystem}`, 6
features), used **read-only as a fixture**; KMPilot is not a dependency. Harness:
`node scripts/dev/e2e-check.mjs --root <kmp-project> [--expect-errors N] [--with-openspec]`.

| Check | Result |
|---|---|
| Reference implementation passes the ported checker | ✓ 19 checks · 6 features · 0 errors · 0 warnings |
| Ported checker reproduces KMPilot's own verdict on the real repo | ✓ identical `mode`/`features`/`checked`/`error`/`warning`/`violations` payloads |
| Injected real-code violation (data→presentation import) | ✓ both checkers: 1 finding, identical payload, exit 1 |
| Broken integration point (`dashboard(` removed from the NavHost) | ✓ I4 error, FAIL, exit 1 |
| Feature-file guard against the real tree | ✓ source blocked, test/Gradle bypassed, `core/` untouched, marker allows |
| Phase 0 anchors (app module, initKoin, NavHost, core modules, pkg prefix) | ✓ all resolve — zero unresolved |
| OpenSpec lifecycle in the fixture | ✓ change created, 1 requirement + 4 scenarios, `validate --strict` passes |
| Exit codes | ✓ unknown feature 2, no args 2, clean 0, violations 1 |
| Negative control (repo with a violation) | ✓ `--expect-errors 0` fails, `--expect-errors 1` passes |

**Not covered by automation:** `./gradlew archTest` could not run here — the Gradle
wrapper needs to download its distribution and resolve AGP, and this environment has
no network for that. The task is a three-line `Exec` wrapper whose `commandLine` is
exactly `python3 shared/scripts/kmp_check.py --all`, which the harness runs directly.
Wiring it in a network-enabled project is the one remaining manual step.

## G. Phase 3 — a new app can be created without KMPilot (done)

KMPilot's last remaining role was scaffolding: its `install.sh` template mode cloned the
KMPilot repo at a tag and trimmed the demo features. The kit now owns that.

- `templates/kmp-project/` — the trimmed template tree (193 files: `core/{common,data,designsystem}`,
  `composeApp`, `androidApp`, `iosApp`, gradle wrapper + catalog), vendored and **normalized**
  from KMPilot's identity to the kit's (`KmpApp` / `dev.kmpapp`). `feature/*`, the demo
  `app/` tiers, `WelcomeScreen` is authored fresh (the empty shell compiles and runs),
  `BaseAppNavHost.kt` is Welcome-only, and `archTest` points at `shared/scripts/kmp_check.py`.
- `scripts/scaffold/km-init.mjs` — deterministic scaffold, exposed as the `kmp-init` npm
  bin (`npm run init`, or `npm link` once): copies the template, rewrites its
  identifiers to the user's name/package (reimplementing `rename.sh`'s two-phase sentinel
  rewrite and package-dir move, including multi-segment package paths), then wires
  `.kmp.json`, a local checker copy, `opencode.json`, the KMP subagents, `.gitignore`,
  OpenSpec (default on; `--no-openspec` opts out), and `git init` + an initial commit
  (`--no-git` opts out).

  `opencode.json` references the kit with **relative** paths by default, so the project and
  the kit stay movable together and a kit update reaches every project. `--vendored` copies
  the skills, rules, policies, AGENTS.md and the guard plugin into the project instead, for
  a fully self-contained repo that can be cloned or shared on its own. Linked paths are
  computed with `realpathSync` on both sides before `relative()` — a lexical relative path
  resolves against the wrong tree when either side is reached through a symlink
  (macOS `/tmp` → `/private/tmp`).
- `skills/kmp-init/` — the skill contract: two inputs, a dry run, a scaffold, a verification,
  and a handoff to `/opsx-propose`.

Verified by scaffolding fresh projects and running the checker + `e2e-check` against them
(10/10), with a grep confirming no template identifier survives.

## H. Open decisions carried into Phase 4

1. **Kit rename.** `package.json` still says `penpot-ai-kit`. Needs a decision on the new
   package name and repository URL.
2. **License.** The kit is CC-BY-4.0 upstream and now contains MIT code and a vendored
   template from KMPilot. CC-BY is a poor fit for a code-bearing kit; MIT (with attribution
   preserved in `shared/scripts/kmp_check.py` and this file) is the recommended license.
   This is a human decision, not a mechanical edit.
3. **Penpot ↔ KMP token bridge.** The highest-value remaining piece: make
   `penpot-design-to-code-review` audit code against a Penpot `DESIGN.md`/tokens instead of
   Stitch HTML.
4. **Template refresh procedure.** `templates/kmp-project` is a snapshot. To refresh it from
   upstream: re-copy the trimmed tree, then `node scripts/scaffold/km-init.mjs --normalize`   to re-apply the identity rewrite. Worth automating if upstream moves.
