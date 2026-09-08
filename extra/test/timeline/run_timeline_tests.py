#!/usr/bin/env python3
"""Compile and run the offline timeline regressions after `ant compile` (JDK 25)."""

import argparse
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile


parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--benchmark", action="store_true", help="also measure incremental loading, drawing, allocations and cache footprint")
args = parser.parse_args()

root = Path(__file__).resolve().parents[3]
classpath = os.pathsep.join([str(root / "bin"), str(root / "resources"), *(str(p) for p in (root / "lib").rglob("*.jar"))])
with tempfile.TemporaryDirectory(prefix="jhv-timeline-tests-") as classes:
    subprocess.run([
        "javac", "-cp", classpath, "-d", classes,
        str(root / "extra/test/timeline/TimelineDataTest.java"),
    ], check=True)
    benchmark_options = []
    if args.benchmark:
        agent = Path(classes) / "timeline-benchmark-agent.jar"
        with zipfile.ZipFile(agent, "w") as jar:
            jar.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nPremain-Class: org.helioviewer.jhv.timelines.band.TimelineDataTest\n\n")
            for compiled in Path(classes).rglob("*.class"):
                jar.write(compiled, compiled.relative_to(classes))
        benchmark_options = ["-javaagent:" + str(agent), "--add-opens=java.base/java.util=ALL-UNNAMED"]
    subprocess.run([
        "java", "-Djava.awt.headless=true", "-Duser.timezone=UTC", "-Duser.home=" + classes,
        *benchmark_options,
        "-cp", os.pathsep.join([classes, classpath]),
        "org.helioviewer.jhv.timelines.band.TimelineDataTest",
        *(["--benchmark"] if args.benchmark else []),
    ], check=True)
