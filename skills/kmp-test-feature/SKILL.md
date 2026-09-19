---
name: kmp-test-feature
description: "Generate the test suite for a Kotlin Multiplatform feature: fixtures, then DataSource/Repository, then ViewModel/UI/integration tests, using the feature's own shapes. Covers Turbine Flow assertions, Ktor MockEngine, and Compose UI tests. Triggers: 'generate tests', 'test this feature', 'add tests', 'write tests for the dashboard'."
disable-model-invocation: false
version: 0.1.0
audiences: [kmp-engineer]
mode-default: review
requires:
  - shared/kmp-patterns.md
  - shared/kmp-agent-base.md
  - policies/approval-checkpoints.md
---

# kmp-test-feature — fixtures first, then layers, in dependency order

Generates a feature's tests in the order the code depends on them: fixtures, then
data-layer tests, then presentation and integration tests. Each stage runs in a
subagent and is awaited before the next, so later tests can rely on earlier shapes.

## The One Rule That Matters Most

**Discovery is done by the caller, not an agent, and generation is staged in
dependency order.** The feature's real names (flows, state slots, namespaces) must be
extracted once and passed down; agents must not re-read source or guess names.
Fixtures come before everything; nothing runs in parallel that depends on something
else.

## Tool surface

Gradle wrapper for test runs and coverage; the checker for the architecture gate.
Test agents live in `.opencode/agent/kmp-test-*.md`. Rules are
`shared/kmp-patterns.md`.

## The Token-Aware Brief Contract

Restate: **Context** (feature, its layers, its spec) / **Objective** (a test suite
for one feature) / **Inputs** (the extracted context: namespaces, flow name, state
slot, capability flags) / **Constraints** (generated tests only; do not change
production code beyond adding missing test dependencies) / **Acceptance criteria**
(tests compile and pass, coverage gate met, checker still green).

## Mandatory Workflow

`references/workflow.md` is authoritative. In outline:

- **Phase 1 — discovery (direct, no agent).** Detect namespaces; extract a YAML
  context block (feature name, `{flow_name}`, `{state}`, capability flags);
  add test dependencies to `feature/{featurename}/build.gradle.kts` **only if
  missing**. **Exit:** a context YAML.
- **Phase 2 — project sync (conditional).** Runs only if Phase 1 changed Gradle.
  **✋ Checkpoint:** wait for user confirmation.
- **Phase 3 — spawn agents, staged.** 3.1 fixtures (sequential) → await;
  3.2 DataSource + Repository (parallel) → await; 3.3 ViewModel + UI + integration
  (parallel) → await.
- **Phase 4 — run the tests.**
- **Phase 5 — coverage** (`./gradlew koverHtmlReport`, gate at the project minimum).
- **Phase 6 — summary.**

## Critical Rules

1. **Phase 1 is direct** — no subagent for discovery.
2. **Never hardcode a flow or state name** — always substitute `{flow_name}` and
   `{state}`; use each agent's documented default only when the context omits it.
3. **Test dependencies are added only if missing** — never duplicate existing ones.
4. **Fixtures are sequential; only independent work is parallel.**
5. **No production-code changes** beyond adding missing test dependencies.
6. **The coverage gate applies to the ViewModel/Repository/DataSource/Screen
   subset** — see `shared/kmp-patterns.md`.
7. **The architecture checker must still pass** after tests are added.
8. **Never write a test that asserts a bug is correct.** When a generator finds behaviour that is
   clearly wrong while writing a test, the correct output is a **failing test plus a reported
   defect** — not a test that pins the wrong behaviour. `assertFailsWith<ClassCastException>` around
   an unsafe cast in `equals()`, or a test asserting a crash, "characterizes" the bug: it turns a
   regression into a specification, and after the fix it must be rewritten (or it blocks the fix).
   Generator rule: assert the *intended* behaviour. If that fails, report the failure as a finding
   and leave it red — do not downgrade it to match the code.

## Domain Architecture

```
feature/{name}/src/commonTest/kotlin/…   fixtures, fakes, test data
feature/{name}/src/desktopTest/kotlin/…  the JVM-run tests
.opencode/agent/kmp-test-*.md            the staged test generators
```

## Modes & Policies

`mode-default: review`. The extracted context and the dependency decision are shown
before agents spawn. Nothing is auto-applied to production code. See
`policies/approval-checkpoints.md`.

## State Management

Write the generation stage and coverage result to `.kmp/run.json` under the
capability's entry, so an interrupted suite resumes at the right stage. See
`shared/kmp-state.md`.

## User Checkpoints

| After phase | Artifacts shown | What we ask |
|---|---|---|
| 1 | the extracted context YAML + any Gradle additions | confirm before syncing |
| 3 | files written per stage | confirm before running tests |
| 5 | coverage report | decide whether the gate is met |

## Naming Conventions

Test files mirror the class under test (`{Feature}ViewModelTest.kt`); fixtures use
the `Fixtures` suffix from the feature's test tree. Follow `shared/kmp-patterns.md`.

## Anti-Rationalization Table

| Excuse the model makes | Why it's wrong | Countermeasure that halts the flow |
|---|---|---|
| "I'll let the test agent read the source to find the names." | Re-reading source in an agent costs context and invites name drift; the orchestrator already extracted the truth. | Pass the context YAML; use `{flow_name}` / `{state}` placeholders. |
| "Coverage is close enough; I'll skip the gate." | The gate is the acceptance criterion, not a suggestion. | Report the actual number; do not claim done below the gate. |
| "I'll adjust the ViewModel so the test passes." | Tests must fit the production shape; bending production code to tests is a spec change in disguise. | Stop. Do not change production behavior from this skill. |
| "I'll run all six agents at once to save time." | Later stages depend on earlier shapes; parallelizing dependent work produces broken fixtures. | Stage them: fixtures → data → presentation/integration, awaiting each. |
| "The code throws here, so I'll assert that it throws — the test passes." | That encodes the bug as the contract. After the fix the test fails, and it must be rewritten before the fix can land. | Assert the intended behaviour. If it fails, report the defect and leave the test red. |

## Helper Code Snippets

```bash
./gradlew :feature:{NAME}:desktopTest
./gradlew koverHtmlReport
python3 shared/scripts/kmp_check.py {NAME}
```

## Reference Resources

- `references/workflow.md` — the full staged test-generation workflow.
- `.opencode/agent/kmp-test-*.md` — the six staged generators.
- `shared/kmp-patterns.md` — coverage scope and test conventions.

## Supporting Files

| File | Use |
|---|---|
| `references/workflow.md` | the end-to-end test workflow with the context YAML contract |

**Doctrine paths.** `shared/…` and `policies/…` resolve inside this bundle in native installs (vendored by the installer); in an opencode install they live at the kit root, two directories up from this file (`skills/<name>/SKILL.md`).
