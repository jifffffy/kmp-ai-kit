# Architecture

This kit is a **layered system**, not a pile of prompts. Each layer solves a distinct problem.

```
Instructions   AGENTS.md ............... how every agent must behave
      ▼
Skills         skills/*/SKILL.md ....... reusable capabilities ("how we do task X")
      ▼
Workflows      workflows/* ............. end-to-end orchestration ("when/with-what/what-next/approve")
      ▼
MCP            Penpot MCP tools ........ real read/write access to the file (shared/penpot-mcp-tool-reference.md)
      ▼
Policies       policies/* .............. modes (suggest/review/autofix), approvals, safe set
      ▼
Evals          evals/* ................. golden tests that prove the workflows work
```
…with `shared/` as the single source of truth (tool reference, API gotchas, token schema, naming,
state, modes, visual self-review, report schemas, pipeline schema, capability probe) transcluded everywhere, and `prompts/` + `templates/` as the human/onboarding surface.

## The four working layers

On top of that stack, work moves through four layers with four owners. The boundary is the point: each
layer's *completion* means something different, and blurring them is how a document's "done" gets
mistaken for code's "verified".

| Layer | Owner | Owns | Completes when | Detail |
|---|---|---|---|---|
| **Planning** | OpenSpec (`/opsx-*`) | requirements | the artifact file exists | `openspec/schemas/kmp/schema.yaml` |
| **Domain** | `kmp-domain-recon` (once), `kmp-domain-model` (per change) | the concepts | the model is written and approved | `shared/domain-modeling.md` |
| **Design** | Penpot (`penpot-*`) | visual intent | the `DESIGN.md` handoff exists | `AGENTS.md` (Design layer) |
| **Build** | KMP skills (`kmp-*`) | code | **a command passes** (build, checker, app launch) | `shared/kmp-patterns.md`, `shared/kmp-runtime-verification.md` |

The chain is **`spec → domain → design?(UI) → build`**, and the order is not a judgment call: the
schema declares it, so OpenSpec blocks `domain` until the spec is done and blocks `tasks` until the
domain model is. `shared/scripts/kmp_route.py` reads those states and writes `.kmp/route.json`.

**Why the build layer is not in the schema** — it would replace a command-verified gate with file
existence — is decision [0003](decisions/0003-what-lives-in-the-schema.md).

## Decisions

Architecture decisions are recorded in [`docs/decisions/`](decisions/): the choice, the reasoning, and
what was rejected, so a settled question is not re-opened and an unsound one is auditable.

## Skill vs Workflow vs Agent vs MCP
- **Skill** — a packaged, versionable capability. Answers *"how is this task done well?"*
- **Workflow** — a sequence that composes skills. Answers *"when, with what data, what next, when to approve?"*
- **Agent** — the runtime actor (opencode) that plans, calls tools, keeps state.
- **MCP** — the protocol exposing real tools/context to the agent (Penpot; OpenSpec is a CLI).

A Skill is knowledge; a Workflow is behavior; MCP is access; the Agent runs it all.

## Why this kit exists
Generic "prompt → pretty UI" is commoditized and produces off-system, un-editable output. Likewise,
"prompt → code" produces a module that compiles and violates every layer boundary. The opportunity is
for agents to operate on **real, structured, governed** artifacts — a real design file, a real spec, a
real architecture gate. This kit supplies the missing layers — instructions, governance skills,
orchestration, policies, evals — on top of Penpot's open data + MCP and OpenSpec's artifact model.

## Skill set

**Build layer (10)** — `kmp-router`, `kmp-init`, `kmp-domain-recon`, `kmp-domain-model`,
`kmp-create-feature`, `kmp-modify-feature`, `kmp-review-feature`, `kmp-test-feature`,
`kmp-bridge-swift`, `kmp-using-design-system`.

**Design layer (13)** — `penpot-router`, `penpot-foundations`, `penpot-component-factory`,
`penpot-build-screen`, `penpot-build-from-code`, `penpot-build-deck`, `penpot-document-handoff`,
`penpot-design-md`, `penpot-audit-accessibility`, `penpot-audit-tokens`,
`penpot-design-to-code-review`, `penpot-migrate`, `penpot-rename-layers`.

## Roadmap — deferred capabilities (priority order, follow `shared/SKILL-template.md`)

1. **Verification evidence after archive** (decision 0003): `.kmp/run.json` is git-ignored, so an
   archived change cannot answer *"was this verified?"*. A `verification` artifact would carry it —
   evidence only, never a gate. Deferred until a real need appears.
2. **Subdomain splits of the living model** (decision 0001): `openspec/domain/model.md` is a single
   file; past roughly 300 lines, `domain/<subdomain>.md` with cross-links. Not needed at current scale.
3. **Icon pipeline**: SVG → `penpot.createShapeFromSvg` / `createShapeFromSvgWithImages` → tokenized,
   named icon components; `import_image` covers raster assets in local mode.
4. **Deeper design↔code**: make `generateMarkup`/`generateStyle` the canonical extraction in
   `penpot-design-to-code-review` beyond plain components (they are only verified there today), and
   feed Code Connect-style mappings from real repos.
5. **Connected libraries**: everything currently assumes `penpot.library.local`; real teams consume
   shared libraries via `penpot.library.connected` — discovery, instancing, and governance across
   library boundaries.
6. **Prototyping/interactions**: flows and interactions exist in the Plugin API; a skill (or a
   `penpot-build-screen` extension) could wire navigation between the screens the kit builds — partially
   delivered by `penpot-build-deck` (flows + navigate-to interactions for decks).
7. Localization/RTL and data-viz remain deferred (niche/specialized).
