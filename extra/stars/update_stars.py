#!/usr/bin/env python3
# Regenerate resources/data/stars.bin from the Gaia archive.
#
#   python3 extra/stars/update_stars.py [--release gaiadr3] [--table gaia_source_lite] [--gmax 9]
#
# Gaia data: ESA/Gaia/DPAC, CC BY-NC 3.0 IGO, https://www.cosmos.esa.int/web/gaia-users/license
#
# Stars too bright for Gaia come from Hipparcos (new reduction, J1991.25), moved to the Gaia epoch,
# with V converted to G by the Gaia DR3 G - V = f(V - I_C) relation (documentation Table 5.9).
#
# Layout, big-endian:
#   header: "JHVSTARS", int version, double ref_epoch [Julian year], int count
#   star:   double ra, double dec [deg], float pmra, float pmdec [mas/yr, pmra with cos(dec)],
#           float parallax [mas], float radial_velocity [km/s], float G, int hip
#   missing pmra, pmdec, parallax, radial_velocity are 0; missing hip is 0; sorted by magnitude, ra, dec

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
HIPPARCOS_EPOCH = 1991.25
G_V = (-0.01597, -0.02809, -0.2483, 0.03656, -0.002939)  # Gaia DR3 Table 5.9, -0.4 < V - I_C < 5.0
# a Gaia star is the same star within MATCH_ANY at any magnitude (variables, binaries), or within MATCH and MATCH_MAG
MATCH_ANY = math.radians(0.5 / 3600)
MATCH = math.radians(5 / 3600)
MATCH_MAG = 1.


def query(adql):
    data = urllib.parse.urlencode({"REQUEST": "doQuery", "LANG": "ADQL", "FORMAT": "csv", "MAXREC": MAXREC, "QUERY": adql}).encode()
    with urllib.request.urlopen(urllib.request.Request(TAP, data), timeout=600) as response:
        return list(csv.DictReader(io.TextIOWrapper(response, encoding="utf-8")))


def number(value):
    return float(value) if value else 0.


def unit(ra, dec):
    ra, dec = math.radians(ra), math.radians(dec)
    return math.cos(dec) * math.cos(ra), math.cos(dec) * math.sin(ra), math.sin(dec)


# linear motion on the tangent plane, enough for decades
def move(ra, dec, pm_ra, pm_dec, years):
    ra, dec = math.radians(ra), math.radians(dec)
    mas = math.radians(1 / 3.6e6) * years
    east, north = pm_ra * mas, pm_dec * mas
    p = (math.cos(dec) * math.cos(ra) - east * math.sin(ra) - north * math.sin(dec) * math.cos(ra),
         math.cos(dec) * math.sin(ra) + east * math.cos(ra) - north * math.sin(dec) * math.sin(ra),
         math.sin(dec) + north * math.cos(dec))
    return math.degrees(math.atan2(p[1], p[0])) % 360, math.degrees(math.atan2(p[2], math.hypot(p[0], p[1])))


def hipparcos_g(vmag, v_i):
    x = min(max(v_i, -0.4), 5.0)
    return vmag + sum(c * x ** k for k, c in enumerate(G_V))


def same(u, g, row):
    cos = sum(a * b for a, b in zip(u, unit(float(row["ra"]), float(row["dec"]))))
    return cos > math.cos(MATCH_ANY) or (cos > math.cos(MATCH) and abs(float(row["phot_g_mean_mag"]) - g) < MATCH_MAG)


# Hipparcos stars brighter than gmax without a Gaia counterpart, moved to the Gaia epoch, in Gaia row format
def hipparcos_rows(rows, epoch, gmax):
    gaia_hip = {row["hip"] for row in rows if row["hip"]}
    cell = math.degrees(MATCH)
    bands = {}  # Gaia rows by declination band
    for row in rows:
        bands.setdefault(int(float(row["dec"]) // cell), []).append(row)

    years = epoch - HIPPARCOS_EPOCH
    added = []
    for h in query("SELECT n.hip, n.ra, n.dec, n.plx, n.pm_ra, n.pm_de, o.vmag, o.v_i "
                   "FROM public.hipparcos_newreduction AS n JOIN public.hipparcos AS o ON n.hip = o.hip "
                   f"WHERE o.vmag < {gmax + 1}"):
        g = hipparcos_g(float(h["vmag"]), number(h["v_i"]))
        if g >= gmax or h["hip"] in gaia_hip:
            continue
        ra, dec = move(float(h["ra"]), float(h["dec"]), number(h["pm_ra"]), number(h["pm_de"]), years)
        u = unit(ra, dec)
        band = int(dec // cell)
        if any(same(u, g, row) for b in (band - 1, band, band + 1) for row in bands.get(b, [])):
            continue
        added.append({"ra": ra, "dec": dec, "pmra": h["pm_ra"], "pmdec": h["pm_de"], "parallax": h["plx"],
                      "radial_velocity": "", "phot_g_mean_mag": g, "hip": h["hip"]})
    return added


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
        f"WHERE g.phot_g_mean_mag < {args.gmax + MATCH_MAG}")  # fainter ones only to match Hipparcos
    if len(rows) >= MAXREC:
        sys.exit(f"Result truncated at {MAXREC} rows")
    bright = hipparcos_rows(rows, epoch, args.gmax)
    rows = sorted([row for row in rows if float(row["phot_g_mean_mag"]) < args.gmax] + bright, key=lambda row: (float(row["phot_g_mean_mag"]), float(row["ra"]), float(row["dec"])))

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
    print(f"{args.output}: {len(rows)} stars G < {args.gmax} ({hip} with HIP, {len(bright)} from Hipparcos), epoch J{epoch}, {len(out)} bytes")


if __name__ == "__main__":
    main()
