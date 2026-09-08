#!/usr/bin/env python3
"""Compile and run the offline timeline regressions after `ant compile` (JDK 25).

Capture once with --capture-stix DIRECTORY --start UTC_TIMESTAMP --end UTC_TIMESTAMP.
Replay with --hapi-benchmark DIRECTORY. The capture stays outside the repository;
replay writes overlay/stacked PNGs into that directory. Captures can contain single-color
or multicolor line plots. Benchmarks report elapsed time and allocations, not CPU time
or whole-application memory usage.
"""

import argparse
import json
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile
from urllib.parse import urlencode
from urllib.request import urlopen


parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--benchmark", action="store_true", help="also measure incremental loading, drawing, allocations and cache footprint")
parser.add_argument("--hapi-benchmark", type=Path, help="benchmark a saved HAPI capture offline, with overlay/stacked plots at 1x and 2x")
parser.add_argument("--capture-stix", type=Path, help="save all five STIX datasets here for repeatable offline benchmarks, then exit")
parser.add_argument("--start", help="capture start, e.g. 2026-09-01T00:00:00Z")
parser.add_argument("--end", help="capture end, e.g. 2026-09-08T00:00:00Z")
args = parser.parse_args()

if args.capture_stix:
    if not args.start or not args.end:
        parser.error("--capture-stix requires --start and --end")
    destination = args.capture_stix.resolve()
    destination.mkdir(parents=True, exist_ok=False)
    endpoint = "https://hapi.swhv.oma.be/SWHV_Timelines/hapi/"
    def capture(path, filename):
        url = endpoint + path
        print("Capturing " + url, flush=True)
        with urlopen(url, timeout=120) as response:
            body = response.read()
        (destination / filename).write_bytes(body)
        with (destination / "sources.txt").open("a") as sources:
            sources.write(url + "\n")
        return body
    catalog = json.loads(capture("catalog", "catalog.json"))
    catalog["catalog"] = [item for item in catalog["catalog"] if item["id"].startswith("STIX.")]
    if len(catalog["catalog"]) != 5:
        raise RuntimeError("Expected five STIX datasets")
    (destination / "catalog.json").write_text(json.dumps(catalog))
    for dataset in catalog["catalog"]:
        identifier = dataset["id"]
        capture("info?" + urlencode({"id": identifier}), identifier + ".info.json")
        capture("data?" + urlencode({"id": identifier, "time.min": args.start, "time.max": args.end, "format": "binary"}), identifier + ".bin")
    (destination / "range.txt").write_text(args.start + "\n" + args.end + "\n")
    print("Saved " + str(destination))
    raise SystemExit(0)
if args.start or args.end:
    parser.error("--start and --end apply only to --capture-stix")

root = Path(__file__).resolve().parents[3]
classpath = os.pathsep.join([str(root / "bin"), str(root / "resources"), *(str(p) for p in (root / "lib").rglob("*.jar"))])
with tempfile.TemporaryDirectory(prefix="jhv-timeline-tests-") as classes:
    subprocess.run([
        "javac", "-cp", classpath, "-d", classes,
        str(root / "extra/test/timeline/TimelineDataTest.java"),
    ], check=True)
    benchmark_options = []
    if args.benchmark:
        agent = Path(classes) / "timeline-benchmark-agent.jar"
        with zipfile.ZipFile(agent, "w") as jar:
            jar.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nPremain-Class: org.helioviewer.jhv.timelines.band.TimelineDataTest\n\n")
            for compiled in Path(classes).rglob("*.class"):
                jar.write(compiled, compiled.relative_to(classes))
        benchmark_options = ["-javaagent:" + str(agent), "--add-opens=java.base/java.util=ALL-UNNAMED"]
    subprocess.run([
        "java", "-Djava.awt.headless=true", "-Duser.timezone=UTC", "-Duser.home=" + classes,
        *benchmark_options,
        "-cp", os.pathsep.join([classes, classpath]),
        "org.helioviewer.jhv.timelines.band.TimelineDataTest",
        *(["--benchmark"] if args.benchmark else []),
        *(["--hapi-benchmark", str(args.hapi_benchmark.resolve())] if args.hapi_benchmark else []),
    ], check=True)
