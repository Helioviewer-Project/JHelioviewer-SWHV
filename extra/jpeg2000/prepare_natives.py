#!/usr/bin/env python3
"""Stage matched KDU/bridge libraries without replacing installed files."""

import argparse
import hashlib
from pathlib import Path
import re


PLATFORMS = {
    "macos-arm64": ("macos-arm64", ("libkdu_jni.dylib", "libjhvj2k.dylib")),
    "macos-x64": ("macos-amd64", ("libkdu_jni.dylib", "libjhvj2k.dylib")),
    "linux-x64": ("linux-amd64", ("libkdu_jni.so", "libjhvj2k.so")),
    "windows-x64": ("windows-amd64", ("kdu_v7AR.dll", "kdu_jni.dll", "jhvj2k.dll")),
}


def prepare(root, builds, output, platform):
    resource, names = PLATFORMS[platform]
    build = builds / platform
    info = (build / "build-info.txt").read_text(encoding="utf-8-sig")
    hashes = dict((name, digest) for digest, name in
                  re.findall(r"^([0-9a-f]{64})  ([\w.]+)$", info, re.MULTILINE))
    replacements = {}
    for name in names:
        data = (build / name).read_bytes()
        if hashlib.sha256(data).hexdigest() != hashes.get(name):
            raise ValueError(f"missing or mismatched build checksum: {build / name}")
        replacements[name] = data

    candidate = output / "jhv" / resource
    if candidate.resolve() == (root / "lib/jhv" / resource).resolve():
        raise ValueError("output must be a staging directory outside lib")
    candidate.mkdir(parents=True, exist_ok=True)
    for name, data in replacements.items():
        (candidate / name).write_bytes(data)
    (output / f"{platform}-build-info.txt").write_text(info, encoding="utf-8")
    print(f"Prepared {candidate}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("builds", type=Path, help="parent of the four platform build directories")
    parser.add_argument("output", type=Path, help="directory for staged libraries and build records")
    parser.add_argument("--platform", choices=PLATFORMS, action="append", help="stage only this platform (repeatable)")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    args.output.mkdir(parents=True, exist_ok=True)
    for platform in args.platform or PLATFORMS:
        prepare(root, args.builds, args.output, platform)


if __name__ == "__main__":
    main()
