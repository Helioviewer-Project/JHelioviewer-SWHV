#!/usr/bin/env python3
"""Run the offline HEK regressions after `ant compile` (JDK 25)."""

import os
from pathlib import Path
import subprocess
import tempfile


root = Path(__file__).resolve().parents[2]
classpath = os.pathsep.join([str(root / "bin"), *(str(p) for p in (root / "lib").rglob("*.jar"))])
with tempfile.TemporaryDirectory(prefix="jhv-hek-tests-") as classes:
    subprocess.run([
        "javac", "-cp", classpath, "-d", classes,
        "extra/test/HEKQueryTest.java",
    ], cwd=root, check=True)
    for test in (
        "org.helioviewer.jhv.plugins.swek.sources.HEKQueryTest",
    ):
        subprocess.run([
            "java", "-Djava.awt.headless=true", "-Duser.timezone=UTC",
            "-cp", classes + os.pathsep + classpath, test,
        ], cwd=root, check=True)
