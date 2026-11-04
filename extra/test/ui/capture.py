#!/usr/bin/env python3
"""Capture the production Swing UI with local data and disposable preferences."""
import argparse
import os
import platform
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("output", type=Path)
parser.add_argument("--theme", choices=["Dark", "Light"], default="Dark")
parser.add_argument("--width", type=int, default=1440)
parser.add_argument("--height", type=int, default=980)
args = parser.parse_args()
output = args.output.resolve()
output.mkdir(parents=True, exist_ok=True)
subprocess.run(["ant", "compile"], cwd=ROOT, check=True)
classpath = os.pathsep.join(map(str, [ROOT / "bin", ROOT / "resources", ROOT / "lib", *sorted((ROOT / "lib").rglob("*.jar"))]))
with tempfile.TemporaryDirectory(prefix="jhv-ui-capture-") as temporary:
    home = Path(temporary)
    settings = home / "JHelioviewer-SWHV/Settings"
    settings.mkdir(parents=True)
    (settings / "user.properties").write_text("display.theme=" + args.theme + "\nstartup.sampHub=false\n")
    subprocess.run(["javac", "-cp", classpath, "-d", temporary, str(ROOT / "extra/test/ui/UiCapture.java")], check=True)
    capture_options = []
    if platform.system() == "Darwin":
        finder = home / "window-id"
        subprocess.run(["swiftc", str(ROOT / "extra/test/ui/window-id.swift"), "-o", str(finder)], check=True)
        capture_options = ["-Djhv.capture.windowFinder=" + str(finder)]
    env = dict(os.environ, JHV_PREFERRED_WIDTH=str(args.width), JHV_PREFERRED_HEIGHT=str(args.height))
    with (output / "capture.log").open("w") as log:
        subprocess.run(["java", "--enable-native-access=ALL-UNNAMED", "--add-exports=java.desktop/sun.awt=ALL-UNNAMED",
                        "--add-exports=java.desktop/sun.swing=ALL-UNNAMED", "-Xdock:name=JHV UI Capture",
                        *capture_options, "-Duser.home=" + temporary, "-Duser.timezone=UTC", "-cp", temporary + os.pathsep + classpath,
                        "org.helioviewer.jhv.UiCapture", str(output)], cwd=ROOT, env=env, stdout=log, stderr=log, check=True, timeout=150)
print(output)
