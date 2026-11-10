#!/usr/bin/env python3
"""Validate a candidate WCS library through its existing binding and package it."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import tempfile

from build import HERE, PLATFORMS, ROOT


TESTS = ["WcsLibraryTest"]


def checked_library(output, target):
    resource, name = PLATFORMS[target]
    library = output / resource / name
    manifest = json.loads((HERE / "wcslib.json").read_text())
    records = json.loads((output / "builds.json").read_text())
    matching = [record for record in records if record["platform"] == target]
    if len(matching) != 1:
        raise ValueError(f"Expected one build record for {target}")
    record = matching[0]
    if record["source"] != manifest:
        raise ValueError("Build record does not match the pinned dependencies")
    if library.stat().st_size != record["bytes"] or hashlib.sha256(library.read_bytes()).hexdigest() != record["sha256"]:
        raise ValueError("Candidate library does not match its build record")
    return library, record


def run(command, log):
    with log.open("wb") as output:
        result = subprocess.run(command, cwd=ROOT, stdout=output, stderr=subprocess.STDOUT, timeout=180)
    print(log.read_text(encoding="utf-8", errors="replace"), end="", flush=True)
    result.check_returncode()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path, help="output directory from build.py")
    parser.add_argument("--platform", choices=PLATFORMS, required=True)
    args = parser.parse_args()
    output = args.output.resolve()
    artifact = output / "artifacts" / args.platform
    if artifact.exists():
        shutil.rmtree(artifact)
    logs = output / "validation" / args.platform
    if logs.exists():
        shutil.rmtree(logs)
    logs.mkdir(parents=True, exist_ok=True)
    library, record = checked_library(output, args.platform)
    revision = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    dirty = bool(subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT, text=True).strip())
    if os.environ.get("GITHUB_ACTIONS") == "true":
        if revision.lower() != os.environ["JHV_COMMIT"].lower() or dirty:
            raise ValueError("CI validation requires a clean checkout of the requested commit")
    with tempfile.TemporaryDirectory(prefix="jhv-wcs-check-") as temporary:
        work = Path(temporary)
        classes = work / "classes"
        classes.mkdir()
        natives = work / "resources/jhv" / PLATFORMS[args.platform][0]
        natives.mkdir(parents=True)
        shutil.copy2(library, natives / library.name)
        jars = sorted((ROOT / "lib").rglob("*.jar"))
        # Only the candidate WCS library is on the resource path, with fresh classes.
        classpath = os.pathsep.join(map(str, [work / "resources", classes, ROOT / "resources", *jars]))
        run(["javac", "--release", "25", "-encoding", "UTF-8", "-sourcepath", str(ROOT / "src"),
             "-cp", classpath, "-d", str(classes), str(ROOT / "extra/test/wcs/WcsLibraryTest.java")],
            logs / "javac.log")
        run(["java", "-version"], logs / "java-version.log")
        run(["java", "--enable-native-access=ALL-UNNAMED", "-Djava.awt.headless=true", "-Duser.timezone=UTC",
             "-Duser.language=en", "-Duser.country=US", "-Duser.home=" + str(work),
             "-Djava.io.tmpdir=" + str(work), "-cp", classpath, "org.helioviewer.jhv.wcs.WcsLibraryTest"],
            logs / "geometry.log")

    artifact.mkdir(parents=True, exist_ok=True)
    shutil.copy2(library, artifact / library.name)
    shutil.copy2(ROOT / "extra/licenses/WCSLIB.txt", artifact / "WCSLIB.txt")
    for name in ("LGPL-3.0.txt", "GPL-3.0.txt"):
        shutil.copy2(ROOT / "resources/licenses" / name, artifact / name)
    (artifact / "builds.json").write_text(json.dumps([record], indent=2) + "\n")
    validation = {
        "status": "passed", "platform": args.platform, "binary_sha256": record["sha256"],
        "source_revision": revision, "source_dirty": dirty, "tests": TESTS,
        "host": {"system": platform.system(), "machine": platform.machine(), "release": platform.release()},
        "workflow_commit": os.environ.get("WORKFLOW_COMMIT"), "workflow_ref": os.environ.get("WORKFLOW_REF"),
        "run_url": (os.environ["GITHUB_SERVER_URL"] + "/" + os.environ["GITHUB_REPOSITORY"] + "/actions/runs/" + os.environ["GITHUB_RUN_ID"])
                   if os.environ.get("GITHUB_ACTIONS") == "true" else None,
    }
    (artifact / "validation.json").write_text(json.dumps(validation, indent=2) + "\n")
    for log in logs.iterdir():
        shutil.copy2(log, artifact / log.name)
    checksums = [hashlib.sha256(path.read_bytes()).hexdigest() + "  " + path.name for path in sorted(artifact.iterdir())]
    (artifact / "SHA256SUMS").write_text("\n".join(checksums) + "\n")
    print(f"Validated {args.platform}: {artifact}", flush=True)


if __name__ == "__main__":
    main()
