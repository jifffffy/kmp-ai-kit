# Task Template

Template for individual task files in `openspec/changes/{change-id}/task-{N}-{title}.md`

---

```markdown
# Task {N}: {Title}

**Status:** ⏳ Pending | 🔄 In Progress | ✅ Done
**Group:** Data | UI | Integration
**Priority:** High | Medium | Low
**Agent:** data-layer | ui-layer | integrator

## Objective
{One sentence describing what this task accomplishes}

## Implementation Details
{Step-by-step implementation instructions}

1. {Step 1}
2. {Step 2}
3. {Step 3}

## Files Created/Modified

### ADDED Files
- `{path/to/new/file.kt}` - {Brief description}

### MODIFIED Files
- `{path/to/existing/file.kt}` - {What change is made}

## Code Reference
- [kmp-patterns.md](../../../../shared/kmp-patterns.md) § {section}
- [architecture/{layer}.md](../architecture/{layer}.md)

## Acceptance Criteria

<!-- Include scenarios for UI/behavior tasks, skip for foundation tasks -->

#### Scenario: {TaskBehavior} (if applicable)
- GIVEN {precondition}
- WHEN {action}
- THEN {expected outcome}

### Verification Checklist
- [ ] {Criterion 1}
- [ ] {Criterion 2}
- [ ] {Criterion 3}

## Dependencies
- Task {N}: {dependency description}
- {Other dependency}
```

---

## Guidance

### When to Include Scenarios

| Task Type | Include Scenarios? |
|-----------|-------------------|
| UI screens, user interactions | ✅ Yes |
| ViewModel behavior | ✅ Yes |
| Navigation flows | ✅ Yes |
| Module structure, build config | ❌ Skip |
| DI setup | ❌ Skip |
| Pure data models | ❌ Skip |

### UI Tasks: Required Checklist Items

Every UI task verification checklist must include:
- [ ] `@Preview` added for each top-level `@Composable` (private, same file, wrapped in `XTheme { ... }`)

### Agent Assignment

| Group | Agent | Example Tasks |
|-------|-------|---------------|
| Data | data-layer | Models (DTOs), Resources, DataSource, Repository (`Either<DTO>`) |
| UI | ui-layer | Single `{Feature}UiModel`, ViewModel, Screens (ScreenRoot takes `uiModel`), Navigation |
| Integration | integrator | DI module, 4 integration points (+ first-feature Welcome handoff) |

### Status Flow

```
⏳ Pending → 🔄 In Progress → ✅ Done
```

Agent updates status when starting and completing task.
