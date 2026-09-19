# Decisions

Architecture decision records for this kit. Each one records a choice, the reasoning, and what was
rejected — so a later reader does not re-open a settled question, and an unsound one is auditable.

Format: **Status · Context · Decision · Consequences · Alternatives considered.** Status is
`accepted`, `superseded by NNNN`, or `proposed`. Number sequentially; never edit the reasoning of an
accepted record — supersede it.

These are the kit's own decisions, not the design/build doctrine. Behavioral rules live in
`AGENTS.md`, `shared/`, and the skills.

| # | Decision | Status |
|---|---|---|
| [0001](0001-domain-artifact-path.md) | Domain artifacts: a directory for scope, filenames for role | accepted |
| [0002](0002-domain-reconnaissance.md) | Reconnaissance (FDD steps 1–2) is an optional skill, not a flag | accepted |
| [0003](0003-what-lives-in-the-schema.md) | The OpenSpec schema carries planning artifacts only; the build stays in skills | accepted |
