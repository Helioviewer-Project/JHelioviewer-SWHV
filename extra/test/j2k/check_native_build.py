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


def resolution_fixtures(folder):
    # Lossless zero-filled 64x64 gray images with three and four resolutions.
    streams = {
        3: bytes.fromhex("ff4fff510029000000000040000000400000000000000000000000400000004000000000000000000001070101ff52000c00000001000204040001ff5c000a4040484850484850ff640025000143726561746564206279204f70656e4a5045472076657273696f6e20322e352e34ff90000a00000000001a0001ff93df8038115054aff4c87f8080ffd9"),
        4: bytes.fromhex("ff4fff510029000000000040000000400000000000000000000000400000004000000000000000000001070101ff52000c00000001000304040001ff5c000d4040484850484850484850ff640025000143726561746564206279204f70656e4a5045472076657273696f6e20322e352e34ff90000a0000000000190001ff93df8028115054a36f808080ffd9"),
    }

    def split(stream):
        at, markers = 2, {}
        while stream[at:at + 2] != b"\xff\x90":
            size = int.from_bytes(stream[at + 2:at + 4], "big") + 2
            markers[stream[at + 1]] = stream[at:at + size]
            at += size
        return stream[:at], markers, stream[at:-2]

    _, markers3, tile3 = split(streams[3])
    main4, markers4, tile4 = split(streams[4])

    def marker(code, body):
        return bytes([255, code]) + struct.pack(">H", len(body) + 2) + body

    def coc(markers):
        cod = markers[0x52]
        return marker(0x53, b"\0" + cod[4:5] + cod[9:])

    def qcc(markers):
        return marker(0x5d, b"\0" + markers[0x5c][4:])

    def tile(data, extra=b"", index=0, part=0, parts=1, unknown_length=False):
        value = bytearray(data[:12] + extra + data[12:])
        value[4:6] = struct.pack(">H", index)
        value[6:10] = struct.pack(">I", 0 if unknown_length else len(value))
        value[10:12] = bytes([part, parts])
        return bytes(value)

    def box(kind, body):
        return struct.pack(">I4s", len(body) + 8, kind) + body

    def image(name, levels, main, tiles, width=64, components=1):
        header = box(b"ihdr", struct.pack(">IIHBBBB", 64, width, components, 7, 7, 0, 0))
        header += box(b"colr", b"\1\0\0" + struct.pack(">I", 17))
        data = box(b"jP  ", b"\r\n\x87\n") + box(b"ftyp", b"jp2 \0\0\0\0jp2 ")
        data += box(b"jp2h", header) + box(b"jp2c", main + b"".join(tiles) + b"\xff\xd9")
        (folder / f"resolutions-{levels}-{name}.jp2").write_bytes(data)

    coding3 = markers3[0x52] + markers3[0x5c]
    image("tile-cod", 3, main4, [tile(tile3, coding3)])
    image("tile-coc", 3, main4, [tile(tile3, coc(markers3) + qcc(markers3))])
    image("coc-over-cod", 4, main4,
          [tile(tile4, markers3[0x52] + coc(markers4) + markers3[0x5c] + qcc(markers4))])
    image("main-coc", 3, main4 + coc(markers3) + qcc(markers3), [tile3])
    wide = bytearray(main4)
    wide[8:12] = struct.pack(">I", 128)
    image("second-tile", 3, bytes(wide), [tile4, tile(tile3, coding3, index=1)], width=128)
    image("first-tile", 3, bytes(wide), [tile(tile3, coding3), tile(tile4, index=1)], width=128)
    image("unknown-length", 3, main4, [tile(tile3, coding3, unknown_length=True)])
    # This image's last two resolution packets are the one-byte empty packets.
    payload = tile3[14:]
    parts = [tile(tile3[:14] + payload[:-2], coding3, part=0, parts=3),
             tile(tile3[:14] + payload[-2:-1], part=1, parts=3),
             tile(tile3[:14] + payload[-1:], part=2, parts=3)]
    image("tile-parts", 3, main4, parts)
    bad_cod = bytearray(markers3[0x52])
    bad_cod[2:4] = b"\xff\xff"
    image("bad-cod-length", 0, main4, [tile(tile3, bytes(bad_cod))])
    bad_coc = bytearray(coc(markers3))
    bad_coc[4] = 1
    image("bad-component", 0, main4, [tile(tile3, bytes(bad_coc))])
    bad_part = bytearray(tile(tile3, coding3))
    bad_part[6:10] = struct.pack(">I", 13)
    image("bad-part-length", 0, main4, [bytes(bad_part)])

    # Component 0 has four resolutions; unused component 1 has three.
    mixed = bytes.fromhex("ff4fff51002c000000000040000000400000000000000000000000400000004000000000000000000002070101070101ff52000c00000001000304040001ff5c000d4040484850484850484850ff53000901000204040001ff5d000b014040484850484850ff640025000143726561746564206279204f70656e4a5045472076657273696f6e20322e352e34ff90000a0000000000250001ff93df8028115054a36fdf8038115054aff4c87f8080808080ffd9")
    mixed_main, _, mixed_tile = split(mixed)
    image("unused-component", 3, mixed_main, [mixed_tile], components=2)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("libraries", type=Path, help="directory containing the built native libraries")
    parser.add_argument("--esajpip", type=Path, required=True, help="esajpip source checkout")
    parser.add_argument("--decoder", choices=("kdu", "openjpeg"), default="kdu",
                        help="bridge to validate (default: kdu)")
    parser.add_argument("--tolerance", type=int, default=0,
                        help="largest per-byte difference from Kakadu's compositor, for an OpenJPEG bridge")
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
        resolution_fixtures(Path(directory))
        subprocess.run([
            "javac", "--release", "25", "-nowarn", "-cp", classpath, "-d", directory,
            str(root / "src/org/helioviewer/jhv/wcs/ImageGrid.java"),
            str(root / "src/org/helioviewer/jhv/source/J2KNative.java"),
            str(root / "extra/test/j2k/J2KNativeTest.java"),
        ], check=True, timeout=60)
        bridge = "jhvj2k_kdu" if args.decoder == "kdu" else "jhvj2k_opj"
        java_args = ["--enable-native-access=ALL-UNNAMED", f"-Dj2k.bridge={bridge}", f"-Dj2k.tolerance={args.tolerance}", "-cp", classpath + os.pathsep + directory,
                     "org.helioviewer.jhv.source.J2KNativeTest"]
        subprocess.run(["java", "-Xmx64m", *java_args, "--responses", str(args.libraries.resolve())], check=True, timeout=30)
        subprocess.run([
            "java", *java_args, "--local",
            str(args.libraries.resolve()), directory, *(str(image) for image in images),
        ], check=True, timeout=300)


if __name__ == "__main__":
    main()
