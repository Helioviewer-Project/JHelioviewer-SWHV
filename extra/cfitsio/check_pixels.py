#!/usr/bin/env python3
"""Check a native FITS reader against independent reference pixels, without JHV."""

import argparse
from concurrent.futures import ThreadPoolExecutor
import ctypes
import gzip
import hashlib
import json
import math
from pathlib import Path
import struct
import sys


DATA = Path(__file__).resolve().parents[1] / "test/data/fits"


def canonical_bytes(output):
    data = bytearray(output)
    for index, (value,) in enumerate(struct.iter_unpack("<f", data)):
        if math.isnan(value):
            struct.pack_into("<I", data, index * 4, 0x7FC00000)
    return bytes(data)


def float_reference(raw, bitpix):
    code = {8: "B", 16: "h", 32: "i", 64: "q", -32: "f", -64: "d"}[bitpix]
    values = [value for (value,) in struct.iter_unpack("<" + code, raw)]
    return canonical_bytes(struct.pack("<" + "f" * len(values), *values))


def scaled_fixture(bitpix):
    cards = ["SIMPLE  =                    T", f"BITPIX  = {bitpix:20d}",
             "NAXIS   =                    2", "NAXIS1  =                    3", "NAXIS2  =                    4",
             "BZERO   =                   10", "BSCALE  =                   -2"]
    values = list(range(12))
    if bitpix > 0:
        cards.append("BLANK   =                    2")
    else:
        values[2] = math.nan
    header = "".join(card.ljust(80) for card in [*cards, "END"]).encode().ljust(2880, b" ")
    code = {8: "B", 16: "h", 32: "i", 64: "q", -32: "f", -64: "d"}[bitpix]
    pixels = struct.pack(">" + code * len(values), *values)
    return header + pixels.ljust(2880, b"\0")


def ieee_fixture(bitpix, compressed):
    def header(cards):
        return "".join(card.ljust(80) for card in [*cards, "END"]).encode().ljust(2880, b" ")

    code = "f" if bitpix == -32 else "d"
    tiny = 2.0 ** -140
    values = [math.inf, -math.inf, math.nan, 0.0, -0.0, tiny]
    pixels = struct.pack(">" + code * len(values), *values)
    expected = float_reference(struct.pack("<" + code * len(values), *values), bitpix)
    if not compressed:
        cards = ["SIMPLE  =                    T", f"BITPIX  = {bitpix:20d}",
                 "NAXIS   =                    2", "NAXIS1  =                    3", "NAXIS2  =                    2"]
        return header(cards) + pixels.ljust(2880, b"\0"), expected, 1
    primary = header(["SIMPLE  =                    T", "BITPIX  =                    8",
                      "NAXIS   =                    0", "EXTEND  =                    T"])
    heap = gzip.compress(pixels, mtime=0)
    extension = header(["XTENSION= 'BINTABLE'", "BITPIX  =                    8", "NAXIS   =                    2",
                        "NAXIS1  =                    8", "NAXIS2  =                    1", f"PCOUNT  = {len(heap):20d}",
                        "GCOUNT  =                    1", "TFIELDS =                    1", "TTYPE1  = 'COMPRESSED_DATA'",
                        "TFORM1  = '1PB'", "ZIMAGE  =                    T", f"ZBITPIX = {bitpix:20d}",
                        "ZNAXIS  =                    2", "ZNAXIS1 =                    3", "ZNAXIS2 =                    2",
                        "ZTILE1  =                    3", "ZTILE2  =                    2", "ZCMPTYPE= 'GZIP_1'"])
    return primary + extension + (struct.pack(">ii", len(heap), 0) + heap).ljust(2880, b"\0"), expected, 2


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("library", type=Path)
    args = parser.parse_args()
    if sys.byteorder != "little":
        raise AssertionError("Reference pixels require a little-endian target")
    library = ctypes.CDLL(str(args.library.resolve()))
    library.jhv_fits_init.argtypes = []
    library.jhv_fits_init.restype = ctypes.c_int
    if library.jhv_fits_init() != 0:
        raise AssertionError("CFITSIO initialization failed")
    read = library.jhv_fits_read_float
    read.argtypes = ([ctypes.c_void_p, ctypes.c_int64] + [ctypes.c_int32] * 9
                     + [ctypes.c_void_p, ctypes.c_int64, ctypes.c_void_p])
    read.restype = ctypes.c_int

    def decode(input_bytes, hdu, bitpix, width, height, plane, first=0, rows=None, step_x=1, step_y=1):
        if rows is None:
            rows = height
        size = ((width + step_x - 1) // step_x) * ((rows + step_y - 1) // step_y) * 4
        output = ctypes.create_string_buffer(size)
        error = ctypes.create_string_buffer(128)
        status = read(input_bytes, ctypes.sizeof(input_bytes) - 1, hdu, bitpix, width, height,
                      plane, first, rows, step_x, step_y, output, size, error)
        if status:
            raise AssertionError(f"FITS read failed ({status}): {error.value.decode(errors='replace')}")
        return output

    fixtures = json.loads((DATA / "manifest.json").read_text())["images"]
    # Exercise first-use quantized reads concurrently, after explicit initialization.
    first = next(item for item in fixtures if item["file"] == "dither2-float.fits")
    input_bytes = ctypes.create_string_buffer((DATA / first["file"]).read_bytes())
    count = first["width"] * first["height"] * 4
    expected = (DATA / "dither2-float.raw").read_bytes()[:count]

    def concurrent_read(_):
        actual = decode(input_bytes, first["hdu"], -32, first["width"], first["height"], 0)
        if canonical_bytes(actual) != expected:
            raise AssertionError("Concurrent first-use quantized pixels differ")

    with ThreadPoolExecutor(max_workers=8) as pool:
        list(pool.map(concurrent_read, range(16)))
    print("PASS concurrent first-use quantized reads", flush=True)

    for fixture in fixtures:
        path = DATA / fixture["file"]
        input_bytes = ctypes.create_string_buffer(path.read_bytes())
        reference = path.with_suffix(".raw").read_bytes()
        if hashlib.sha256(reference).hexdigest() != fixture["sha256"]:
            raise AssertionError(f"Reference checksum differs: {path.name}")
        hdu, bitpix = fixture["hdu"], fixture["bitpix"]
        reference = float_reference(reference, bitpix)
        width, height = fixture["width"], fixture["height"]
        pixel_size = 4
        plane_size = width * height * pixel_size
        for plane in range(fixture["planes"]):
            expected = reference[plane * plane_size:(plane + 1) * plane_size]
            if canonical_bytes(decode(input_bytes, hdu, bitpix, width, height, plane)) != expected:
                raise AssertionError(f"Full-plane pixels differ: {path.name}, plane {plane}")
            expected_subset = b"".join(expected[(y * width + x) * pixel_size:(y * width + x + 1) * pixel_size]
                                       for y in range(0, height, 4) for x in range(0, width, 3))
            subset = decode(input_bytes, hdu, bitpix, width, height, plane, step_x=3, step_y=4)
            if canonical_bytes(subset) != expected_subset:
                raise AssertionError(f"Subset pixels differ: {path.name}, plane {plane}")
            for row in range(0, height, 8):
                rows = min(8, height - row)
                band = decode(input_bytes, hdu, bitpix, width, height, plane, row, rows)
                if canonical_bytes(band) != expected[row * width * pixel_size:(row + rows) * width * pixel_size]:
                    raise AssertionError(f"Band pixels differ: {path.name}, plane {plane}, row {row}")
        print(f"PASS full/subset/band planes: {path.name}", flush=True)

    input_bytes = ctypes.create_string_buffer((DATA / "multi.fits").read_bytes())
    if bytes(decode(input_bytes, 1, 8, 70, 35, 0)) != float_reference((DATA / "byte.raw").read_bytes(), 8):
        raise AssertionError("Primary HDU pixels differ")
    for plane in range(2):
        expected = struct.pack("<99f", *range(plane * 99, (plane + 1) * 99))
        if bytes(decode(input_bytes, 2, -32, 11, 9, plane)) != expected:
            raise AssertionError("Selected extension HDU/plane pixels differ")
    print("PASS selected HDUs with different dimensions and planes", flush=True)

    for bitpix in (-32, -64):
        for compressed in (False, True):
            data, expected, hdu = ieee_fixture(bitpix, compressed)
            input_bytes = ctypes.create_string_buffer(data)
            actual = decode(input_bytes, hdu, bitpix, 3, 2, 0)
            if canonical_bytes(actual) != expected:
                raise AssertionError(f"IEEE values changed: BITPIX {bitpix}, compressed={compressed}")
            size = 4
            subset = decode(input_bytes, hdu, bitpix, 3, 2, 0, step_x=2)
            expected_subset = b"".join(expected[i * size:(i + 1) * size] for i in (0, 2, 3, 5))
            if canonical_bytes(subset) != expected_subset:
                raise AssertionError(f"IEEE subset values changed: BITPIX {bitpix}, compressed={compressed}")
    print("PASS infinities, NaN, signed zero and subnormal values in plain/lossless float images", flush=True)

    for bitpix in (16, 32, 64, -32, -64):
        input_bytes = ctypes.create_string_buffer(scaled_fixture(bitpix))
        values = [10, 8, math.nan, 4, 2, 0, -2, -4, -6, -8, -10, -12]
        expected = canonical_bytes(struct.pack("<12f", *values))
        if canonical_bytes(decode(input_bytes, 1, bitpix, 3, 4, 0)) != expected:
            raise AssertionError(f"Scaling/undefined pixels changed: BITPIX {bitpix}")
        subset = decode(input_bytes, 1, bitpix, 3, 4, 0, step_x=2, step_y=2)
        if canonical_bytes(subset) != canonical_bytes(struct.pack("<4f", 10, math.nan, -2, -6)):
            raise AssertionError(f"Scaled subset differs: BITPIX {bitpix}")
    print("PASS BZERO/BSCALE, integer BLANK, floating NaN, and valid zero/negative values", flush=True)

    input_bytes = ctypes.create_string_buffer(scaled_fixture(-64)[:2880]
            + struct.pack(">12d", *([1e100, -1e100] * 6)).ljust(2880, b"\0"))
    actual = decode(input_bytes, 1, -64, 3, 4, 0)
    if bytes(actual) != struct.pack("<12f", *([-math.inf, math.inf] * 6)):
        raise AssertionError("Double-to-float overflow must retain signed infinity")
    print("PASS finite double overflow narrows to signed infinity", flush=True)

    mask = library.jhv_fits_read_mask
    mask.argtypes = [ctypes.c_void_p, ctypes.c_int64, ctypes.c_int32, ctypes.c_int32,
                     ctypes.c_void_p, ctypes.c_int64, ctypes.c_void_p]
    mask.restype = ctypes.c_int
    input_bytes = ctypes.create_string_buffer(scaled_fixture(8))
    if bytes(decode(input_bytes, 1, 8, 3, 4, 0)) != struct.pack("<12f", *range(12)):
        raise AssertionError("Byte images must retain their raw display values")
    output = ctypes.create_string_buffer(12)
    error = ctypes.create_string_buffer(128)
    if mask(input_bytes, ctypes.sizeof(input_bytes) - 1, 3, 4, output, 12, error) or bytes(output) != bytes(range(12)):
        raise AssertionError("Mask reader must retain raw byte values, including BLANK")
    print("PASS raw byte mask ignores image scaling and BLANK", flush=True)

    error = ctypes.create_string_buffer(128)
    output = ctypes.create_string_buffer(4)
    invalid = ctypes.create_string_buffer(b"not a FITS file")
    for input_bytes, hdu, capacity in ((invalid, 1, 4), (input_bytes, 999, 4), (input_bytes, 1, 0)):
        if read(input_bytes, ctypes.sizeof(input_bytes) - 1, hdu, 8, 1, 1, 0, 0, 1, 1, 1,
                output, capacity, error) == 0:
            raise AssertionError("Malformed input, invalid HDU, or undersized output accepted")
    print("PASS malformed input, HDU and output-capacity errors", flush=True)


if __name__ == "__main__":
    main()
