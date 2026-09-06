#!/usr/bin/env python3
"""Compile and run the offline event regressions after `ant compile` (JDK 25)."""

import gzip
import json
import os
from pathlib import Path
import subprocess
import tempfile


root = Path(__file__).resolve().parents[3]
tests = {
    "HEKHandlerTest": "plugins.swek.sources",
    "EventDatabaseTest": "database",
    "EventCacheTest": "event",
    "EventFilterRequestsTest": "event.filter",
    "SWEKIntegrationTest": "plugins.swek",
}
classpath = os.pathsep.join([str(root / "bin"), str(root / "resources"), *(str(p) for p in (root / "lib").rglob("*.jar"))])
with tempfile.TemporaryDirectory(prefix="jhv-event-tests-") as workspace:
    work = Path(workspace)
    classes = work / "classes"
    classes.mkdir()
    fixture = work / "hek-events.json"
    fixture.write_bytes(gzip.decompress((root / "extra/test/swek/hek-events.json.gz").read_bytes()))
    subprocess.run([
        "javac", "-cp", classpath, "-d", str(classes),
        *("extra/test/swek/" + name + ".java" for name in tests),
    ], cwd=root, check=True)

    def run(name, *args, cache=None, resources=None):
        home = work / (cache or name)
        home.mkdir(exist_ok=True)
        subprocess.run([
            "java", "--enable-native-access=ALL-UNNAMED", "-Djava.awt.headless=true",
            "-Duser.timezone=UTC", "-Duser.language=en", "-Duser.country=US", "-Duser.home=" + str(home),
            "-cp", os.pathsep.join([str(resources or classes), str(classes), classpath]),
            "org.helioviewer.jhv." + tests[name] + "." + name, *args,
        ], cwd=root, check=True)

    for scenario in ("duplicate-supplier", "missing-definition", "disabled-source"):
        config = json.loads((root / "resources/settings/SWEK.json").read_text())
        suppliers = config["events_types"][0]["suppliers"]
        if scenario == "duplicate-supplier":
            suppliers[1]["id"] = suppliers[0]["id"]
        elif scenario == "missing-definition":
            del config["sources"][0]["numeric_parameters"]["ar_noaanum"]
        else:
            suppliers[0]["source"] = "COMESEP"
        resources = work / scenario
        (resources / "settings").mkdir(parents=True)
        (resources / "settings/SWEK.json").write_text(json.dumps(config))
        run("SWEKIntegrationTest", "config-" + scenario, cache=scenario, resources=resources)

    for name in tests:
        if name == "EventDatabaseTest":
            for scenario in ("store", "reload", "parameters", "relations", "decoding"):
                cache = "persistence" if scenario in ("store", "reload") else scenario
                run(name, scenario, cache=cache)
        elif name == "EventCacheTest":
            for scenario in ("groups", "downloads"):
                run(name, scenario, cache=scenario)
        elif name in ("HEKHandlerTest", "SWEKIntegrationTest"):
            run(name, str(fixture))
        else:
            run(name)
