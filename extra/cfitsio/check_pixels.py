#!/usr/bin/env python3
"""Check a native FITS reader against independent reference pixels, without JHV."""

import argparse
from concurrent.futures import ThreadPoolExecutor
import ctypes
import hashlib
import json
import math
from pathlib import Path
import struct
import sys


DATA = Path(__file__).resolve().parents[1] / "test/data/fits"


def canonical_bytes(output, bitpix):
    data = bytes(output)
    if bitpix < 0:
        values = memoryview(output).cast("B").cast("f" if bitpix == -32 else "d")
        normalized = None
        for index, value in enumerate(values):
            if math.isnan(value):
                if normalized is None:
                    normalized = bytearray(data)
                if bitpix == -32:
                    struct.pack_into("<I", normalized, index * 4, 0x7FC00000)
                else:
                    struct.pack_into("<Q", normalized, index * 8, 0x7FF8000000000000)
        if normalized is not None:
            return bytes(normalized)
    return data


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
    read = library.jhv_fits_read
    read.argtypes = ([ctypes.c_void_p, ctypes.c_int64] + [ctypes.c_int32] * 9
                     + [ctypes.c_void_p, ctypes.c_int64, ctypes.c_void_p])
    read.restype = ctypes.c_int

    def decode(input_bytes, hdu, bitpix, width, height, plane, first=0, rows=None, step_x=1, step_y=1):
        if rows is None:
            rows = height
        size = ((width + step_x - 1) // step_x) * ((rows + step_y - 1) // step_y) * (abs(bitpix) // 8)
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
        if canonical_bytes(actual, -32) != expected:
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
        width, height = fixture["width"], fixture["height"]
        pixel_size = abs(bitpix) // 8
        plane_size = width * height * pixel_size
        for plane in range(fixture["planes"]):
            expected = reference[plane * plane_size:(plane + 1) * plane_size]
            if canonical_bytes(decode(input_bytes, hdu, bitpix, width, height, plane), bitpix) != expected:
                raise AssertionError(f"Full-plane pixels differ: {path.name}, plane {plane}")
            expected_subset = b"".join(expected[(y * width + x) * pixel_size:(y * width + x + 1) * pixel_size]
                                       for y in range(0, height, 4) for x in range(0, width, 3))
            subset = decode(input_bytes, hdu, bitpix, width, height, plane, step_x=3, step_y=4)
            if canonical_bytes(subset, bitpix) != expected_subset:
                raise AssertionError(f"Subset pixels differ: {path.name}, plane {plane}")
            for row in range(0, height, 8):
                rows = min(8, height - row)
                band = decode(input_bytes, hdu, bitpix, width, height, plane, row, rows)
                if canonical_bytes(band, bitpix) != expected[row * width * pixel_size:(row + rows) * width * pixel_size]:
                    raise AssertionError(f"Band pixels differ: {path.name}, plane {plane}, row {row}")
        print(f"PASS full/subset/band planes: {path.name}", flush=True)

    input_bytes = ctypes.create_string_buffer((DATA / "multi.fits").read_bytes())
    if bytes(decode(input_bytes, 1, 8, 70, 35, 0)) != (DATA / "byte.raw").read_bytes():
        raise AssertionError("Primary HDU pixels differ")
    for plane in range(2):
        expected = struct.pack("<99f", *range(plane * 99, (plane + 1) * 99))
        if bytes(decode(input_bytes, 2, -32, 11, 9, plane)) != expected:
            raise AssertionError("Selected extension HDU/plane pixels differ")
    print("PASS selected HDUs with different dimensions and planes", flush=True)

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
