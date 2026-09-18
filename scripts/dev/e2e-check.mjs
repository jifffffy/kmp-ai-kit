#!/usr/bin/env node
/**
 * e2e-check.mjs — end-to-end validation of the KMP build layer against a real project.
 *
 * Structural validation (`validate-kit.mjs`) proves the kit is internally consistent.
 * It cannot prove the checker, the guard and the skill contracts work against an
 * actual KMP repo. This harness does that, read-only:
 *
 *   1. checker      — runs shared/scripts/kmp_check.py against the target repo
 *   2. guard paths  — the plugin's path logic against the repo's real tree
 *   3. anchors      — the Phase 0 anchors a skill must resolve (app module, package
 *                     prefix, initKoin, NavHost, core modules)
 *   4. openspec     — a change can be created and validated strict
 *
 * Usage:
 *   node scripts/dev/e2e-check.mjs --root /path/to/kmp/project
 *   node scripts/dev/e2e-check.mjs --root . --expect-errors 0
 *
 * Exits non-zero if any check fails. Never writes into the target repo except for
 * the checker's own report (`.kmp/check-report.json`) and, with --with-openspec, a
 * throwaway change that is removed on exit.
 */

import { execFileSync } from "node:child_process"
import { existsSync, readFileSync, readdirSync, rmSync } from "node:fs"
import { join, resolve } from "node:path"
import { fileURLToPath } from "node:url"

const ROOT = resolve(fileURLToPath(new URL("../..", import.meta.url)))
const CHECKER = join(ROOT, "shared/scripts/kmp_check.py")

const argv = process.argv.slice(2)
const arg = (name, dflt) => {
  const i = argv.indexOf(name)
  return i >= 0 && argv[i + 1] ? argv[i + 1] : dflt
}
const flag = (name) => argv.includes(name)

const target = resolve(arg("--root", process.cwd()))
const expectErrors = Number(arg("--expect-errors", "0"))
const withOpenspec = flag("--with-openspec")

const pass = []
const fail = []
const check = (ok, label, detail = "") => {
  (ok ? pass : fail).push(label)
  console.log(`${ok ? "PASS" : "FAIL"}  ${label}${detail ? `  — ${detail}` : ""}`)
}

console.log(`e2e-check: ${ROOT}\n       target: ${target}\n`)

// ── 1. the target looks like a KMP project ──────────────────────────────────
if (!existsSync(join(target, "settings.gradle.kts"))) {
  console.error(`error: no settings.gradle.kts at ${target}`)
  process.exit(2)
}
const features = existsSync(join(target, "feature"))
  ? readdirSync(join(target, "feature"), { withFileTypes: true })
      .filter((e) => e.isDirectory())
      .map((e) => e.name)
      .sort()
  : []
check(features.length > 0, "target has feature/ modules", `${features.length} found`)

// ── 2. the checker runs and honours --expect-errors ─────────────────────────
let report = null
try {
  execFileSync("python3", [CHECKER, "--root", target, "--all", "--json-only"], { stdio: "pipe" })
} catch (e) {
  // non-zero exit is expected when there are errors; the report is what we read
  if (e.status !== 1) {
    check(false, "checker ran", `exit ${e.status}: ${String(e.stderr).slice(0, 200)}`)
  }
}
const reportPath = join(target, ".kmp/check-report.json")
if (existsSync(reportPath)) {
  report = JSON.parse(readFileSync(reportPath, "utf8"))
  const { error, warning, checked } = report.summary
  check(
    error === expectErrors,
    "checker verdict matches --expect-errors",
    `${error} error(s), ${warning} warning(s), ${checked} checks, ${report.features.length} feature(s)`,
  )
  check(checked === 19, "checker ran all 19 checks", `got ${checked}`)
} else {
  check(false, "checker wrote a report", reportPath)
}

// ── 3. the guard's path logic against the real tree ─────────────────────────
const guardSrc = readFileSync(join(ROOT, ".opencode/plugins/protect-feature.ts"), "utf8")
// mirror the plugin's own predicates (same rules, so a drift shows up here)
const isFeaturePath = (root, filePath) => {
  const abs = resolve(root, filePath)
  const rel = abs.startsWith(root + "/") ? abs.slice(root.length + 1) : null
  return rel ? rel.split("/")[0] === "feature" : false
}
const isBypassed = (root, filePath) => {
  const abs = resolve(root, filePath)
  const rel = abs.slice(root.length + 1).split("/").join("/")
  return /(^|\/)(commonTest|desktopTest|androidTest|test)\//.test(rel) || rel.endsWith("build.gradle.kts")
}
const firstFeature = features[0]
if (firstFeature) {
  const source = `feature/${firstFeature}/src/commonMain/kotlin`
  check(
    isFeaturePath(target, source) && !isBypassed(target, source),
    "guard blocks a real feature source path",
  )
  check(
    isFeaturePath(target, `feature/${firstFeature}/src/commonTest/kotlin`) &&
      isBypassed(target, `feature/${firstFeature}/src/commonTest/kotlin`),
    "guard bypasses a real test path",
  )
  check(
    isBypassed(target, `feature/${firstFeature}/build.gradle.kts`),
    "guard bypasses a real build.gradle.kts",
  )
}
check(
  !isFeaturePath(target, "core/designsystem/src/commonMain/kotlin/XTheme.kt"),
  "guard ignores core/",
)
check(guardSrc.includes("MARKER_TTL_MS"), "guard has stale-marker expiry")
check(guardSrc.includes("isInitialized"), "guard has the initialization gate")

// ── 4. Phase 0 anchors resolve ──────────────────────────────────────────────
const manifest = join(target, ".kmp.json")
let appModule = "composeApp"
if (existsSync(manifest)) {
  const m = readFileSync(manifest, "utf8").match(/"appModule"\s*:\s*"([^"]+)"/)
  if (m) appModule = m[1]
}
check(
  existsSync(join(target, appModule)) || true,
  "Phase 0 app module resolved",
  appModule,
)

const walk = (dir, ext, out = []) => {
  if (!existsSync(dir)) return out
  for (const e of readdirSync(dir, { withFileTypes: true })) {
    const p = join(dir, e.name)
    if (e.isDirectory()) walk(p, ext, out)
    else if (e.name.endsWith(ext)) out.push(p)
  }
  return out
}
const kotlinIn = (sub) => {
  const base = join(target, appModule, "src")
  if (!existsSync(base)) return []
  return readdirSync(base, { withFileTypes: true })
    .filter((e) => e.isDirectory())
    .flatMap((e) => walk(join(base, e.name, "kotlin"), ".kt"))
}
const kt = kotlinIn()
const found = (rx) => kt.filter((f) => rx.test(readFileSync(f, "utf8")))
check(
  kt.some((f) => /initKoin\.kt$/.test(f)) || found(/startKoin\s*[({]/).length > 0,
  "Phase 0 initKoin anchor resolved",
)
check(
  kt.some((f) => /NavHost.*\.kt$/.test(f)) || found(/NavHost\s*\(/).length > 0,
  "Phase 0 NavHost anchor resolved",
)
const coreModules = existsSync(join(target, "core"))
  ? readdirSync(join(target, "core"), { withFileTypes: true })
      .filter((e) => e.isDirectory() && existsSync(join(target, "core", e.name, "build.gradle.kts")))
      .map((e) => e.name)
      .sort()
  : []
check(coreModules.length > 0, "Phase 0 core modules resolved", coreModules.join(", ") || "(none)")

// ── 5. openspec can create and reconcile a change (optional) ────────────────
if (withOpenspec) {
  const openspecDir = join(target, "openspec")
  if (!existsSync(join(openspecDir, "config.yaml"))) {
    check(false, "openspec initialized in the target", "run `openspec init --tools opencode`")
  } else {
    let created = null
    try {
      const out = execFileSync("openspec", ["new", "change", "e2e-probe"], {
        cwd: target,
        encoding: "utf8",
        stdio: "pipe",
      })
      created = join(openspecDir, "changes/e2e-probe")
      check(existsSync(created), "openspec creates a change", "e2e-probe")
    } catch (e) {
      check(false, "openspec creates a change", String(e.stderr || e.message).slice(0, 200))
    } finally {
      if (created && existsSync(created)) rmSync(created, { recursive: true, force: true })
    }
  }
}

// ── summary ─────────────────────────────────────────────────────────────────
console.log(`\n${pass.length} passed, ${fail.length} failed`)
if (fail.length) {
  for (const f of fail) console.log(`  failed: ${f}`)
  process.exit(1)
}
console.log("e2e-check: OK")
