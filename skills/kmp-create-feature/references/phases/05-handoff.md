# Phase 5: Handoff

**Purpose**: Reconcile the living spec, run the deterministic checker, archive the OpenSpec change, and write the run ledger.

**Prerequisites**: All agents completed successfully, build passing.

---

## Checklist

```
Handoff Progress:
- [ ] Step 5.1: Verify the living spec
- [ ] Step 5.2: Guardrails (grep gate): Rule 11 + first-feature Welcome handoff
- [ ] Step 5.3: Run the checker
- [ ] Step 5.4: Archive the OpenSpec change
- [ ] Step 5.5: Write the run ledger
- [ ] Step 5.6: Generate final report
```

---

## Step 5.1: Verify the Living Spec Exists

```bash
ls -la openspec/specs/{featurename}/spec.md
```

**If spec.md exists**: Proceed to the guardrail checks

**If spec.md missing**: Do NOT invent a spec file. Reconcile the OpenSpec change (`openspec/changes/{change-id}/`) into `openspec/specs/{featurename}/spec.md` first.

---

## Step 5.2: Guardrail Checks (Grep Gate)

Mechanical checks that the feature follows architectural conventions. All must pass.

```bash
# (a) No data→presentation imports (Rule 11)
grep -rEn 'import\s+\S+\.presentation\.' \
  feature/{featurename}/src/commonMain/kotlin/**/data/ \
  && echo "❌ Rule 11 violation: data layer imports from presentation" && exit 1

# (b) No *UiState.kt file (Rule 11)
find feature/{featurename}/src/commonMain/kotlin -name '*UiState.kt' \
  | grep . && echo "❌ Rule 11 violation: *UiState.kt found; collapse into *UiModel.kt" && exit 1

# (c) @Preview exists in UI files
grep -rn "@Preview" feature/{featurename}/src/commonMain/kotlin \
  | grep -v "^Binary" | grep . \
  || echo "⚠️ No @Preview composables found in feature/{featurename}. Add previews per shared/kmp-patterns.md § Previews."

# (d) First-feature Welcome handoff completed (Integration Point 4a).
#     The kit installer placeholder must be gone once the FIRST feature is wired.
#     The build still compiles with a leftover Welcome, so this gate catches it.
find composeApp/src/commonMain/kotlin -name 'WelcomeScreen.kt' | grep . \
  && echo "❌ Welcome handoff incomplete: WelcomeScreen.kt still present — delete it and repoint startDestination (see skills/kmp-create-feature/references/architecture/integration.md § 4a)" && exit 1
grep -rn "WelcomeRoute" composeApp/src/commonMain/kotlin | grep . \
  && echo "❌ Welcome handoff incomplete: WelcomeRoute still referenced in the nav host — set startDestination = {Feature}Route and drop the WelcomeRoute composable" && exit 1
```

**If (a), (b), or (d) fails**: Stop. Surface the violation to the user. Do NOT proceed to handoff. For (d), re-run the integration agent's first-feature handoff (`skills/kmp-create-feature/references/architecture/integration.md` § 4a).

**If (c) finds no previews**: Surface the warning but do NOT block handoff — previews are required but a missing preview does not break the build or architecture. The user may choose to add them via `/kmp-modify-feature`.

**If all pass**: Proceed to Step 5.3.

---

## Step 5.3: Run the Checker

```bash
python3 shared/scripts/kmp_check.py {featurename}
```

**Expected**: zero errors. The checker writes its report to `.kmp/check-report.json`.

**If the checker fails**: Stop. Surface the errors to the user and fix them before archiving the change.

---

## Step 5.4: Archive the OpenSpec Change

```bash
/opsx-archive {change-id}
```

The ephemeral planning files — `proposal.md`, `design.md`, and `tasks.md` under
`openspec/changes/{change-id}/` — are **archived**, not deleted. The living spec
`openspec/specs/{featurename}/spec.md` remains the source of truth.

### What Gets Archived

| File | Purpose | Why Archive |
|------|---------|------------|
| `proposal.md` | Planning document | Change complete; superseded by the reconciled spec |
| `design.md` | Design decisions | Change complete; the living spec carries the outcome |
| `tasks.md` | Task summary | Work complete |

### What Remains

| File | Purpose | Permanent |
|------|---------|-----------|
| `openspec/specs/{featurename}/spec.md` | Living specification | ✅ Source of truth |
| `.kmp/{featurename}/review.md` | Code review results | ✅ If exists |
| `.kmp/{featurename}/fixes.md` | Applied fixes | ✅ If exists |

---

## Step 5.5: Write the Run Ledger

Update `.kmp/run.json` to `phase: done`, recording the reconciled spec path and the
checker result, then release the feature-file guard:

```bash
rm -f /tmp/.kmp-skill-active
```

See `shared/kmp-state.md`.

---

## Step 5.6: Generate Final Report

```markdown
## Feature Complete: {FeatureName}

### Implementation Summary
✅ Data layer implemented
✅ UI layer implemented
✅ Integration complete
✅ Build passing + ktlint formatted
✅ Checker green (`.kmp/check-report.json`)

### Documentation
✅ Living spec: `openspec/specs/{featurename}/spec.md`
✅ OpenSpec change archived
✅ Run ledger: `.kmp/run.json`

### Files Created

#### Feature Module
- `feature/{featurename}/build.gradle.kts`
- `feature/{featurename}/src/commonMain/kotlin/{PKG_PATH}/{featurename}/`
  - `data/model/*.kt`
  - `data/remote/*.kt`
  - `data/datasource/*.kt`
  - `data/repository/*.kt`
  - `presentation/*.kt`
  - `presentation/ui/*.kt`
  - `presentation/navigation/*.kt`
  - `di/*.kt`

#### Integration Points Modified
- `settings.gradle.kts`
- `composeApp/build.gradle.kts`
- `{INIT_KOIN_PATH}`
- `{NAV_HOST_PATH}`

### What's next
- Test navigation: `navController.navigate({FeatureName}Route)`
- Review spec: `openspec/specs/{featurename}/spec.md`

---

> **Next step —** run `/clear` to free the context window (the spec at `openspec/specs/{featurename}/spec.md` and the `DESIGN.md` handoff are durable artifacts — the next skill re-reads them fresh, so clearing loses nothing), then `/kmp-review-feature {featurename}` to validate against Clean Architecture guidelines.
```

---

## Output

Feature creation workflow complete:
- All code implemented
- Build passing
- `openspec/specs/{featurename}/spec.md` is the permanent source of truth
- OpenSpec change archived
- Run ledger at `.kmp/run.json`
- Feature ready for testing
