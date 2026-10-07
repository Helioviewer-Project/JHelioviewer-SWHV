#!/usr/bin/env python3
"""Stage native JARs from matched KDU/bridge builds without replacing installed JARs."""

import argparse
import copy
import hashlib
from pathlib import Path
import re
import zipfile


PLATFORMS = {
    "macos-arm64": ("macos-arm64", "macos-arm64", ("libkdu_jni.dylib", "libjhvj2k.dylib")),
    "macos-x64": ("macos", "macos-amd64", ("libkdu_jni.dylib", "libjhvj2k.dylib")),
    "linux-x64": ("linux", "linux-amd64", ("libkdu_jni.so", "libjhvj2k.so")),
    "windows-x64": ("windows", "windows-amd64", ("kdu_v7AR.dll", "kdu_jni.dll", "jhvj2k.dll")),
}


def prepare(root, builds, output, platform):
    jar_platform, resource, names = PLATFORMS[platform]
    build = builds / platform
    info = (build / "build-info.txt").read_text(encoding="utf-8-sig")
    hashes = dict((name, digest) for digest, name in
                  re.findall(r"^([0-9a-f]{64})  ([\w.]+)$", info, re.MULTILINE))
    replacements = {}
    for name in names:
        data = (build / name).read_bytes()
        if hashlib.sha256(data).hexdigest() != hashes.get(name):
            raise ValueError(f"missing or mismatched build checksum: {build / name}")
        replacements[f"jhv/{resource}/{name}"] = data

    filename = f"jhv-natives-{jar_platform}.jar"
    original = root / "lib/jhv" / filename
    candidate = output / filename
    if candidate.resolve() == original.resolve():
        raise ValueError("output must be a staging directory outside lib/jhv")
    with zipfile.ZipFile(original) as before:
        if len(before.namelist()) != len(set(before.namelist())):
            raise ValueError(f"duplicate entries in {original}")
        with zipfile.ZipFile(candidate, "w", compression=zipfile.ZIP_DEFLATED) as after:
            after.comment = before.comment
            for entry in before.infolist():
                data = replacements.get(entry.filename)
                after.writestr(copy.copy(entry), before.read(entry) if data is None else data)
            for name, data in replacements.items():
                if name not in before.namelist():
                    entry = zipfile.ZipInfo(name)
                    entry.compress_type = zipfile.ZIP_DEFLATED
                    entry.external_attr = 0o100755 << 16
                    after.writestr(entry, data)
        with zipfile.ZipFile(candidate) as after:
            for name in before.namelist():
                if name not in replacements and before.read(name) != after.read(name):
                    raise ValueError(f"unrelated entry changed: {name}")
            for name, data in replacements.items():
                if after.read(name) != data:
                    raise ValueError(f"native entry differs from build: {name}")
    (output / f"{platform}-build-info.txt").write_text(info, encoding="utf-8")
    print(f"Prepared {candidate}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("builds", type=Path, help="parent of the four platform build directories")
    parser.add_argument("output", type=Path, help="directory for staged JARs and build records")
    parser.add_argument("--platform", choices=PLATFORMS, action="append", help="stage only this platform (repeatable)")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    args.output.mkdir(parents=True, exist_ok=True)
    for platform in args.platform or PLATFORMS:
        prepare(root, args.builds, args.output, platform)


if __name__ == "__main__":
    main()
