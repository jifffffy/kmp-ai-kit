# Runtime Verification — `archTest` PASS is not "the app works"

The deterministic checker (`shared/scripts/kmp_check.py`, wired as `./gradlew archTest`) is a
**static** gate. It reads source. It cannot see the assembled Koin graph, a `@Serializable`
contract against a real payload, or a cast that only executes on a comparison. A feature can pass
every check, pass every unit test, and still **crash the host app at launch on all three platforms**.

This file defines the runtime gate that closes that gap. It is mandatory for
`kmp-create-feature` Phase 5 and for `kmp-review-feature` when a feature has never been run.

---

## The failure class this exists for

Three real bugs, each of which passed `archTest` and unit tests, and each of which was fatal:

| Bug | Shape | Why static checks missed it |
|---|---|---|
| `NetworkErrorModel` nullable fields **without defaults** | `@Serializable` treats a nullable property with no default as **required**; any error body omitting it threw `MissingFieldException`, and the adapter collapsed every failure into a generic network error | The model compiles and its own test only ever supplied both fields |
| `ErrorModel.MessageCode.equals()` did `other as MessageCode` | Comparing a `MessageCode` to any other `ErrorModel` threw `ClassCastException` — hit by a DataSource retry guard | `equals` is only exercised when two *different* error types are compared; the unit test compared two `MessageCode`s |
| `viewModelOf(::ViewModel)` with a defaulted, non-injectable parameter | Koin resolves **every** constructor parameter — a Kotlin default does not exempt it — so it asked for a `Function0<Long>` binding and threw `NoDefinitionFoundException` from `startKoin` | Nothing about the class is wrong; the defect is in the **composed graph**, which only exists at runtime |

The common thread: **every one of these is a composition/contract failure, not a source failure.**
Static analysis reads one file at a time; these emerge from putting the files together at runtime.

---

## The gate: run the desktop target

The desktop target is the cheapest runnable surface — no emulator, no simulator, no signing, and
it initialises the **same** shared Koin graph and the **same** serialization stack the mobile
targets use. A startup crash surfaces here identically.

```bash
# FIRST RUN — the correct task. It launches the window.
./gradlew :composeApp:run

# NOT this one. `desktopRun` ignores the `compose.desktop { application { mainClass } }`
# block and fails with "No main class specified and classpath is not an executable jar".
./gradlew :composeApp:desktopRun
```

The app must reach its start destination and render. A window that opens and immediately closes,
or a stack trace mentioning Koin / `MissingFieldException` / `ClassCastException`, is a **failed**
gate — regardless of what `archTest` said.

**If the window cannot be inspected visually** (headless CI, no screen-recording permission for the
terminal), a render smoke test is the accepted substitute:

```kotlin
// renders the real ScreenRoot off-screen and writes a PNG — proves composition + the Koin graph
@Test fun `leaderboard screen composes without crashing`() { /* setContent + captureToImage */ }
```

Write the PNG under `build/smoke/` and **look at it**. A screenshot nobody looks at is not
verification — it is a file. This is a fallback, not an equivalent: it exercises composition and
DI but not the platform windowing layer.

---

## When to run it

| Situation | Gate |
|---|---|
| A feature is created | mandatory before Phase 5 handoff |
| A feature's DI graph changes (any `di/` edit) | mandatory |
| A `@Serializable` model changes | mandatory |
| A shared `core/` type changes (`Either`, `ErrorModel`, `UiState`) | mandatory across every feature |
| Ordinary UI-only change | optional (build + checker is enough) |
| Reviewing a feature that has never been run | mandatory, reported as a blocking finding |

---

## Preflight — verify a capability before you depend on it

The same discipline applies to the tools the work depends on. "Configured" is not "available":

```bash
# In the kit itself:
node scripts/dev/preflight.mjs --url "http://localhost:9001/mcp/stream?userToken=<key>"

# In a scaffolded project (no scripts/ copied in) — the same three probes, by hand:
U="http://localhost:9001/mcp/stream?userToken=<key>"
curl -s -o /dev/null -w "transport %{http_code}\n" -X POST "$U" \
  -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"probe","version":"0"}}}'
```

A 200 on `initialize` proves only the **server** is up. It says nothing about the in-app plugin —
which is the failure that actually blocks work, and which **only a real tool call reveals**. In
multi-user mode the plugin's token and the client's token must match **byte-for-byte**; a single
wrong character produces "No Penpot instance connected for user token" while the endpoint answers
happily. So probe with an actual `execute_code` returning `penpotUtils.getPages()`, and only treat
the capability as available when that returns data.

The general rule, worth applying to any capability a task leans on:

> **Probe it once, at the start, with a real call.** "The target compiles" ≠ "the target runs";
> "the MCP server is configured" ≠ "the plugin is connected"; "the model has the tool" ≠ "the tool
> works". Five minutes of probing beats hours of debugging a mistaken assumption.

---

## Recording the verdict

A run's `.kmp/run.json` entry carries the result:

```json
"runtime": { "target": "desktop", "command": "./gradlew :composeApp:run", "result": "launched", "at": "…" }
```

`result` is one of `launched` | `render-smoke` | `not-run`. **`not-run` is a first-class outcome:**
record it and say so in the handoff rather than implying the feature was verified. An honest
`not-run` is worth more than a false green — and it is the difference between a gate and a ritual.

## Anti-rationalization

| Excuse | Why it's wrong | Countermeasure |
|---|---|---|
| "`archTest` is green, so the feature is fine." | The checker is static; the three bugs above all passed it. | Run the target. The gate is a launch, not a lint. |
| "Unit tests pass." | The failing behaviours require composing the real graph / real payload. | Green tests are necessary, not sufficient. |
| "It compiles, that's enough for a UI change." | "Compiles" is exactly what the desktop target did before its modules were filled in. | If DI or serialization is touched, run it. |
| "I can't see the window, so I'll skip it." | Unverifiable ≠ verified. | Render smoke test, and record `render-smoke`, not `launched`. |
| "The checker doesn't flag it, so the rule can't matter." | The checker mechanizes what is mechanizable. Composition failures are not. | Read this file; run the target. |
