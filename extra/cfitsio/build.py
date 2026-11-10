#!/usr/bin/env python3
"""Build the pinned FITS reader with CMake and the JPIP platform toolchains."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess
import tarfile

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
PLATFORMS = {
    "macos-arm64": ("macos-arm64", "libjhvfits.dylib"),
    "macos-x64": ("macos-amd64", "libjhvfits.dylib"),
    "linux-x64": ("linux-amd64", "libjhvfits.so"),
    "windows-x64": ("windows-amd64", "jhvfits.dll"),
}


def output(*command):
    return subprocess.check_output(command, text=True).strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("target", choices=PLATFORMS)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--install", action="store_true")
    args = parser.parse_args()
    destination = args.output.resolve()
    resource, name = PLATFORMS[args.target]
    build = destination / (args.target + "-build")
    manifest = json.loads((HERE / "cfitsio.json").read_text())
    options = []
    tools = None
    if args.target.startswith("macos"):
        if platform.system() != "Darwin":
            parser.error("macOS builds require macOS")
        architecture = "arm64" if args.target == "macos-arm64" else "x86_64"
        options = ["-DCMAKE_OSX_ARCHITECTURES=" + architecture, "-DCMAKE_OSX_DEPLOYMENT_TARGET=13.0"]
    elif args.target == "linux-x64":
        if platform.system() != "Linux" or platform.machine().lower() not in ("x86_64", "amd64"):
            parser.error("Linux x64 builds require Linux x64")
    else:
        if platform.system() != "Windows" or platform.machine().lower() not in ("amd64", "x86_64"):
            parser.error("Windows x64 builds require Windows x64")
        tools = Path(os.environ.get("MINGW_ROOT", r"C:\mingw64")) / "bin"
        options = ["-G", "MinGW Makefiles", "-DCMAKE_C_COMPILER=" + str(tools / "gcc.exe"),
                   "-DCMAKE_CXX_COMPILER=" + str(tools / "g++.exe"),
                   "-DCMAKE_MAKE_PROGRAM=" + str(tools / "make.exe")]
    commands = []

    def configure(source, folder, extra):
        command = ["cmake", "-S", str(source), "-B", str(folder), "-DCMAKE_BUILD_TYPE=Release",
                   "-DCMAKE_POSITION_INDEPENDENT_CODE=ON", "-DCMAKE_C_FLAGS=-ffp-contract=off",
                   *options, *extra]
        commands.append(command)
        subprocess.run(command, check=True)

    def compile_and_install(folder):
        for command in (["cmake", "--build", str(folder), "--parallel", os.environ.get("CMAKE_BUILD_PARALLEL_LEVEL", "8")],
                        ["cmake", "--install", str(folder)]):
            commands.append(command)
            subprocess.run(command, check=True)

    for key in ("cfitsio", "zlib"):
        dependency = manifest[key]
        archive = HERE / f"{key}-{dependency['version']}.tar.xz"
        if hashlib.sha256(archive.read_bytes()).hexdigest() != dependency["archive_sha256"]:
            raise RuntimeError(f"{key} source checksum mismatch")
        source = build / (key + "-source")
        source.mkdir(parents=True, exist_ok=True)
        with tarfile.open(archive) as bundle:
            bundle.extractall(source, filter="data")
    zlib_prefix = build / "zlib-install"
    configure(build / "zlib-source", build / "zlib", ["-DZLIB_BUILD_EXAMPLES=OFF", "-DCMAKE_INSTALL_PREFIX=" + str(zlib_prefix)])
    compile_and_install(build / "zlib")
    cfitsio_prefix = build / "cfitsio-install"
    configure(build / "cfitsio-source", build / "cfitsio", [
        "-DBUILD_SHARED_LIBS=OFF", "-DUSE_CURL=OFF", "-DUSE_PTHREADS=ON",
        "-DTESTS=OFF", "-DITERPROGS=OFF", "-DUTILS=OFF",
        "-DZLIB_USE_STATIC_LIBS=ON", "-DZLIB_ROOT=" + str(zlib_prefix),
        "-DCMAKE_INSTALL_PREFIX=" + str(cfitsio_prefix), "-DCMAKE_INSTALL_LIBDIR=lib"])
    compile_and_install(build / "cfitsio")
    configure(ROOT / "native/fits", build / "adapter", ["-DCMAKE_PREFIX_PATH=" + str(cfitsio_prefix)])
    command = ["cmake", "--build", str(build / "adapter"), "--target", "jhvfits", "--parallel",
               os.environ.get("CMAKE_BUILD_PARALLEL_LEVEL", "8")]
    commands.append(command)
    subprocess.run(command, check=True)
    library = build / "adapter" / name
    if args.target.startswith("macos"):
        if output("lipo", "-archs", str(library)) != architecture:
            raise ValueError("Unexpected macOS reader architecture")
        if not re.search(r"\bminos\s+13\.0\b", output("otool", "-l", str(library))):
            raise ValueError("Reader must target macOS 13.0")
        if re.search(r"lib(?:cfitsio|z)\.", output("otool", "-L", str(library))):
            raise ValueError("CFITSIO and zlib must be linked statically")
    elif args.target == "linux-x64":
        if not re.search(r"ELF 64-bit.*x86-64", output("file", str(library))):
            raise ValueError("Reader is not Linux x64")
        versions = re.findall(r"\bGLIBC_(\d+)\.(\d+)(?:\.(\d+))?", output("readelf", "--version-info", str(library)))
        if any(tuple(int(v or 0) for v in version) > (2, 17, 0) for version in versions):
            raise ValueError("Reader requires a glibc newer than 2.17; use the JPIP manylinux2014 image")
        if re.search(r"lib(?:cfitsio|z|stdc\+\+|gcc_s)\.", output("readelf", "-d", str(library))):
            raise ValueError("Unexpected dynamic dependency")
    else:
        subprocess.run([str(tools / "strip.exe"), "--strip-unneeded", "--preserve-dates", str(library)], check=True)
        headers = output(str(tools / "objdump.exe"), "-p", str(library))
        if ("pei-x86-64" not in headers or not re.search(r"MajorSubsystemVersion\s+5\b", headers)
                or not re.search(r"MinorSubsystemVersion\s+2\b", headers)):
            raise ValueError("Reader must be an x64 DLL with subsystem version 5.2")
        if re.search(r"DLL Name:.*(?:cfitsio|zlib|libgcc|libstdc\+\+|libwinpthread|MSVCP|VCRUNTIME).*\.dll", headers, re.IGNORECASE):
            raise ValueError("Unexpected Windows runtime dependency")
    installed = destination / resource / name
    installed.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(library, installed)
    cache = (build / "adapter/CMakeCache.txt").read_text()
    compiler = re.search(r"^CMAKE_C_COMPILER:[^=]+=(.+)$", cache, re.MULTILINE).group(1)
    record = {"platform": args.target, "source": manifest, "commands": commands,
              "cmake": output("cmake", "--version").splitlines()[0],
              "compiler": output(compiler, "--version").splitlines()[0],
              "bytes": installed.stat().st_size, "sha256": hashlib.sha256(installed.read_bytes()).hexdigest()}
    (destination / "builds.json").write_text(json.dumps([record], indent=2) + "\n")
    if args.install:
        target = ROOT / "lib/jhv" / resource / name
        staged = target.with_suffix(target.suffix + ".new")
        shutil.copy2(installed, staged)
        staged.replace(target)
    print(f"Built {args.target}: {installed}", flush=True)


if __name__ == "__main__":
    main()
