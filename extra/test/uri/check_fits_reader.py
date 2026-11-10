#!/usr/bin/env python3
"""Check the Java FITS reader with fresh classes and only the candidate native library."""

import argparse
import os
from pathlib import Path
import shutil
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[3]
PLATFORMS = {
    "macos-arm64": ("macos-arm64", "libjhvfits.dylib"),
    "macos-x64": ("macos-amd64", "libjhvfits.dylib"),
    "linux-x64": ("linux-amd64", "libjhvfits.so"),
    "windows-x64": ("windows-amd64", "jhvfits.dll"),
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path, help="output directory from extra/cfitsio/build.py")
    parser.add_argument("--platform", choices=PLATFORMS, required=True)
    args = parser.parse_args()
    resource, name = PLATFORMS[args.platform]
    library = args.output.resolve() / resource / name
    with tempfile.TemporaryDirectory(prefix="jhv-fits-java-") as temporary:
        work = Path(temporary)
        classes = work / "classes"
        classes.mkdir()
        natives = work / "resources/jhv" / resource
        natives.mkdir(parents=True)
        shutil.copy2(library, natives / name)
        jars = sorted((ROOT / "lib").rglob("*.jar"))
        # Exclude bin and lib so neither stale classes nor a bundled reader can substitute.
        classpath = os.pathsep.join(map(str, [work / "resources", classes, ROOT / "resources", *jars]))
        subprocess.run(["javac", "--release", "25", "-encoding", "UTF-8", "-sourcepath", str(ROOT / "src"),
                        "-cp", classpath, "-d", str(classes), str(ROOT / "extra/test/uri/FITSReaderTest.java")],
                       cwd=ROOT, check=True, timeout=120)
        subprocess.run(["java", "--enable-native-access=ALL-UNNAMED", "-Djava.awt.headless=true", "-Duser.timezone=UTC",
                        "-Duser.language=en", "-Duser.country=US", "-Duser.home=" + str(work),
                        "-Djava.io.tmpdir=" + str(work), "-cp", classpath, "org.helioviewer.jhv.source.FITSReaderTest"],
                       cwd=ROOT, check=True, timeout=180)


if __name__ == "__main__":
    main()
