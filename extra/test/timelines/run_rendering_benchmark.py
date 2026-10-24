#!/usr/bin/env python3
"""Benchmark the current chart renderer on accelerated Java2D surfaces (desktop required).

Run each device scale in a separate JVM. Outputs and the isolated JHV home
stay in --output.
Run ant compile first. No network access or normal user settings are used.
"""

import argparse
import os
import platform
import shutil
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--output", type=Path, required=True)
parser.add_argument("--scale", type=int, choices=[1, 2], default=2, help="Java2D device scale (separate JVM per scale)")
parser.add_argument("--ffmpeg", type=Path, help="override the bundled encoder")
parser.add_argument("--workload", choices=["reduced", "full", "multicolor"], help="run only one fixture")
args = parser.parse_args()
root = Path(__file__).resolve().parents[3]
output = args.output.resolve()
output.mkdir(parents=True, exist_ok=False)
classes = output / "classes"
classes.mkdir()
classpath = os.pathsep.join([str(root / "bin"), str(root / "resources"), str(root / "lib"),
                            *(str(p) for p in sorted((root / "lib").rglob("*.jar")))])
saved_sources = output / "sources"
saved_sources.mkdir()
benchmark = root / "extra/test/timelines/TimelineRenderingBenchmark.java"
shutil.copy2(benchmark, saved_sources / benchmark.name)
band_source = root / "src/org/helioviewer/jhv/timelines/band/Band.java"
shutil.copy2(band_source, saved_sources / band_source.name)
renderer = root / "src/org/helioviewer/jhv/timelines/chart/ChartDrawGraphPane.java"
shutil.copy2(renderer, saved_sources / renderer.name)
subprocess.run(["javac", "-cp", classpath, "-d", str(classes), str(benchmark)], check=True)
command = ["java", "-Dsun.java2d.uiScale=" + str(args.scale), "-Djava.awt.headless=false", "-Duser.timezone=UTC", "-Duser.home=" + str(output / "home")]
if args.workload:
    command.append("-Djhv.benchmark.workload=" + args.workload)
system = {"Darwin": "macos", "Linux": "linux", "Windows": "windows"}[platform.system()]
architecture = {"arm64": "arm64", "aarch64": "arm64", "x86_64": "amd64", "AMD64": "amd64"}[platform.machine()]
ffmpeg = args.ffmpeg or root / "lib/jhv" / (system + "-" + architecture) / "ffmpeg"
command += ["-cp", os.pathsep.join([str(classes), classpath]),
            "org.helioviewer.jhv.timelines.band.TimelineRenderingBenchmark", str(output),
            str(ffmpeg.resolve())]
with (output / "command.txt").open("w") as recorded:
    recorded.write("\n".join(command) + "\n")
with (output / "results.txt").open("w") as log:
    result = subprocess.run(command, stdout=log, stderr=subprocess.STDOUT)
print((output / "results.txt").read_text(), end="")
raise SystemExit(result.returncode)
