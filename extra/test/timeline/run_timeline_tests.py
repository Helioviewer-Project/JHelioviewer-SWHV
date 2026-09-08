#!/usr/bin/env python3
"""Compile and run the offline timeline regressions after `ant compile` (JDK 25)."""

import argparse
import os
from pathlib import Path
import subprocess
import tempfile


parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--benchmark", action="store_true", help="also measure cache insertion, graph preparation and offscreen painting")
args = parser.parse_args()

root = Path(__file__).resolve().parents[3]
classpath = os.pathsep.join([str(root / "bin"), str(root / "resources"), *(str(p) for p in (root / "lib").rglob("*.jar"))])
with tempfile.TemporaryDirectory(prefix="jhv-timeline-tests-") as classes:
    subprocess.run([
        "javac", "-cp", classpath, "-d", classes,
        str(root / "extra/test/timeline/TimelineDataTest.java"),
    ], check=True)
    subprocess.run([
        "java", "-Djava.awt.headless=true", "-Duser.timezone=UTC", "-Duser.home=" + classes,
        "-cp", os.pathsep.join([classes, classpath]),
        "org.helioviewer.jhv.timelines.band.TimelineDataTest",
        *(["--benchmark"] if args.benchmark else []),
    ], check=True)
