#!/usr/bin/env python3
"""Check prebuilt KDU and bridge libraries through JHV's FFM binding."""

import argparse
import os
from pathlib import Path
import struct
import subprocess
import tempfile


def palette_fixtures(folder):
    # Lossless 4x1 codestreams of samples 0,1,2,3, encoded at 8 and 2 bits.
    prefix = "ff4fff510029000000000004000000010000000000000000000000040000000100000000000000000001"
    comment = "ff640025000143726561746564206279204f70656e4a5045472076657273696f6e20322e352e34"
    streams = {
        8: bytes.fromhex(prefix + "070101ff52000c00000001000004040001ff5c00044040" + comment + "ff90000a0000000000170001ff93df8030065ea9b3787fffd9"),
        2: bytes.fromhex(prefix + "010101ff52000c00000001000004040001ff5c00044010" + comment + "ff90000a0000000000120001ff93dd080669ffd9"),
    }

    def box(kind, body):
        return struct.pack(">I4s", len(body) + 8, kind) + body

    def image(name, index_bits=8, index_signed=False, entries=4, gray=False,
              columns=(0, 1, 2), swap=False, bits=8, signed=False, samples=None):
        stream = bytearray(streams[2 if index_bits == 2 else 8])
        stream[42] = (128 if index_signed else 0) | (index_bits - 1)
        rgb = [(0, 0, 0), (255, 0, 0), (0, 255, 0), (0, 0, 255)]
        if samples is not None:
            rgb = [(value, value, value) for value in samples]
        values = b"".join((value & ((1 << bits) - 1)).to_bytes((bits + 7) // 8, "big")
                          for row in rgb[:entries] for value in row)
        depths = bytes([(128 if signed else 0) | (bits - 1)] * 3)
        header = box(b"ihdr", struct.pack(">IIHBBBB", 1, 4, 1, stream[42], 7, 0, 0))
        header += box(b"colr", bytes([1, 0, 0]) + struct.pack(">I", 17 if gray else 16))
        header += box(b"pclr", struct.pack(">HB", entries, 3) + depths + values)
        header += box(b"cmap", b"".join(struct.pack(">HBB", 0, 1, column) for column in (columns[:1] if gray else columns)))
        if swap:
            header += box(b"cdef", struct.pack(">H", 3) + b"".join(struct.pack(">HHH", c, 0, a) for c, a in [(0, 3), (1, 2), (2, 1)]))
        data = box(b"jP  ", b"\r\n\x87\n") + box(b"ftyp", b"jp2 \0\0\0\0jp2 ")
        data += box(b"jp2h", header) + box(b"jp2c", stream)
        (folder / (name + ".jp2")).write_bytes(data)

    image("small")
    image("index2", index_bits=2)
    image("clamped", entries=2)
    image("gray", gray=True, columns=(2,))
    image("swap", swap=True)
    image("mapped", columns=(2, 0, 1))
    image("unsigned3", bits=3, samples=[0, 2, 4, 7])
    image("signed4", bits=4, signed=True, samples=[-8, -3, 2, 7])
    image("unsigned10", bits=10, samples=[0, 341, 682, 1023])
    image("signed10", bits=10, signed=True, samples=[-512, -171, 170, 511])
    image("index9", index_bits=9)
    image("signed-index", index_signed=True)


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
        palette_fixtures(Path(directory))
        subprocess.run([
            "javac", "--release", "25", "-nowarn", "-cp", classpath, "-d", directory,
            str(root / "src/org/helioviewer/jhv/source/J2KNative.java"),
            str(root / "extra/test/j2k/J2KNativeTest.java"),
        ], check=True, timeout=60)
        java_args = ["--enable-native-access=ALL-UNNAMED", "-cp", classpath + os.pathsep + directory,
                     "org.helioviewer.jhv.source.J2KNativeTest"]
        subprocess.run(["java", "-Xmx64m", *java_args, "--responses", str(args.libraries.resolve())], check=True, timeout=30)
        subprocess.run([
            "java", *java_args, "--local",
            str(args.libraries.resolve()), directory, *(str(image) for image in images),
        ], check=True, timeout=300)


if __name__ == "__main__":
    main()
