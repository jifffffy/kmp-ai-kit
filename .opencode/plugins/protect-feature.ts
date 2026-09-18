import type { Plugin } from "@opencode-ai/plugin"
import { existsSync, statSync } from "node:fs"
import { isAbsolute, join, relative, resolve, sep } from "node:path"

/**
 * Protect `feature/**` from ad-hoc edits.
 *
 * Ports KMPilot's Claude Code `PreToolUse` hook (`protect-feature-files.sh`) to the
 * opencode plugin surface — the hook never ran under opencode, so the guard moves to
 * where it actually executes. A feature may only be created or changed by the owning
 * skill (`kmp-create-feature` / `kmp-modify-feature`), which creates the marker for
 * the duration of its run and removes it on exit.
 *
 * Fail-closed: an unrecognized future write tool is not exempted by name — the guard
 * matches on the target path for any tool that carries one.
 */

const MARKER = "/tmp/.kmp-skill-active"
const MARKER_TTL_MS = 2 * 60 * 60 * 1000 // a stale marker must not outlive its run
const WRITE_TOOLS = new Set(["edit", "write", "patch", "multiedit"])

function isFeaturePath(root: string, filePath: string): boolean {
  const abs = isAbsolute(filePath) ? filePath : resolve(root, filePath)
  const rel = relative(root, abs)
  if (rel.startsWith("..") || isAbsolute(rel)) return false
  return rel.split(sep)[0] === "feature"
}

/** Test source sets and Gradle files are written by test agents without a marker. */
function isBypassedPath(root: string, filePath: string): boolean {
  const abs = isAbsolute(filePath) ? filePath : resolve(root, filePath)
  const rel = relative(root, abs).split(sep).join("/")
  if (/(^|\/)(commonTest|desktopTest|androidTest|test)\//.test(rel)) return true
  return rel.endsWith("build.gradle.kts")
}

function markerIsFresh(): boolean {
  if (!existsSync(MARKER)) return false
  try {
    return Date.now() - statSync(MARKER).mtimeMs < MARKER_TTL_MS
  } catch {
    return false
  }
}

/**
 * Initialization gate: a repo with neither `core/common` nor an optional `.kmp.json`
 * cannot compile the code a feature imports. No marker may override that.
 */
function isInitialized(root: string): boolean {
  return existsSync(join(root, "core", "common")) || existsSync(join(root, ".kmp.json"))
}

const KmpFeatureGuard: Plugin = async ({ directory, worktree }) => {
  const root = worktree || directory
  return {
    "tool.execute.before": async (input, output) => {
      if (!WRITE_TOOLS.has(input.tool)) return
      const args = output.args as Record<string, unknown> | undefined
      if (!args) return
      const filePath = args.filePath ?? args.file_path ?? args.path
      if (typeof filePath !== "string" || !isFeaturePath(root, filePath)) return
      if (isBypassedPath(root, filePath)) return

      if (!isInitialized(root)) {
        throw new Error(
          "Blocked: this repo is not kit-managed (no core/common and no .kmp.json). " +
            "The kit's feature skills generate code that imports core/ types, so a " +
            "feature written here would not compile.",
        )
      }
      if (markerIsFresh()) return
      throw new Error(
        "Blocked: cannot edit feature/ source directly (kmp-ai-kit). " +
          "Invoke the kmp-create-feature or kmp-modify-feature skill first — it " +
          `creates ${MARKER} for the duration of the run.`,
      )
    },
  }
}

export default KmpFeatureGuard
