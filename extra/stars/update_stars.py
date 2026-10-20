#!/usr/bin/env python3
# Regenerate resources/data/stars.bin from the Gaia archive.
#
#   python3 extra/stars/update_stars.py [--release gaiadr3] [--table gaia_source_lite] [--gmax 9]
#
# Gaia data: ESA/Gaia/DPAC, CC BY-NC 3.0 IGO, https://www.cosmos.esa.int/web/gaia-users/license
#
# Layout, big-endian:
#   header: "JHVSTARS", int version, double ref_epoch [Julian year], int count
#   star:   double ra, double dec [deg], float pmra, float pmdec [mas/yr, pmra with cos(dec)],
#           float parallax [mas], float radial_velocity [km/s], float phot_g_mean_mag, int hip
#   missing pmra, pmdec, parallax, radial_velocity are 0; missing hip is 0; sorted by magnitude

import argparse
import csv
import io
import math
import struct
import sys
import urllib.parse
import urllib.request
from pathlib import Path

TAP = "https://gea.esac.esa.int/tap-server/tap/sync"
MAXREC = 5_000_000
VERSION = 1
OUTPUT = Path(__file__).resolve().parents[2] / "resources" / "data" / "stars.bin"


def query(adql):
    data = urllib.parse.urlencode({"REQUEST": "doQuery", "LANG": "ADQL", "FORMAT": "csv", "MAXREC": MAXREC, "QUERY": adql}).encode()
    with urllib.request.urlopen(urllib.request.Request(TAP, data), timeout=600) as response:
        return list(csv.DictReader(io.TextIOWrapper(response, encoding="utf-8")))


def number(value):
    return float(value) if value else 0.


def main():
    parser = argparse.ArgumentParser(description="Regenerate the bundled Gaia star catalogue")
    parser.add_argument("--release", default="gaiadr3")
    parser.add_argument("--table", default="gaia_source_lite")
    parser.add_argument("--gmax", type=float, default=9)
    parser.add_argument("--output", type=Path, default=OUTPUT)
    args = parser.parse_args()

    source = f"{args.release}.{args.table}"
    epoch = float(query(f"SELECT TOP 1 ref_epoch FROM {args.release}.gaia_source")[0]["ref_epoch"])  # one epoch per release

    rows = query(
        "SELECT g.ra, g.dec, g.pmra, g.pmdec, g.parallax, g.radial_velocity, g.phot_g_mean_mag, "
        "h.original_ext_source_id AS hip "
        f"FROM {source} AS g LEFT OUTER JOIN {args.release}.hipparcos2_best_neighbour AS h ON g.source_id = h.source_id "
        f"WHERE g.phot_g_mean_mag < {args.gmax} ORDER BY g.phot_g_mean_mag")
    if len(rows) >= MAXREC:
        sys.exit(f"Result truncated at {MAXREC} rows")

    out = bytearray(b"JHVSTARS")
    out += struct.pack(">idi", VERSION, epoch, len(rows))
    for row in rows:
        ra, dec, mag = float(row["ra"]), float(row["dec"]), float(row["phot_g_mean_mag"])
        if not (math.isfinite(ra) and math.isfinite(dec) and math.isfinite(mag)):
            sys.exit(f"Invalid row {row}")
        out += struct.pack(">ddfffffi", ra, dec, number(row["pmra"]), number(row["pmdec"]), number(row["parallax"]),
                           number(row["radial_velocity"]), mag, int(row["hip"] or 0))

    args.output.write_bytes(out)
    hip = sum(1 for row in rows if row["hip"])
    print(f"{args.output}: {len(rows)} stars G < {args.gmax} ({hip} with HIP), epoch J{epoch}, {len(out)} bytes")


if __name__ == "__main__":
    main()
