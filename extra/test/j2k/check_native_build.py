#!/usr/bin/env python3
"""Check prebuilt KDU and bridge libraries through JHV's FFM binding."""

import argparse
import os
from pathlib import Path
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("libraries", type=Path, help="directory containing the built native libraries")
    parser.add_argument("--esajpip", type=Path, required=True, help="esajpip source checkout")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[3]
    fixtures = args.esajpip.resolve() / "tests"
    images = [
        fixtures / "transcode/fixtures/kakadu/synthetic_rgb_129x129_CPRL_SOP_EPH.jp2",
        fixtures / "transcode/fixtures/kakadu/synthetic_rgb_129x129_origin129_CPRL.jp2",
        fixtures / "transcode/fixtures/kakadu/solo_fsi174_127x129_RLCP_PLT.jp2",
        fixtures / "merge/fixtures/expected/merged.jpx",
    ]
    for image in images:
        if not image.is_file():
            parser.error(f"missing fixture: {image}")
    jars = sorted((root / "lib").glob("annotations-*.jar"))
    jars.append(root / "extra/test/j2k/lib/kdu_jni.jar")
    classpath = os.pathsep.join(str(path) for path in jars)
    with tempfile.TemporaryDirectory(prefix="jhv-native-check-") as directory:
        subprocess.run([
            "javac", "--release", "25", "-nowarn", "-cp", classpath, "-d", directory,
            str(root / "src/org/helioviewer/jhv/view/j2k/J2KNative.java"),
            str(root / "extra/test/j2k/J2KNativeTest.java"),
        ], check=True)
        subprocess.run([
            "java", "--enable-native-access=ALL-UNNAMED", "-cp", classpath + os.pathsep + directory,
            "org.helioviewer.jhv.view.j2k.J2KNativeTest", "--local",
            str(args.libraries.resolve()), *(str(image) for image in images),
        ], check=True)


if __name__ == "__main__":
    main()
