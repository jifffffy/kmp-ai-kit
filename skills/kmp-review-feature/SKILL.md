---
name: kmp-review-feature
description: "Audit a Kotlin Multiplatform feature against the architecture rules: run the deterministic checker and consume its report, then review the judgment-call rules, spec compliance and UI file organization. Read-only — reports, never edits. Triggers: 'review this feature', 'check the dashboard', 'audit feature', 'is this feature correct'."
disable-model-invocation: false
version: 0.1.0
audiences: [kmp-engineer, design-engineer]
mode-default: suggest
requires:
  - shared/kmp-patterns.md
  - shared/kmp-agent-base.md
  - shared/report-schemas
  - policies/approval-checkpoints.md
---

# kmp-review-feature — the checker decides the mechanized half

Audits one feature and produces a severity-ranked report. It **never edits** the
feature it reviews. Its central design choice: the greppable half of the 14 rules is
already mechanized in `shared/scripts/kmp_check.py` — this skill runs it and reports
its verdict, it does **not** re-derive those checks by grepping. A review and a CI run
must not be able to disagree.

## The One Rule That Matters Most

**Run the checker; never re-implement it.** Duplicating a mechanized rule in prose
produces a second, disagreeing verdict. The report is the source of truth for
mechanized rules; this skill only adds the judgment calls no grep can settle.

## Tool surface

The checker is `shared/scripts/kmp_check.py`; it writes `.kmp/check-report.json` and
exits non-zero on any `error`. Rules and their mechanized/judgment split are defined
in `shared/kmp-patterns.md`. Report shape: `shared/report-schemas/`.

## The Token-Aware Brief Contract

Restate: **Context** (feature, spec version) / **Objective** (audit one feature) /
**Inputs** (the feature dir, its spec, the checker report) / **Constraints**
(read-only; no edits; report every finding verbatim) / **Acceptance criteria** (every
mechanized violation surfaced, judgment rules assessed, spec gaps named).

## Mandatory Workflow

`references/workflow.md` carries the detail. In outline:

- **Phase 0 — context load (read-only).** Glob the feature's Kotlin; read its spec at
  `openspec/specs/{featurename}/spec.md` and its Penpot `DESIGN.md` if present.
- **Phase 1 — mechanized checks.** Run
  `python3 shared/scripts/kmp_check.py {featurename}`, read
  `.kmp/check-report.json`, and report every violation for this feature (plus any
  repo-scoped finding) **verbatim**. Do not add, reorder, or re-word findings.
- **Phase 2 — judgment rules.** Assess the rules the checker deliberately does not
  mechanize (Rule 1 interface/impl semantics, Rule 2 fallibility, Rule 4, Rule 10,
  Rule 14) with reasoning per finding.
- **Phase 3 — spec + design compliance.** Compare the implementation against the
  spec's requirements/scenarios and, where a `DESIGN.md` exists, the design intent.
- **Phase 4 — UI file organization.** Check the `Screen.kt` allowlist, `components/`
  placement, and preview presence.
- **Phase 5 — report.** Emit the severity-ranked report. **Exit:** findings listed;
  zero edits made.

## Critical Rules

1. **Read-only.** No Edit/Write to `feature/**` or any source. The report is the only
   artifact.
2. **Never re-derive a mechanized check.** Run the script and quote its output.
3. **Every mechanized finding is reported verbatim** — including ones the reviewer
   thinks are fine. Severity is the checker's, not the reviewer's.
4. **Separate mechanized from judgment.** Label each finding with which it is.
5. **Name the spec gap, don't invent a requirement.** If code and spec disagree,
   report the disagreement; the spec is OpenSpec's to change.
6. **A never-run feature is a blocking finding.** If the feature has no runtime verdict in
   `.kmp/run.json` (or was last run before its DI or a `@Serializable` model changed), report it as
   an `error`, not a note. `archTest` green does not clear it — the checker is static and cannot see
   the composed Koin graph, a serializer contract against a real payload, or a runtime cast. Three
   such bugs have shipped through a fully green gate. See `shared/kmp-runtime-verification.md`.
   The reviewer does not run the app itself (read-only); it reports that the run is owed, and names
   the specific risk (DI change / model change) that makes it mandatory.

## Domain Architecture

Report severities mirror the checker's `error` / `warning` and add `info` for
judgment observations. Report fields are defined in `shared/report-schemas/` so the
review, the CI gate and any downstream consumer read the same shape.

## Modes & Policies

`mode-default: suggest`. This skill reports and proposes; it applies nothing, not
even the safe set. See `policies/approval-checkpoints.md`.

## State Management

No ledger of its own; the report lands in `.kmp/check-report.json`. A review invoked
inside a create/modify run appends its findings to that run's `.kmp/run.json` entry.
See `shared/kmp-state.md`.

## User Checkpoints

| After phase | Artifacts shown | What we ask |
|---|---|---|
| 1 | the checker report for this feature | confirm the scope of judgment review |
| 5 | the full severity-ranked report | decide which findings to act on (in a later modify run) |

## Naming Conventions

Findings use the checker's check-ids (e.g. `R3`, `R11a`, the four integration
points). Do not rename them; downstream consumers match on them.

## Anti-Rationalization Table

| Excuse the model makes | Why it's wrong | Countermeasure that halts the flow |
|---|---|---|
| "I can spot these violations faster by grepping." | A second implementation of a mechanized rule will eventually disagree with the first. | Run `kmp_check.py`; report its output verbatim. |
| "This reported violation looks acceptable, I'll drop it." | The reviewer does not get to overrule the gate; silent suppression is how drift hides. | Report every finding; if a rule is wrong, that is a rules change, not a review decision. |
| "The code is clearly right, I'll skip the spec comparison." | Spec/code divergence is the failure this review exists to catch. | Read the spec and report divergences explicitly. |
| "I'll fix the small issues while I'm here." | A review that edits is no longer a review, and it bypasses the feature guard and the modify gate. | Report only; route fixes to `kmp-modify-feature`. |
| "`archTest` is green, so I won't raise the missing runtime run." | The three worst bugs found in practice all passed the static gate and crashed on launch. | Raise it as an `error` finding whenever DI or a `@Serializable` model is in play (`shared/kmp-runtime-verification.md`). |
| "The feature has tests, so behaviour is covered." | The bugs that matter live in composition, not in any single unit. | Tests are necessary, not sufficient; check the runtime verdict. |

## Helper Code Snippets

```bash
# the mechanized gate (run from the host project root)
python3 shared/scripts/kmp_check.py {NAME}
python3 shared/scripts/kmp_check.py {NAME} --json-only
python3 shared/scripts/kmp_check.py --all --baseline   # scan tier: errors as warnings, exit 0
```

## Reference Resources

- `references/workflow.md` — the full review workflow (ported and re-homed).
- `.opencode/agent/kmp-code-reviewer.md` — the reviewer subagent.
- `shared/kmp-patterns.md` — the mechanized/judgment split per rule.
- `shared/report-schemas/` — report shapes.

## Supporting Files

| File | Use |
|---|---|
| `references/workflow.md` | the end-to-end review workflow — read before Phase 2 |

**Doctrine paths.** `shared/…` and `policies/…` resolve inside this bundle in native installs (vendored by the installer); in an opencode install they live at the kit root, two directories up from this file (`skills/<name>/SKILL.md`).
