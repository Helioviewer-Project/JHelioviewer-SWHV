#!/usr/bin/env python3
"""Run JHV regression suites. Defaults to offline checks without a graphics context."""

import argparse
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import time


TESTS = Path(__file__).resolve().parent
ROOT = TESTS.parents[1]
DEFAULT_SUITES = ["maintenance", "model", "j2k", "timelines", "event"]
SCRIPTS = {
    "j2k": TESTS / "j2k/run_j2k_tests.py",
    "timelines": TESTS / "timelines/run_timelines_tests.py",
    "event": TESTS / "event/run_event_tests.py",
    "shaders": TESTS / "opengl/validate_glsl_syntax.py",
    "wcs": TESTS / "wcs/run_jhv_wcs_hpc_validation_suite.py",
}
JAVA_TESTS = {
    "model": ["io.ModelDataUriTest", "opengl.model.AssimpMetaDataTest", "opengl.model.AssimpModelLoaderTest"],
    "opengl": ["opengl.ColoredVertexRenderingTest", "opengl.ModelRenderingTest", "opengl.GLGrabRenderingTest"],
}


def run_java_suite(suite):
    classpath = os.pathsep.join([str(ROOT / "bin"), str(ROOT / "resources"),
                                *(str(p) for p in sorted((ROOT / "lib").rglob("*.jar")))])
    with tempfile.TemporaryDirectory(prefix="jhv-" + suite + "-tests-") as temporary:
        sources = [TESTS / suite / (name.rsplit(".", 1)[-1] + ".java") for name in JAVA_TESTS[suite]]
        subprocess.run(["javac", "-cp", classpath, "-d", temporary, *map(str, sources)],
                       cwd=ROOT, check=True, timeout=60)
        for name in JAVA_TESTS[suite]:
            home = Path(temporary) / name
            home.mkdir()
            print("Running " + name, flush=True)
            subprocess.run([
                "java", "--enable-native-access=ALL-UNNAMED", "-Djava.awt.headless=true",
                "-Duser.timezone=UTC", "-Duser.language=en", "-Duser.country=US", "-Duser.home=" + str(home),
                "-cp", os.pathsep.join([temporary, classpath]), "org.helioviewer.jhv." + name,
            ], cwd=ROOT, check=True, timeout=120)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("suites", nargs="*", choices=[*DEFAULT_SUITES, "shaders", "wcs", "opengl", "uri"],
                        default=DEFAULT_SUITES, help="suites to run (default: %(default)s)")
    parser.add_argument("--no-build", action="store_true", help="skip the initial ant compile")
    args = parser.parse_args()
    if not args.no_build and any(suite != "maintenance" and suite != "shaders" for suite in args.suites):
        subprocess.run(["ant", "compile"], cwd=ROOT, check=True)
    failed = []
    for suite in dict.fromkeys(args.suites):
        print("\nRunning suite: " + suite, flush=True)
        start = time.monotonic()
        try:
            if suite in JAVA_TESTS:
                run_java_suite(suite)
            elif suite == "uri":
                subprocess.run(["bash", str(TESTS / "uri/run-fast-rice-verifier.sh")],
                               cwd=ROOT, env={**os.environ, "JHV_SKIP_COMPILE": "1"}, check=True)
            elif suite == "maintenance":
                for directory in (ROOT / "extra/licenses", ROOT / "extra/ffmpeg"):
                    subprocess.run([sys.executable, "-m", "unittest", "discover", "-s", str(directory), "-v"],
                                   cwd=ROOT, check=True, timeout=120)
            else:
                subprocess.run([sys.executable, str(SCRIPTS[suite])], cwd=ROOT, check=True)
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired, OSError) as error:
            detail = "exit status " + str(error.returncode) if isinstance(error, subprocess.CalledProcessError) else str(error)
            print("FAIL: " + suite + ": " + detail, flush=True)
            failed.append(suite)
        else:
            print(f"PASS: {suite} ({time.monotonic() - start:.1f}s)", flush=True)
    if failed:
        print("\nFailed suites: " + ", ".join(failed), flush=True)
        return 1
    print("\nAll requested suites passed.", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
