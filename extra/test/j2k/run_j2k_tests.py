#!/usr/bin/env python3
"""Compile and run JPIP regressions after ant compile (JDK 25).

Offline by default. --live also retrieves and decodes a fixed ROB AIA image, a movie and a Callisto spectrogram.
"""

import argparse
import json
import os
from pathlib import Path
import platform
import subprocess
import tempfile
import zipfile
from urllib.request import urlopen


parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--live", action="store_true", help="also test live ROB retrieval (requires network and the native libraries)")
parser.add_argument("--cleaner", action="store_true", help="with --live, also exercise GC-dependent abandoned-view cleanup")
parser.add_argument("--bridge", help="libjhvj2k built by native/jpeg2000/build.sh, when the natives jar has none")
args = parser.parse_args()
if args.cleaner and not args.live:
    parser.error("--cleaner requires --live")
root = Path(__file__).resolve().parents[3]
classpath = os.pathsep.join([str(root / "bin"), str(root / "resources"),
                             *(str(p) for p in sorted((root / "lib").rglob("*.jar")))])
with tempfile.TemporaryDirectory(prefix="jhv-jpip-tests-") as temporary:
    work = Path(temporary)
    if platform.system() == "Windows" and not temporary.isascii():
        parser.error("Set TMPDIR to a writable ASCII path so the Windows test cache stays isolated")
    sources = ["JPIPCacheManagerTest.java", "HTTPStreamTest.java", "JPIPSocketTest.java", "ImageBufferCacheTest.java", "J2KReaderTest.java"]
    if args.live:
        sources.extend(["J2KFixture.java", "FrameDecoderTest.java", "CallistoTest.java"])
    subprocess.run(["javac", "-cp", classpath, "-d", temporary,
                    *(str(Path(__file__).parent / name) for name in sources)], check=True, timeout=60)
    java = ["java", "-Djava.awt.headless=true", "-Duser.timezone=UTC", "-Duser.home=" + temporary,
            "-Djava.io.tmpdir=" + temporary,
            "--enable-native-access=ALL-UNNAMED", "-cp", os.pathsep.join([temporary, classpath])]
    print("Running ImageBufferCacheTest", flush=True)
    subprocess.run([*java, "-da", "-Dorg.lwjgl.util.DebugAllocator=true",
                    "org.helioviewer.jhv.image.ImageBufferCacheTest"], check=True, timeout=60)
    for name in ["JPIPCacheManagerTest", "http.HTTPStreamTest", "JPIPSocketTest"]:
        print("Running " + name, flush=True)
        subprocess.run([*java, "-Xmx64m", "org.helioviewer.jhv.source.jpip." + name],
                       check=True, timeout=60)
    print("Running J2KReaderTest without native libraries", flush=True)
    subprocess.run([*java, "org.helioviewer.jhv.source.J2KReaderTest"], check=True, timeout=60)
    if platform.system() != "Windows":
        print("Running JPIPCacheManagerTest with Windows cache paths", flush=True)
        subprocess.run([*java, "-Xmx64m", "org.helioviewer.jhv.source.jpip.JPIPCacheManagerTest", "--windows-path"],
                       check=True, timeout=60)
    if args.live:
        system = platform.system()
        machine = platform.machine().lower()
        if system == "Darwin":
            target = "macos-arm64" if machine == "arm64" else "macos"
            suffix = ".dylib"
        elif system == "Linux" and machine in ("x86_64", "amd64"):
            target, suffix = "linux", ".so"
        else:
            parser.error("Live test currently supports macOS and Linux x86-64")
        # The bridge finds Kakadu next to itself.
        libraries = [str(work / ("libkdu_jni" + suffix)), str(work / ("libjhvj2k" + suffix))]
        with zipfile.ZipFile(root / "lib/jhv" / ("jhv-natives-" + target + ".jar")) as jar:
            native_directory = "jhv/" + (target if target == "macos-arm64" else target + "-amd64") + "/"
            Path(libraries[0]).write_bytes(jar.read(native_directory + "libkdu_jni" + suffix))
            if args.bridge:
                Path(libraries[1]).write_bytes(Path(args.bridge).read_bytes())
            else:
                Path(libraries[1]).write_bytes(jar.read(native_directory + "libjhvj2k" + suffix))
        uri = "jpip://jpip.swhv.oma.be/aia_171/2026/09/09/2026_09_09__12_00_33_349__SDO_AIA_AIA_171.jp2"
        movie_request = ("https://api.swhv.oma.be/hv_docpage/v2/getJPX/?sourceId=10"
                         "&startTime=2026-09-08T12:00:00Z&endTime=2026-09-10T12:00:00Z"
                         "&cadence=1800&verbose=true&linked=true&jpip=true")
        print("Preparing movie through " + movie_request, flush=True)
        with urlopen(movie_request, timeout=180) as response:
            movie = json.load(response)["uri"]
        print("Running J2KReaderTest (single image and movie)", flush=True)
        subprocess.run([*java, "-Xmx512m", "org.helioviewer.jhv.source.J2KReaderTest", *libraries, uri, movie],
                       check=True, timeout=360)
        subprocess.run([*java, "-Xmx512m", "-Djhv.test.timeoutSeconds=480",
                        "-Djhv.test.cleaner=" + str(args.cleaner).lower(),
                        "org.helioviewer.jhv.layers.FrameDecoderTest", *libraries, movie],
                       check=True, timeout=510)

        callisto = "https://api.swhv.oma.be/hv_docpage/v2/getJP2Image/?sourceId=5000&date=2026-09-09T00:00:00Z"
        print("Retrieving " + callisto, flush=True)
        with urlopen(callisto, timeout=60) as response:
            (work / "callisto.jp2").write_bytes(response.read())
        subprocess.run([*java, "-Xmx512m", "org.helioviewer.jhv.timelines.radio.CallistoTest",
                        *libraries, str(work / "callisto.jp2")], check=True, timeout=180)
