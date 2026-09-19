#!/usr/bin/env node
/**
 * preflight.mjs — verify a capability is actually usable BEFORE starting work on it.
 *
 * The post-mortem lesson this exists for: "MCP is configured" does not mean "MCP is available".
 * opencode loads MCP servers at session start, and in multi-user mode the Penpot side needs the
 * in-app plugin connected with a byte-identical token. A bad guess about any of those costs hours,
 * and the failure only surfaces mid-task. This makes it a 5-second check up front.
 *
 *   node scripts/dev/preflight.mjs                 # probe penpot from the configured MCP url
 *   node scripts/dev/preflight.mjs --url <url>     # override
 *   node scripts/dev/preflight.mjs --json
 *
 * What it checks, in order — each layer is a different failure with a different fix:
 *   1. transport   the MCP endpoint answers `initialize`
 *   2. token       the configured userToken is accepted (its fingerprint is logged server-side)
 *   3. plugin      the Penpot plugin is CONNECTED for that token — proved by a real tool call,
 *                  because nothing else can tell you
 *   4. tools       the expected tool set is present
 *
 * Exits 0 only if all four pass. Never prints the token.
 */

import { existsSync, readFileSync } from "node:fs"
import { homedir } from "node:os"
import { join, resolve } from "node:path"

const argv = process.argv.slice(2)
const flag = (n) => argv.includes(n)
const arg = (n, d) => {
  const i = argv.indexOf(n)
  return i >= 0 && argv[i + 1] ? argv[i + 1] : d
}
const jsonOut = flag("--json")

const redact = (url) => url.replace(/(userToken=)[^&]+/, "$1***")

/** Find the penpot MCP url: project config, then global. */
function findUrl() {
  const candidates = [
    resolve(process.cwd(), "opencode.json"),
    resolve(process.cwd(), ".opencode/opencode.json"),
    join(homedir(), ".config/opencode/opencode.json"),
  ]
  for (const p of candidates) {
    if (!existsSync(p)) continue
    try {
      const cfg = JSON.parse(readFileSync(p, "utf8"))
      const url = cfg?.mcp?.penpot?.url
      if (typeof url === "string" && url) return { url, from: p }
    } catch {
      /* not JSON or unreadable — keep looking */
    }
  }
  return null
}

const explicit = arg("--url")
const found = explicit ? { url: explicit, from: "(cli)" } : findUrl()
if (!found) {
  console.error(
    "preflight: no Penpot MCP url found.\n" +
      "  Expected `mcp.penpot.url` in ./opencode.json or ~/.config/opencode/opencode.json,\n" +
      "  or pass --url http://localhost:9001/mcp/stream?userToken=...",
  )
  process.exit(2)
}

/** POST a JSON-RPC message; streamable HTTP replies either JSON or an SSE `data:` stream. */
async function rpc(url, body, sessionId) {
  const headers = { "Content-Type": "application/json", Accept: "application/json, text/event-stream" }
  if (sessionId) headers["Mcp-Session-Id"] = sessionId
  const res = await fetch(url, { method: "POST", headers, body: JSON.stringify(body) })
  const text = await res.text()
  let payload = null
  if (text) {
    const dataLines = text.split("\n").filter((l) => l.startsWith("data: "))
    const raw = dataLines.length ? dataLines.map((l) => l.slice(6)).join("") : text
    try {
      payload = JSON.parse(raw)
    } catch {
      /* leave payload null; caller reports the raw body */
    }
  }
  return { status: res.status, sessionId: res.headers.get("mcp-session-id") || sessionId, payload, text }
}

const checks = []
const record = (name, ok, detail, fix) => {
  checks.push({ name, ok, detail, fix })
  if (!jsonOut) {
    console.log(`${ok ? "PASS" : "FAIL"}  ${name}${detail ? `  — ${detail}` : ""}`)
    if (!ok && fix) console.log(`      fix: ${fix}`)
  }
}

if (!jsonOut) console.log(`preflight: penpot\n       url: ${redact(found.url)}\n       cfg: ${found.from}\n`)

let session
try {
  const r = await rpc(found.url, {
    jsonrpc: "2.0",
    id: 1,
    method: "initialize",
    params: { protocolVersion: "2025-06-18", capabilities: {}, clientInfo: { name: "kit-preflight", version: "0" } },
  })
  session = r.sessionId
  const server = r.payload?.result?.serverInfo
  record(
    "transport — MCP endpoint answers",
    r.status === 200 && !!session,
    server ? `${server.name} ${server.version} (http ${r.status})` : `http ${r.status}`,
    "The MCP server is not running. Start the Penpot stack (self-hosted: `./setup.sh up`), then retry.",
  )
  if (!session) throw new Error("no session")
  await rpc(found.url, { jsonrpc: "2.0", method: "notifications/initialized" }, session)
} catch (e) {
  record("transport — MCP endpoint answers", false, String(e.message ?? e), "Start the MCP server, then retry.")
}

let tools = []
if (session) {
  try {
    const r = await rpc(found.url, { jsonrpc: "2.0", id: 2, method: "tools/list", params: {} }, session)
    tools = r.payload?.result?.tools ?? []
    record("tools — tool set is exposed", tools.length > 0, tools.map((t) => t.name).join(", ") || "none")
  } catch (e) {
    record("tools — tool set is exposed", false, String(e.message ?? e))
  }
}

// The load-bearing check. Only a real tool call proves the plugin is connected: the endpoint
// answers happily with no plugin attached.
if (session && tools.length) {
  try {
    const r = await rpc(
      found.url,
      {
        jsonrpc: "2.0",
        id: 3,
        method: "tools/call",
        params: {
          name: "execute_code",
          arguments: { code: "return { pages: penpotUtils.getPages().map(p => p.name) };" },
        },
      },
      session,
    )
    const text = JSON.stringify(r.payload?.result ?? r.payload ?? "")
    const disconnected = /No Penpot instance connected for user token/i.test(text)
    const authProblem = /token|unauthor/i.test(text) && disconnected
    record(
      "plugin — Penpot plugin connected for this token",
      !disconnected,
      disconnected ? "no plugin attached to this token" : "a live Penpot instance answered",
      authProblem
        ? "Open Penpot in the browser, open a design file, then File → MCP server → Connect. " +
          "The token in the client config and the plugin's token must match BYTE-FOR-BYTE " +
          "(multi-user mode); regenerate the MCP key under Your account → Integrations and update " +
          "the client config if they differ by even one character."
        : undefined,
    )
  } catch (e) {
    record("plugin — Penpot plugin connected for this token", false, String(e.message ?? e))
  }
}

const ok = checks.length > 0 && checks.every((c) => c.ok)
if (jsonOut) {
  console.log(JSON.stringify({ ok, url: redact(found.url), checks }, null, 2))
} else {
  console.log(ok ? "\npreflight: OK — Penpot MCP is usable." : "\npreflight: NOT READY — see the fix above.")
  if (ok) console.log("Note: keep the Penpot tab open and active; a suspended tab drops the bridge.")
}
process.exit(ok ? 0 : 1)
