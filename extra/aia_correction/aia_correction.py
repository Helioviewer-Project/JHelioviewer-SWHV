#!./bin/python

import astropy.units as u
from aiapy.calibrate.util import get_correction_table
import numpy as np

CALIBRATION_VERSION = 9

table = get_correction_table()

# find max calibration version for 4500
w4500 = 4500 * u.angstrom
table4500 = table[table["WAVELNTH"] == w4500]
maxCal4500 = np.max(table4500["VER_NUM"])

table = table[
    (table["VER_NUM"] == CALIBRATION_VERSION)
    | ((table["WAVELNTH"] == w4500) & (table["VER_NUM"] == maxCal4500))
]
table = table["WAVE_STR", "WAVELNTH", "T_START", "T_STOP", "EFF_AREA", "EFFA_P1"]

#table.write("correction.csv", format="pandas.csv")

waves = list(set(table["WAVELNTH"]))

for wave in waves:
    thin = "_THIN" if wave not in (1600, 1700, 4500) * u.angstrom else ""
    wtable = table[table["WAVE_STR"] == f"{wave.value:.0f}{thin}"]

    wtable = wtable["T_START", "T_STOP", "EFF_AREA", "EFFA_P1"]
    wtable.write(f"{wave.value:.0f}" + "_aia_response.json", format="pandas.json")
