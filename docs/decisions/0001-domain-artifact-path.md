# 0001 — Domain artifacts: a directory for scope, filenames for role

**Status:** accepted (supersedes the earlier `openspec/domain.md`)
**Date:** 2026-09-19

## Context

The kit keeps two domain models: one per change (the OpenSpec artifact) and one per project (the
living vocabulary every later change reads). They were named:

```
openspec/domain.md                 the living vocabulary
openspec/changes/<id>/domain.md    a change's delta
```

Reading it back, the naming had four defects — each independently sufficient to change it:

1. **Same basename, two semantic roles.** Only path depth distinguished them, so a sentence saying
   "domain.md" was unreadable. The doctrine itself printed both in a single code block, which is the
   proof: when the document that defines a thing cannot refer to it unambiguously, the name is wrong.
2. **"domain" already named four things** — the skill, the doctrine file, the OpenSpec artifact id, and
   the living file. Reconnaissance (0002) would have made five.
3. **Location asserted an ownership it lacked.** It sat inside `openspec/`, beside `specs/`, but
   OpenSpec neither owns it nor sees it (`openspec validate` ignores it), and the kit is explicit that
   it is *not* a spec. A path inside a tool's directory that the tool does not manage misleads.
4. **The name did not state its role.** `domain.md` reads as a normative model; the file holds a
   vocabulary plus archetype decisions. And `specs/` is a plural directory, so a bare `domain.md`
   beside it read like a *product* rather than a living vocabulary.

## Decision

A directory expresses scope; filenames express role. The living material gets a directory parallel to
`specs/` — both are living and project-wide:

```
openspec/
├── specs/                    living specs (one per capability)
├── domain/                   the project's domain material (kit-owned; OpenSpec does not see it)
│   ├── model.md              the living VOCABULARY
│   └── features.md           the reconnaissance candidate list (0002)
└── changes/<id>/
    └── domain.md             this change's DELTA (the OpenSpec artifact)
```

The per-change artifact keeps its name. It follows OpenSpec's `artifact id → filename` convention, and
renaming it would be fighting the tool; the living model is kit-owned and can be named freely.

## Consequences

- `openspec/domain/model.md` cannot be mistaken for the change artifact: different tree, different
  shape (`domain/` holds more than one file).
- The directory has room to grow — `features.md` has a home, and a subdomain split
  (`domain/billing.md`, `domain/identity.md`) is possible without another rename.
- 25 references updated. `docs/architecture.md`, `AGENTS.md`, the README, `config.yaml`, the schema
  instruction, `kmp_route.py`, `shared/kmp-state.md`, and both domain skills all name the new path.
- Verified `openspec/domain/` does not disturb `openspec list`, `list --specs`, `validate`, or schema
  resolution.
- Cost: one mechanical rename, and any existing project's `openspec/domain.md` must move. No released
  project depends on the old path.

## Alternatives considered

- **Rename only the per-change artifact** (e.g. `domain-delta.md`). Rejected: it breaks OpenSpec's
  convention, and the change artifact is the one bound by the tool.
- **Rename only the living file** (`vocabulary.md`, `glossary.md`). Rejected as insufficient: it fixes
  defect 1, but leaves "domain" overloaded (2) and the ownership claim (3). A directory also gives
  `features.md` a natural home instead of another top-level `openspec/` file.
- **Keep `openspec/domain.md` and rely on prose to disambiguate.** Rejected: the doctrine already
  tried this and failed — it had to print both paths side by side.
- **Put the living model in `specs/` as a spec.** Rejected on a stronger ground than naming: it is not
  a behaviour contract. It emits no SHALL/MUST, and requirements belong to OpenSpec.
