#!/usr/bin/env python3
"""
kmp_route.py — compute the routing decision for a request, and persist it.

The router skill used to re-derive the same facts on every turn, in prose, with no record. Two
things went wrong with that: the decision was not reproducible, and the *ordering* between missing
artifacts was left to the model ("no spec and no design — which first?"). This script makes the
mechanical half deterministic and writes the verdict where every later step can read it.

    python3 shared/scripts/kmp_route.py --capability leaderboard --action create --ui
    python3 shared/scripts/kmp_route.py --capability leaderboard          # infer create|modify
    python3 shared/scripts/kmp_route.py --json-only

Writes `.kmp/route.json` (tooling output; `.kmp/` is git-ignored).

What is decided here (mechanical, from the file system and from OpenSpec):
  managed         is this a kit project at all
  action          create | modify | review | test | init  (inferred when not given)
  target          the one skill that owns the work
  artifacts       spec? domain model? design handoff?  — existence only
  active_changes  in-flight OpenSpec changes, with task progress and age
  gaps            what is missing, in the order it must be filled
  next            the single next command, and which layer owns it

Artifact states come from `openspec status --change <id> --json`, so the **schema** owns the
artifact ids, their paths and their order — the kit hard-codes none of them. `design` here means
the *Penpot* handoff (`DESIGN.md`), which is found by search, not OpenSpec's `design.md` artifact.

What is NOT decided here (the model's job): reading the request, choosing the capability slug,
classifying the intent, and doing any of the work the target skill owns. This script never mutates
anything but `.kmp/route.json`.

Ordering rule — spec first, always:
    no spec        → BLOCKING. Define the spec (`/opsx-propose`) before anything else.
    no domain      → BLOCKING, for create/modify. Model the domain before building.
    no design      → non-blocking; only when the work has UI.
    otherwise      → the build skill.
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import time
from pathlib import Path

REPORT_PATH = Path(".kmp/route.json")
MANIFEST = Path(".kmp.json")
SETTINGS_GRADLE = Path("settings.gradle.kts")

BUILD_TARGET = {
    "create": "kmp-create-feature",
    "modify": "kmp-modify-feature",
    "review": "kmp-review-feature",
    "test": "kmp-test-feature",
}


def read(path: Path) -> str:
    try:
        return path.read_text(errors="ignore")
    except OSError:
        return ""


def norm(s: str) -> str:
    """Fold a name for comparison: `contributor-leaderboard` == `contributorleaderboard`."""
    return re.sub(r"[^a-z0-9]", "", s.lower())


def find_matching(root: Path, parents: list[str], want: str) -> str | None:
    """First dir under each parent that names `want`.

    Exact fold first (`leaderboard` → `leaderboard`), then suffix fold, because OpenSpec names
    change directories after the *change*, not the capability — `add-leaderboard` and
    `github-contributor-leaderboard` both belong to `leaderboard`. Returned as a repo-relative path.
    """
    target = norm(want)
    best: str | None = None
    for parent in parents:
        base = root / parent
        if not base.is_dir():
            continue
        for child in sorted(base.iterdir()):
            if not child.is_dir():
                continue
            name = norm(child.name)
            if name == target:
                return child.relative_to(root).as_posix()
            if best is None and name.endswith(target):
                best = child.relative_to(root).as_posix()
    return best


def find_design(root: Path, capability: str) -> str | None:
    """The Penpot handoff for this capability, wherever the design layer put it.

    Note this is NOT OpenSpec's `design` artifact (`design.md`, the technical design doc).
    The build needs the *Penpot* handoff, which is a `DESIGN.md` the design layer emits
    outside the change directory — so it is found by search, not by the schema.
    """
    target = norm(capability)
    for p in sorted(root.rglob("DESIGN.md")):
        if any(part.startswith(".") or part == "build" for part in p.parts):
            continue
        parent = p.parent.name
        if norm(parent) == target or target in norm(str(p)):
            return p.relative_to(root).as_posix()
    return None


def openspec_status(root: Path, change_id: str) -> dict | None:
    """Artifact states for a change, from OpenSpec itself.

    The schema defines the artifact ids, their paths and their dependency order, so the kit
    must not hard-code any of them — this replaces the file-probing the router used to do.
    Returns `None` when the CLI is unavailable or the call fails, so the caller falls back.
    """
    try:
        r = subprocess.run(
            ["openspec", "status", "--change", change_id, "--json"],
            cwd=root,
            capture_output=True,
            text=True,
            timeout=30,
        )
        if r.returncode != 0:
            return None
        data = json.loads(r.stdout)
    except (OSError, ValueError, subprocess.SubprocessError):
        return None
    return {
        a["id"]: {"status": a.get("status"), "outputPath": a.get("outputPath")}
        for a in data.get("artifacts", [])
    }


def active_changes(root: Path) -> list[dict]:
    base = root / "openspec/changes"
    if not base.is_dir():
        return []
    out = []
    for d in sorted(base.iterdir()):
        if not d.is_dir() or d.name == "archive":
            continue
        tasks = d / "tasks.md"
        done = total = 0
        if tasks.is_file():
            text = read(tasks)
            done = len(re.findall(r"^\s*-\s*\[[xX]\]", text, re.M))
            total = done + len(re.findall(r"^\s*-\s*\[\s\]", text, re.M))
        age_h = round((time.time() - d.stat().st_mtime) / 3600, 1)
        unchecked = total - done
        out.append(
            {
                "id": d.name,
                "tasks": {"done": done, "total": total},
                # The archive gate. A change is only ready to archive when every task is ticked
                # or struck through — archiving over unchecked boxes leaves a permanent, false
                # "incomplete" record, and `--yes` forces past OpenSpec's own warning. Prose asks
                # for this; the boolean is what a workflow can actually check.
                "unchecked": unchecked,
                "archive_ready": total > 0 and unchecked == 0,
                "age_hours": age_h,
                # Not yet started and untouched for a day: worth a user decision, not a silent resume.
                "stale": total > 0 and done == 0 and age_h > 24,
            }
        )
    return out


def compute(root: Path, capability: str | None, action: str | None, ui: bool) -> dict:
    core = root / "core"
    core_modules = (
        sorted(d.name for d in core.iterdir() if d.is_dir() and (d / "build.gradle.kts").is_file())
        if core.is_dir()
        else []
    )
    manifest = root / MANIFEST
    managed = (root / "core/common").is_dir() or manifest.is_file()

    app_module = "composeApp"
    if manifest.is_file():
        m = re.search(r'"appModule"\s*:\s*"([^"]+)"', read(manifest))
        if m:
            app_module = m.group(1)

    feature_dir = root / "feature"
    features = (
        sorted(d.name for d in feature_dir.iterdir() if d.is_dir()) if feature_dir.is_dir() else []
    )

    changes = active_changes(root)

    if not managed or action == "init":
        return {
            "capability": capability,
            "action": "init",
            "target": "skill:kmp-init",
            "project": {"managed": managed, "app_module": app_module, "core": core_modules, "features": features},
            "artifacts": {"spec": None, "domain": None, "design": None, "living_domain": None, "recon": None},
            "active_changes": changes,
            "gaps": []
            if managed
            else [
                {
                    "what": "project",
                    "blocking": True,
                    "fix": "kmp-ai-kit new <Name> <pkg>",
                    "why": "no core/common and no .kmp.json — this is not a kit project",
                }
            ],
            "next": {
                "step": "init",
                "command": "kmp-ai-kit new <Name> <pkg>" if not managed else "(already managed)",
                "owner": "build",
            },
            "chain": ["init"],
        }

    # ---- artifacts ----------------------------------------------------------
    # The schema owns the artifact ids, their paths and their order; OpenSpec reports the
    # states. Nothing here may hard-code an artifact filename.
    change_dir = find_matching(root, ["openspec/changes"], capability) if capability else None
    living_spec = find_matching(root, ["openspec/specs"], capability) if capability else None
    art = openspec_status(root, Path(change_dir).name) if change_dir else {}

    def artifact_path(aid: str) -> str | None:
        """Repo-relative path of a completed artifact, per the schema's own outputPath."""
        info = art.get(aid)
        if not info or info.get("status") != "done":
            return None
        out = info.get("outputPath") or ""
        if not out or "*" in out:  # glob artifacts (e.g. `specs/**/*.md`) have no single file
            return None
        return f"{change_dir}/{out}"

    # A capability is specced when the change's `specs` artifact is complete, or a living
    # spec already exists (the change has been archived).
    # `spec` means the ACTIVE CHANGE has its spec delta — it does not mean a living spec
    # exists. A capability with a living spec still needs a new change to carry the new delta
    # (`/opsx-propose`), so a living spec must NOT close the spec gap: doing so routed a modify
    # straight to `kmp-domain-model` with no change-id to write its delta into.
    spec = f"{change_dir}/specs" if art.get("specs", {}).get("status") == "done" else None
    domain = artifact_path("domain")
    # `design` here means the PENPOT handoff (DESIGN.md), not OpenSpec's design.md artifact.
    design = find_design(root, capability) if capability else None
    # The living domain model (the project's cumulative vocabulary). Not an artifact of this
    # change: it is read at the proposal step so a new spec reuses the project's words, and at
    # C0 of the domain model. Absent on a project's first change — that is normal, not a gap.
    living_domain = "openspec/domain/model.md" if (root / "openspec/domain/model.md").is_file() else None
    # Reconnaissance output (FDD steps 1-2). Present only on a project that ran recon; a project
    # may legitimately skip it, so its absence is not a gap.
    recon = "openspec/domain/features.md" if (root / "openspec/domain/features.md").is_file() else None

    # ---- action inference ---------------------------------------------------
    resolved = action
    if not resolved:
        feature_exists = any(norm(f) == norm(capability or "") for f in features)
        # A capability that already has a feature module or a living spec is a modification.
        resolved = "modify" if (feature_exists or living_spec) else "create"

    target_skill = BUILD_TARGET.get(resolved, "kmp-create-feature")

    # ---- gaps, in the order they must be filled -----------------------------
    gaps: list[dict] = []
    # At the very start — a managed project with nothing in flight and no vocabulary yet — the
    # deliberate first step is reconnaissance (FDD steps 1-2). It is optional, so it is never
    # blocking; it is surfaced so the first proposal does not name the domain by accident.
    if not changes and not living_domain and not recon:
        gaps.append(
            {
                "what": "recon",
                "blocking": False,
                "fix": "/kmp-domain-recon",
                "why": "a brand-new project: frame the domain first, or the first proposal names it by accident (skip is fine — record it)",
            }
        )
    if not spec:
        gaps.append(
            {
                "what": "spec",
                "blocking": True,
                "fix": f"/opsx-propose {capability}" if capability else "/opsx-propose",
                "why": "requirements are OpenSpec's and only OpenSpec's — the build never infers them",
            }
        )
    if resolved in ("create", "modify") and not domain:
        gaps.append(
            {
                "what": "domain-model",
                "blocking": True,
                "fix": f"/kmp-domain-model {capability}" if capability else "/kmp-domain-model",
                "why": "the domain layer sits between spec and build; run it, or record `Domain model: none` with a reason",
            }
        )
    if ui and not design:
        gaps.append(
            {
                "what": "design",
                "blocking": False,
                "fix": "/penpot-build-screen",
                "why": "UI with no Penpot handoff — the build would invent the visual intent",
            }
        )

    # ---- next step ----------------------------------------------------------
    blocking = [g for g in gaps if g["blocking"]]
    if blocking:
        nxt = {"step": blocking[0]["what"], "command": blocking[0]["fix"], "owner": "planning"}
    elif gaps:  # only non-blocking gaps remain — surface the first, but the work may proceed
        nxt = {"step": gaps[0]["what"], "command": gaps[0]["fix"], "owner": "design"}
    else:
        nxt = {"step": "build", "command": f"/{target_skill}" + (f" {capability}" if capability else ""), "owner": "build"}

    chain = ["spec", "domain"]
    if ui:
        chain.append("design?")
    chain.append(resolved if resolved in ("review", "test") else "build")

    return {
        "capability": capability,
        "action": resolved,
        "target": f"skill:{target_skill}",
        "project": {"managed": True, "app_module": app_module, "core": core_modules, "features": features},
        "artifacts": {"spec": spec, "domain": domain, "design": design, "living_domain": living_domain, "recon": recon},
        "active_changes": changes,
        "gaps": gaps,
        "next": nxt,
        "chain": chain,
    }


def main() -> int:
    ap = argparse.ArgumentParser(prog="kmp_route.py", description="Compute and persist the routing decision.")
    ap.add_argument("--capability", help="capability slug, e.g. leaderboard")
    ap.add_argument("--action", choices=["create", "modify", "review", "test", "init"], help="override inference")
    ap.add_argument("--ui", action="store_true", help="the work has UI, so a design handoff is relevant")
    ap.add_argument("--root", default=".", help="project root (default: cwd)")
    ap.add_argument("--report", default=str(REPORT_PATH), help=f"where to write the verdict (default: {REPORT_PATH})")
    ap.add_argument("--json-only", action="store_true", help="print the JSON only")
    args = ap.parse_args()

    root = Path(args.root).resolve()
    route = compute(root, args.capability, args.action, args.ui)
    route["request_root"] = str(root)
    route["computed_at"] = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())

    report = root / args.report
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps(route, indent=2) + "\n")

    if args.json_only:
        print(json.dumps(route, indent=2))
        return 0

    print(f"route — capability={route['capability'] or '(unspecified)'} action={route['action']}")
    print(f"  target  : {route['target']}")
    a = route["artifacts"]
    print(f"  artifacts: spec={'yes' if a['spec'] else 'NO'}  domain={'yes' if a['domain'] else 'NO'}  design={'yes' if a['design'] else 'no'}")
    print(f"  vocabulary: living domain model {'yes' if a.get('living_domain') else 'none (created by the first domain model)'}")
    if a.get("recon"):
        print(f"  recon     : feature candidates at {a['recon']}")
    if route["active_changes"]:
        for c in route["active_changes"]:
            flag = "  (stale — decide: resume or archive)" if c["stale"] else ""
            print(f"  in flight: {c['id']}  {c['tasks']['done']}/{c['tasks']['total']} tasks  {c['age_hours']}h{flag}")
            if not c["archive_ready"] and c["tasks"]["total"] > 0:
                # Loud on purpose: /opsx-archive with `--yes` would turn this into a permanent
                # false "incomplete" record in the archive.
                print(
                    f"  ⚠ archive NOT ready: {c['unchecked']} unchecked task(s) — tick each as its "
                    f"work lands, or strike it with a reason; never force past the warning"
                )
    if route["gaps"]:
        for g in route["gaps"]:
            print(f"  {'BLOCKING' if g['blocking'] else 'optional'}: missing {g['what']} → {g['fix']}")
    n = route["next"]
    print(f"\n  next    : [{n['owner']}] {n['command']}")
    print(f"  chain   : {' → '.join(route['chain'])}")
    print(f"  report  : {args.report}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
