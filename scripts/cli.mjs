#!/usr/bin/env node
/**
 * kmp-ai-kit — the kit's command line.
 *
 *   kmp-ai-kit new <Name> <com.pkg> [dest]
 *   kmp-ai-kit --help
 *
 * A thin dispatcher: each subcommand's logic lives in its own script so it stays runnable
 * on its own (`npm run init`, `node scripts/scaffold/km-init.mjs`). This file only parses
 * the subcommand and hands the rest through, keeping one obvious entry point.
 */

import { execFileSync } from "node:child_process"
import { readFileSync } from "node:fs"
import { dirname, join, resolve } from "node:path"
import { fileURLToPath } from "node:url"

const HERE = dirname(fileURLToPath(import.meta.url))
const SCHEMA = "kmp-ai-kit"

const HELP = `${SCHEMA} — Kotlin Multiplatform AI Kit

usage
  ${SCHEMA} new <Name> <com.pkg> [dest]     scaffold a new, self-contained KMP app
  ${SCHEMA} help                            this message

new — a new app
  <Name>   project name, PascalCase (e.g. GithubLeaderboard)
  <pkg>    package prefix, lowercase dotted (e.g. com.example.demo)
  [dest]   destination directory (default: next to the kit, <kit>/../<Name>)

  The app is self-contained: every skill, rule and the architecture guard are copied in,
  so it depends on no path outside itself and can be moved, cloned, or shared.

  flags
    --dry-run        show what would be created, write nothing
    --force          scaffold into a non-empty destination
    --linked         thin relative links to the kit instead of copying (must move together)
    --no-openspec    skip \`openspec init\`
    --no-git         skip \`git init\` and the initial commit

examples
  ${SCHEMA} new GithubLeaderboard com.example.demo
  ${SCHEMA} new MyApp com.acme.myapp ~/projects/MyApp
  ${SCHEMA} new MyApp com.acme.myapp --dry-run
`

const [, , sub, ...rest] = process.argv

switch (sub) {
  case "new":
  case "init": {
    const script = join(HERE, "scaffold/km-init.mjs")
    try {
      execFileSync(process.execPath, [script, ...rest], { stdio: "inherit" })
    } catch (e) {
      process.exit(typeof e.status === "number" ? e.status : 1)
    }
    break
  }
  case "help":
  case "--help":
  case "-h":
  case undefined:
    process.stdout.write(HELP)
    break
  case "--version":
  case "-v": {
    const pkg = JSON.parse(readFileSync(resolve(HERE, "../package.json"), "utf8"))
    process.stdout.write(`${pkg.version}\n`)
    break
  }
  default:
    process.stderr.write(`${SCHEMA}: unknown command '${sub}'\n\n${HELP}`)
    process.exit(2)
}
