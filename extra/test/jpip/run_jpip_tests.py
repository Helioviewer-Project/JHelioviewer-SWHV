#!/usr/bin/env python3
"""Compile and run JPIP regressions after ant compile (JDK 25).

Offline by default. --live also retrieves and decodes a fixed ROB AIA image.
"""

import argparse
import os
from pathlib import Path
import platform
import subprocess
import tempfile
import zipfile


parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--live", action="store_true", help="also test live ROB retrieval (requires network and bundled KDU)")
args = parser.parse_args()
root = Path(__file__).resolve().parents[3]
classpath = os.pathsep.join([str(root / "bin"), str(root / "resources"),
                             *(str(p) for p in sorted((root / "lib").rglob("*.jar")))])
with tempfile.TemporaryDirectory(prefix="jhv-jpip-tests-") as temporary:
    work = Path(temporary)
    sources = ["JPIPSerializerTest.java", "JPIPResponseTest.java", "JPIPCacheManagerTest.java", "HTTPStreamTest.java", "JPIPSocketTest.java"]
    if args.live:
        sources.append("ROBTest.java")
    subprocess.run(["javac", "-cp", classpath, "-d", temporary,
                    *(str(Path(__file__).parent / name) for name in sources)], check=True, timeout=60)
    java = ["java", "-Djava.awt.headless=true", "-Duser.timezone=UTC", "-Duser.home=" + temporary,
            "--enable-native-access=ALL-UNNAMED", "-cp", os.pathsep.join([temporary, classpath])]
    for name in ["JPIPSerializerTest", "JPIPResponseTest", "JPIPCacheManagerTest", "http.HTTPStreamTest", "JPIPSocketTest"]:
        print("Running " + name, flush=True)
        subprocess.run([*java, "-Xmx64m", "org.helioviewer.jhv.view.j2k.jpip." + name],
                       check=True, timeout=60)
    if args.live:
        system = platform.system()
        machine = platform.machine().lower()
        if system == "Darwin":
            target = "macos-arm64" if machine == "arm64" else "macos"
            library = "libkdu_jni.dylib"
        elif system == "Linux" and machine in ("x86_64", "amd64"):
            target, library = "linux", "libkdu_jni.so"
        else:
            parser.error("Live test currently supports macOS and Linux x86-64")
        with zipfile.ZipFile(root / "lib/jhv" / ("jhv-natives-" + target + ".jar")) as jar:
            native_directory = target if target == "macos-arm64" else target + "-amd64"
            (work / library).write_bytes(jar.read("jhv/" + native_directory + "/" + library))
        uri = "jpip://jpip.swhv.oma.be/aia_171/2026/09/09/2026_09_09__12_00_33_349__SDO_AIA_AIA_171.jp2"
        print("Retrieving " + uri, flush=True)
        subprocess.run([*java, "-Xmx512m", "org.helioviewer.jhv.view.j2k.ROBTest", str(work / library), uri],
                       check=True, timeout=180)
