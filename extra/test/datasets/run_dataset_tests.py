#!/usr/bin/env python3
"""Offline dataset dialogs and the Settings and FITS forms through their shared definitions and headless Swing widgets. Never opens windows."""
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
CP = os.pathsep.join(map(str, [ROOT / "bin", ROOT / "resources", ROOT / "lib", *sorted((ROOT / "lib").rglob("*.jar"))]))
with tempfile.TemporaryDirectory(prefix="jhv-dataset-tests-") as tmp:
    subprocess.run(["javac", "-cp", CP, "-d", tmp, str(HERE / "DialogScreenTest.java"), str(HERE / "FormScreensTest.java"), str(HERE / "ImageScreenTest.java")], check=True)
    for name in ("DialogScreenTest", "FormScreensTest", "ImageScreenTest"):
        home = Path(tmp) / name
        home.mkdir()
        subprocess.run(["java", "--enable-native-access=ALL-UNNAMED", "--add-exports=java.desktop/sun.swing=ALL-UNNAMED", "-Djava.awt.headless=true", "-Duser.timezone=UTC", "-Duser.home=" + str(home), "-Djhv.reference=" + str(HERE / "swing-reference.tsv"), "-Djhv.image.reference=" + str(HERE / "image-swing-reference.tsv"), "-cp", tmp + os.pathsep + CP, "org.helioviewer.jhv.ui." + name], check=True, timeout=120)
