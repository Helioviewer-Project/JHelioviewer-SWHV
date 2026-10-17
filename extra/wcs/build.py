#!/usr/bin/env python3
"""Build the pinned WCSLIB and JHV adapter with Zig, outside the source tree."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tarfile

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
PLATFORMS = {
    "macos-arm64": ("aarch64-macos.13.0", "macos-arm64", "libjhvwcs.dylib"),
    "macos-x64": ("x86_64-macos.13.0", "macos-amd64", "libjhvwcs.dylib"),
    "linux-x64": ("x86_64-linux-gnu.2.28", "linux-amd64", "libjhvwcs.so"),
    "windows-x64": ("x86_64-windows-gnu", "windows-amd64", "jhvwcs.dll"),
}
SOURCES = [
    "flexed/wcsbth.c", "flexed/wcspih.c", "flexed/wcsulex.c", "flexed/wcsutrn.c",
    "cel.c", "dis.c", "lin.c", "log.c", "prj.c", "spc.c", "sph.c", "spx.c",
    "tab.c", "wcs.c", "wcserr.c", "wcsfix.c", "wcshdr.c", "wcsprintf.c",
    "wcsunits.c", "wcsutil.c", "wcstrig.c",
]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    parser.add_argument("--platform", choices=PLATFORMS, action="append")
    parser.add_argument("--install", action="store_true", help="copy the built libraries into lib/jhv")
    args = parser.parse_args()
    manifest = json.loads((HERE / "wcslib.json").read_text())
    if subprocess.check_output(["zig", "version"], text=True).strip() != manifest["zig"]:
        raise RuntimeError(f"Use Zig {manifest['zig']}")
    archive = HERE / f"wcslib-{manifest['version']}.tar.xz"
    if hashlib.sha256(archive.read_bytes()).hexdigest() != manifest["archive_sha256"]:
        raise RuntimeError("WCSLIB source checksum mismatch")
    output = args.output.resolve()
    source = output / "source"
    source.mkdir(parents=True, exist_ok=True)
    with tarfile.open(archive) as bundle:
        bundle.extractall(source, filter="data")
    cdir = source / "C"
    (cdir / "wcsconfig.h").write_text(
        "#define HAVE_WCSLIB_VERSION 1\n#define WCSLIB_VERSION 8.10\n"
        "#define WCSLIB_INT64 long long int\n"
    )
    environment = dict(os.environ, ZIG_GLOBAL_CACHE_DIR=str(output / "zig-cache"))
    builds = []
    for platform in args.platform or PLATFORMS:
        target, resource, name = PLATFORMS[platform]
        destination = output / resource / name
        destination.parent.mkdir(parents=True, exist_ok=True)
        command = ["zig", "cc", "-target", target, "-O3", "-s", "-std=gnu11", "-shared",
                   "-ffp-contract=off", "-fvisibility=hidden", "-DWCSLIB_TLS=_Thread_local",
                   "-I", str(cdir), "-I", str(ROOT / "native/wcs")]
        if platform == "windows-x64":
            command += ["-DYY_NO_UNISTD_H", "-D_NO_OLDNAMES", "-DNO_OLDNAMES",
                        "-D_CRT_SECURE_NO_WARNINGS"]
        if platform.startswith("macos"):
            command += ["-Wl,-install_name,@rpath/" + name]
        command += [str(ROOT / "native/wcs/jhv_wcs.c"), *[str(cdir / f) for f in SOURCES],
                    "-o", str(destination)]
        subprocess.run(command, env=environment, check=True)
        record = {"platform": platform, "target": target, "bytes": destination.stat().st_size,
                  "sha256": hashlib.sha256(destination.read_bytes()).hexdigest(),
                  "source": manifest, "command": command}
        builds.append(record)
        if args.install:
            installed = ROOT / "lib/jhv" / resource / name
            staged = installed.with_suffix(installed.suffix + ".new")
            staged.write_bytes(destination.read_bytes())
            staged.replace(installed)
        print(f"Built {platform}: {record['bytes']} bytes", flush=True)
    (output / "builds.json").write_text(json.dumps(builds, indent=2) + "\n")


if __name__ == "__main__":
    main()
