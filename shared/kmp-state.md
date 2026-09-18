# KMP state & resumability

The KMP layer's ledger. One file, `.kmp/run.json`, authoritative for every in-flight
KMP build. It is the KMP counterpart of the design layer's `RUN_ID` ledger
(`shared/state-management.md`) and exists for the same reason: after context
truncation, re-read the ledger and re-derive reality **before** continuing.

`.kmp/` is tooling output — git-ignored. Do not commit it.

## Ledger shape

```json
{
  "run_id": "kmp-2026-09-18-dashboard",
  "capability": "dashboard",
  "spec_path": "openspec/specs/dashboard/spec.md",
  "change_id": "add-dashboard",
  "design_path": "openspec/…/DESIGN.md",
  "app_module": "composeApp",
  "pkg_prefix": "com.example.kmp",
  "phase": 3,
  "marker": true,
  "layers": {
    "data": "done",
    "platform": "skipped",
    "ui": "pending",
    "integration": "pending"
  },
  "checker": { "ran_at": "…", "errors": 0, "warnings": 0, "report": ".kmp/check-report.json" },
  "updated_at": "2026-09-18T20:00:00Z"
}
```

## Rules

1. **One run per capability.** Starting a second run for the same capability requires
   the first to be archived or explicitly abandoned — never run two in parallel.
2. **Write the ledger before a mutation, not after.** A crash mid-step must leave
   enough to know which step was in flight.
3. **`marker: true` means feature files are editable.** The marker on disk
   (`/tmp/.kmp-skill-active`) is derived from this field, never set independently.
4. **Re-derive on resume.** A resumed run must run
   `python3 shared/scripts/kmp_check.py --baseline` and re-read the OpenSpec change
   before trusting the ledger's `phase`.
5. **Spec and design are references, not copies.** The ledger stores paths; the
   authoritative content stays in `openspec/` and Penpot. Never inline spec text.

## Authority order on conflict

1. `openspec/specs/` — requirements.
2. The Penpot design artifact — visual intent.
3. `shared/kmp-patterns.md` + `kmp_check.py` — architecture rules.
4. `.kmp/run.json` — progress only; never a source of requirements.

If the ledger disagrees with the spec, the spec wins and the ledger is corrected.
