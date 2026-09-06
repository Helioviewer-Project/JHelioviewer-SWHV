#!/usr/bin/env python3
"""Run the offline HEK regressions after `ant compile` (JDK 25)."""

import os
from pathlib import Path
import subprocess
import tempfile


root = Path(__file__).resolve().parents[2]
classpath = os.pathsep.join([str(root / "bin"), str(root / "resources"), *(str(p) for p in (root / "lib").rglob("*.jar"))])
with tempfile.TemporaryDirectory(prefix="jhv-hek-tests-") as classes:
    subprocess.run([
        "javac", "-cp", classpath, "-d", classes,
        "extra/test/HEKQueryTest.java",
        "extra/test/HEKHandlerTest.java",
        "extra/test/EventDatabaseTest.java",
        "extra/test/HEKGeometryTest.java",
    ], cwd=root, check=True)
    for test in (
        "org.helioviewer.jhv.plugins.swek.sources.HEKQueryTest",
        "org.helioviewer.jhv.plugins.swek.sources.HEKHandlerTest",
    ):
        subprocess.run([
            "java", "-Djava.awt.headless=true", "-Duser.timezone=UTC",
            "-cp", classes + os.pathsep + classpath, test,
        ], cwd=root, check=True)
    with tempfile.TemporaryDirectory(prefix="jhv-hek-home-") as test_home:
        for phase in ("store", "reload"):
            subprocess.run([
                "java", "--enable-native-access=ALL-UNNAMED", "-Djava.awt.headless=true",
                "-Duser.timezone=UTC", "-Duser.home=" + test_home,
                "-cp", classes + os.pathsep + classpath,
                "org.helioviewer.jhv.database.EventDatabaseTest", phase,
            ], cwd=root, check=True)
    with tempfile.TemporaryDirectory(prefix="jhv-hek-geometry-home-") as test_home:
        subprocess.run([
            "java", "--enable-native-access=ALL-UNNAMED", "-Djava.awt.headless=true", "-Duser.timezone=UTC", "-Duser.home=" + test_home,
            "-cp", classes + os.pathsep + classpath, "org.helioviewer.jhv.plugins.swek.sources.HEKGeometryTest",
        ], cwd=root, check=True)
