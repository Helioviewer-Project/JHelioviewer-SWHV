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
parser.add_argument("--bridge", help="libjhvj2k to use instead of the one in lib/jhv")
args = parser.parse_args()
if args.cleaner and not args.live:
    parser.error("--cleaner requires --live")
root = Path(__file__).resolve().parents[3]
classpath = os.pathsep.join([str(root / "bin"), str(root / "resources"), str(root / "lib"),
                             *(str(p) for p in sorted((root / "lib").rglob("*.jar")))])
with tempfile.TemporaryDirectory(prefix="jhv-jpip-tests-") as temporary:
    work = Path(temporary)
    if platform.system() == "Windows" and not temporary.isascii():
        parser.error("Set TMPDIR to a writable ASCII path so the Windows test cache stays isolated")
    sources = ["JPIPCacheManagerTest.java", "HTTPStreamTest.java", "JPIPSocketTest.java", "ImageBufferCacheTest.java", "J2KReaderTest.java", "FramesTest.java", "ImageRequestSettingsTest.java", "../uri/FramesLoadTest.java", "../uri/LayerLoadTest.java"]
    # Compile live tests offline too, so API changes cannot silently break them.
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
    for proxy in ["http", "socks"]:
        profiles = ["stored", "override", "partial", "missing", "wrong", "bypass", "failover"] + (["hostname", "blocked", "mixed"] if proxy == "http" else [])
        for profile in profiles:
            print(f"Running {proxy} proxy: {profile}", flush=True)
            subprocess.run([*java, "-Xmx64m", "org.helioviewer.jhv.source.jpip.JPIPSocketTest", proxy, profile],
                           check=True, timeout=60)
    print("Running ImageRequestSettingsTest", flush=True)
    subprocess.run([*java, "org.helioviewer.jhv.io.ImageRequestSettingsTest"], check=True, timeout=60)
    print("Running FramesTest", flush=True)
    subprocess.run([*java, "-Dorg.lwjgl.util.DebugAllocator=true", "org.helioviewer.jhv.layers.FramesTest"], check=True, timeout=60)
    fits = work / "fits"
    fits.mkdir()
    inputs = []
    for name in ["psp_L3_wispr_20231227T150508_V1_1211.fits", "psp_L3_wispr_20231227T150704_V1_2222.fits", "solo_L2_eui-fsi174-image_20251002T150055171_V00.fits"]:
        path = fits / name
        path.write_bytes((root / "extra/test/data" / name).read_bytes())
        inputs.append(str(path))
    bogus = fits / "not-an-image.txt"
    bogus.write_text("Not an image\n")
    archive = work / "two.zip"
    with zipfile.ZipFile(archive, "w") as zip_file:
        for path in inputs[:2]:
            zip_file.write(path, Path(path).name)
    print("Running FramesLoadTest", flush=True)
    subprocess.run([*java, "org.helioviewer.jhv.layers.FramesLoadTest", *inputs, str(archive), str(bogus)], check=True, timeout=60)
    print("Running LayerLoadTest", flush=True)
    subprocess.run([*java, "org.helioviewer.jhv.layers.LayerLoadTest", inputs[0], str(bogus)], check=True, timeout=60)
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
        native_directory = root / "lib/jhv" / (target if target == "macos-arm64" else target + "-amd64")
        Path(libraries[0]).write_bytes((native_directory / ("libkdu_jni" + suffix)).read_bytes())
        Path(libraries[1]).write_bytes(Path(args.bridge).read_bytes() if args.bridge
                                      else (native_directory / ("libjhvj2k" + suffix)).read_bytes())
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
        callisto = "https://api.swhv.oma.be/hv_docpage/v2/getJP2Image/?sourceId=5000&date=2026-09-09T00:00:00Z"
        print("Retrieving " + callisto, flush=True)
        with urlopen(callisto, timeout=60) as response:
            (work / "callisto.jp2").write_bytes(response.read())
        subprocess.run([*java, "-Xmx512m", "-Djhv.test.timeoutSeconds=480",
                        "-Djhv.test.cleaner=" + str(args.cleaner).lower(),
                        "org.helioviewer.jhv.layers.FrameDecoderTest", *libraries, movie, uri, str(work / "callisto.jp2")],
                       check=True, timeout=510)

        subprocess.run([*java, "-Xmx512m", "org.helioviewer.jhv.timelines.radio.CallistoTest",
                        *libraries, str(work / "callisto.jp2")], check=True, timeout=180)
