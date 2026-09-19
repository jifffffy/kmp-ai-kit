# Phase 3: Task Generation

**Purpose**: Break down the OpenSpec spec into implementation-ready tasks with agent assignments. Tasks live in `openspec/changes/{change-id}/tasks.md` and are executed by the KMP skills — they are not written into PRD files.

**Prerequisites**: the OpenSpec spec and layer plan confirmed by user.

---

## Checklist

```
Task Generation Progress:
- [ ] Step 3.1: Determine task count based on complexity
- [ ] Step 3.2: Generate structured task files
- [ ] Step 3.3: Define task groups for validation
- [ ] Step 3.4: Create summary file (tasks.md)
- [ ] Step 3.5: Request user confirmation
```

---

## Step 3.1: Determine Task Count

Based on spec complexity:

### Simple (UI-only, no API)
- **Tasks**: 3-5
- **Groups**:
  - Group 1: Foundation + UI (2-3 tasks)
  - Group 2: Integration (1-2 tasks)

### Medium (CRUD with API)
- **Tasks**: 6-10
- **Groups**:
  - Group 1: Foundation + Data (3-4 tasks)
  - Group 2: Presentation + UI (2-3 tasks)
  - Group 3: Integration (1-2 tasks)

### Complex (Multiple screens/entities)
- **Tasks**: 10-15
- **Groups**:
  - Group 1: Foundation + Data (4-6 tasks)
  - Group 2: Presentation + UI (4-6 tasks)
  - Group 3: Integration (2-3 tasks)

---

## Step 3.2: Generate Task Files

Create individual task files in `openspec/changes/{change-id}/`:
- `task-1-{title}.md`
- `task-2-{title}.md`
- etc.

Use template: `skills/kmp-create-feature/references/templates/task-template.md`

### Agent Assignment

| Group | Agent | Tasks |
|-------|-------|-------|
| Data | `kmp-data-layer` | Module structure, models, DataSource, Repository, Ktor Resources. *First* check `data.app` for an existing shared endpoint/datasource to **reuse** (cross-feature remote — see `skills/kmp-create-feature/references/architecture/data.md` → "Shared remote data"); if a second feature would re-declare the same endpoint/wire model, hoist to `data.app` instead of duplicating. |
| Platform *(Rule 14, tag ≠ `network`)* | `kmp-platform` | Capability DataSource interface + per-platform actuals (android/ios/desktop) + `expect/actual val platformModule` |
| UI | `kmp-ui-layer` | UiModel, ViewModel, Screen composables, Navigation (+ `expect/actual` native view for `native-view`/`mixed`) |
| Integration | `kmp-integrator` | DI module, 4 integration points (+ first-feature Welcome handoff; + `platformModule` registration for Rule 14) |

**Platform Profile (Rule 14)**: if the spec's Platform Profile is `platform-capability` / `native-view` / `mixed`, add a **Platform** group with `kmp-platform` tasks. The **module-structure** task ("Foundation") moves to whichever agent owns the scaffold (see Phase 4, Step 4.0): `kmp-data-layer` for `network`/`mixed`, else `kmp-platform`, else `kmp-ui-layer`. A `network` feature keeps the original three groups unchanged.

### Scenario Guidance

| Task Type | Include Scenarios? |
|-----------|-------------------|
| UI tasks, ViewModel behavior, navigation | ✅ Yes |
| Module structure, build config, DI setup, pure data models | ❌ Skip |

---

## Step 3.3: Define Task Groups for Validation

### Group 1: Data Layer
- **Agent**: `kmp-data-layer`
- **Tasks**: Module structure, models, DataSource, Repository, Ktor Resources
- **Validate after**:
  ```bash
  ./gradlew :feature:{featurename}:assembleAndroidMain
  ```

### Group 2: UI Layer
- **Agent**: `kmp-ui-layer`
- **Tasks**: UiModel, ViewModel, Screen composables, Navigation
- **Validate after**:
  ```bash
  ./gradlew :feature:{featurename}:assembleAndroidMain
  ```

### Group 3: Integration
- **Agent**: `kmp-integrator`
- **Tasks**: DI module, 4 integration points, first-feature Welcome handoff (delete `WelcomeScreen.kt` + repoint `startDestination` when this is the first feature)
- **Validate after**:
  ```bash
  ./gradlew assembleDebug
  ./gradlew ktlintFormat
  ```

---

## Step 3.4: Create Summary File

Create `tasks.md` as overview:

```markdown
# Tasks: {FeatureName}

## Summary
- **Total Tasks**: {N}
- **Complexity**: {Simple/Medium/Complex}
- **Groups**: Data ({X}) | UI ({Y}) | Integration ({Z})

## Task List

### Group 1: Data Layer (kmp-data-layer)
- [ ] Task 1: {title}
- [ ] Task 2: {title}

### Group 2: UI Layer (kmp-ui-layer)
- [ ] Task 3: {title}
- [ ] Task 4: {title}

### Group 3: Integration (kmp-integrator)
- [ ] Task 5: {title}
```

**These checkboxes are the change's completion record — keep them true.** OpenSpec's archive step
reads them: archiving with unchecked boxes warns "0/N tasks", and forcing past that warning leaves
a permanent, false "work incomplete" record in the archived change. Tick a box in the **same step**
that completes the task — never batch-tick at the end, and never tick a box for work that was
skipped (mark it `- [ ] ~~Task 5: …~~ (skipped: <reason>)` and say so in the handoff instead).

| When | Action |
|---|---|
| A task's work lands and its build + checker pass | mark `- [x]` immediately |
| A task is deliberately dropped | leave the box unchecked, strike the text, name the reason |
| Phase 5 handoff | every box is either `[x]` or explicitly struck-through — then archive |

If you reach Phase 5 with unticked boxes, **do not archive yet**: either finish the task or
explicitly strike it. Archiving an inconsistent `tasks.md` is worse than not archiving.

---

## Step 3.5: Request Confirmation

1. **Display first 2-3 task files** as examples

2. **Display `tasks.md`** overview

3. **Show summary**:
   ```
   "{X} tasks in {Y} groups (Data: {N}, UI: {M}, Integration: {K})"
   ```

4. **Ask** — emit as the very last line of output, styled to catch the eye:
   ```
   ---

   > **Next step —** review the tasks at `openspec/changes/{change-id}/tasks.md` and confirm to proceed with implementation, or request changes.
   ```

5. **Wait for user approval** before proceeding to Phase 4

---

## Output

After user confirms tasks:
- Individual task files saved to `openspec/changes/{change-id}/task-{N}-{title}.md`
- Summary file saved to `openspec/changes/{change-id}/tasks.md`
- Agent assignments clear for each task
- Ready to proceed to **Phase 4: Implementation**
