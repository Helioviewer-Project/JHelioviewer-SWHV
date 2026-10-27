#!/usr/bin/env python3
"""Build and record an OpenJPEG bridge for the current host platform."""

import argparse
import hashlib
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess


def output(*command):
    return subprocess.check_output(command, universal_newlines=True).strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("target", choices=("macos-arm64", "macos-x64", "linux-x64", "windows-x64"))
    parser.add_argument("--esajpip", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True, help="parent of platform artifact directories")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    esajpip = args.esajpip.resolve()
    destination = args.output.resolve() / args.target
    build = args.output.resolve() / (args.target + "-build")
    options = []
    tools = None
    if args.target.startswith("macos-"):
        if platform.system() != "Darwin":
            parser.error("macOS builds require macOS")
        architecture = "arm64" if args.target == "macos-arm64" else "x86_64"
        options = ["-DCMAKE_OSX_ARCHITECTURES=" + architecture, "-DCMAKE_OSX_DEPLOYMENT_TARGET=13.0"]
        name = "libjhvj2k_opj.dylib"
    elif args.target == "linux-x64":
        if platform.system() != "Linux" or platform.machine().lower() not in ("x86_64", "amd64"):
            parser.error("Linux x64 builds require Linux x64")
        options = ["-DCMAKE_SHARED_LINKER_FLAGS=-static-libgcc -static-libstdc++"]
        name = "libjhvj2k_opj.so"
    else:
        if platform.system() != "Windows" or platform.machine().lower() not in ("amd64", "x86_64"):
            parser.error("Windows x64 builds require Windows x64")
        tools = Path(os.environ.get("MINGW_ROOT", r"C:\mingw64")) / "bin"
        options = ["-G", "MinGW Makefiles", "-DCMAKE_C_COMPILER=" + str(tools / "gcc.exe"),
                   "-DCMAKE_CXX_COMPILER=" + str(tools / "g++.exe"),
                   "-DCMAKE_MAKE_PROGRAM=" + str(tools / "make.exe")]
        name = "jhvj2k_opj.dll"

    subprocess.run(["cmake", "-S", str(root / "native/jpeg2000"), "-B", str(build),
                    "-DCMAKE_BUILD_TYPE=Release", "-DJHV_J2K_DECODER=openjpeg",
                    "-DESAJPIP_CLIENT_ONLY=ON", "-DESAJPIP=" + str(esajpip), *options], check=True)
    subprocess.run(["cmake", "--build", str(build), "--target", "jhvj2k", "--parallel",
                    os.environ.get("CMAKE_BUILD_PARALLEL_LEVEL", "8")], check=True)
    library = build / name
    if args.target.startswith("macos-"):
        if output("lipo", "-archs", str(library)) != architecture:
            raise ValueError("unexpected macOS bridge architecture")
        headers = output("otool", "-l", str(library))
        if not re.search(r"\bminos\s+13\.0\b", headers):
            raise ValueError("bridge must target macOS 13.0")
    elif args.target == "linux-x64":
        if not re.search(r"ELF 64-bit.*x86-64", output("file", str(library))):
            raise ValueError("bridge is not Linux x64")
        versions = re.findall(r"\bGLIBC_(\d+)\.(\d+)(?:\.(\d+))?", output("readelf", "--version-info", str(library)))
        if any(tuple(int(value or 0) for value in version) > (2, 17, 0) for version in versions):
            raise ValueError("bridge requires a glibc newer than 2.17")
        dynamic = output("readelf", "-d", str(library))
        if "libstdc++" in dynamic or "libgcc_s" in dynamic:
            raise ValueError("GNU runtimes must be linked statically")
    else:
        subprocess.run([str(tools / "strip.exe"), "--strip-unneeded", "--preserve-dates", str(library)], check=True)
        headers = output(str(tools / "objdump.exe"), "-p", str(library))
        if ("pei-x86-64" not in headers or not re.search(r"MajorSubsystemVersion\s+5\b", headers)
                or not re.search(r"MinorSubsystemVersion\s+2\b", headers)):
            raise ValueError("bridge must be an x64 DLL with subsystem version 5.2")
        if re.search(r"DLL Name:.*(?:libgcc|libstdc\+\+|libwinpthread|kdu).*\.dll", headers, re.IGNORECASE):
            raise ValueError("unexpected Windows runtime or Kakadu dependency")

    destination.mkdir(parents=True, exist_ok=True)
    shutil.copy2(str(library), str(destination / name))
    records = []
    for label, checkout in (("JHV", root), ("esajpip", esajpip)):
        records.append(label + ": " + output("git", "-C", str(checkout), "rev-parse", "HEAD"))
        if output("git", "-C", str(checkout), "-c", "core.fsmonitor=false", "status", "--porcelain"):
            records.append("  working tree has local changes")
    records.append(output("cmake", "--version").splitlines()[0])
    cache = (build / "CMakeCache.txt").read_text()
    compiler = re.search(r"^CMAKE_CXX_COMPILER:[^=]+=(.+)$", cache, re.MULTILINE).group(1)
    records.append(output(compiler, "--version").splitlines()[0])
    records.append(hashlib.sha256(library.read_bytes()).hexdigest() + "  " + name)
    (destination / "build-info.txt").write_text("\n".join(records) + "\n")
    print("Created " + str(destination / name))


if __name__ == "__main__":
    main()
