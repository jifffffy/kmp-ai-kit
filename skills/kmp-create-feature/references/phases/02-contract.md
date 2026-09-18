# Phase 2: Contract + Plan

**Purpose:** Turn the resolved inputs into an explicit, testable contract and a layer
plan, and get the user's approval **before** any code is written.

**When:** After Phase 1 input resolution.

**Exit:** the user approves the contract and the plan. "Looks good" approves this phase
only — not the implementation, not a future phase.

---

## Checklist

```
Contract + Plan Progress:
- [ ] Step 2.1: Write the Token-Aware Brief Contract
- [ ] Step 2.2: Resolve the Platform Profile (Rule 14)
- [ ] Step 2.3: Choose the agent set and execution strategy
- [ ] Step 2.4: Present and WAIT for approval
```

---

## Step 2.1: The Token-Aware Brief Contract

Act as a senior Kotlin Multiplatform engineer who never hardcodes values and follows the
14 architecture rules. Restate the request as five fields — every field resolved from the
spec and the design, never invented:

| Field | Source | Must contain |
|---|---|---|
| **Context** | `proposal.md` | product, audience, why now |
| **Objective** | `specs/<capability>/spec.md` | exactly one capability |
| **Inputs** | spec + `DESIGN.md` | screens, states, tokens, component refs |
| **Constraints** | `shared/kmp-patterns.md` | forbidden dependencies, inviolable rules |
| **Acceptance Criteria** | spec scenarios + checker | quantitative: build green, checker green, 4 integration points wired, coverage gate |

If a field cannot be filled from the inputs, that is a gap — ask one question; do not
guess.

---

## Step 2.2: Resolve the Platform Profile (Rule 14)

Classify the feature from its spec's **Platform Profile & Capabilities** field:

| Profile | Agent set | Notes |
|---|---|---|
| `network` | data + ui + integrator | plain REST/UI feature |
| `platform-capability` | data + **platform** + ui + integrator | device capability (GPS, camera, BLE, biometrics) behind a `commonMain` DataSource |
| `native-view` | data + **platform** + ui + integrator | native view (map, camera preview, WebView) via `expect @Composable` |
| `mixed` | all four | both of the above |

If the spec omits the field, ask once. Full patterns:
`references/architecture/platform.md`.

---

## Step 2.3: Agent set and execution strategy

Record which subagents will run and in what order. Default to **parallel where layers are
independent** (data ∥ ui), then integration last — never run the integrator before the
layers it wires exist.

| Agent | Layer | Runs |
|---|---|---|
| `kmp-data-layer` | models, DataSource, Repository, Ktor | first or parallel (`network`/`mixed`) |
| `kmp-platform` | capability DataSource + per-platform actuals + `platformModule` | first or parallel (`platform-capability`/`native-view`/`mixed`) |
| `kmp-ui-layer` | UiModel, ViewModel, Screens, Navigation | second or parallel |
| `kmp-integrator` | DI + the 4 integration points | last |

Detail and the exact context passed to each agent:
`references/phases/04-implementation.md` → Step 4.0.

---

## Step 2.4: Present and wait

Show the contract, the Platform Profile, and the layer plan. Ask for approval.

**✋ Checkpoint — do not proceed without an explicit OK.**

- **Approve** → proceed to Phase 3 (task plan) with the marker still **off**.
- **Modify** → revise the contract and re-present.
- **Reject** → stop; nothing was written.

---

## Output

- An approved Token-Aware Brief Contract.
- A resolved Platform Profile and agent set.
- An approved layer plan.

Proceed to **Phase 3: Task Plan**.
