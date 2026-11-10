from pathlib import Path
import ctypes as c, json, hashlib
import numpy as np
from astropy.io import fits
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("--cfitsio-library", required=True)
args = parser.parse_args()
lib = c.CDLL(args.cfitsio_library)
lib.ffdkopn.argtypes = [c.POINTER(c.c_void_p), c.c_char_p, c.c_int, c.POINTER(c.c_int)]
lib.ffmahd.argtypes = [c.c_void_p, c.c_int, c.POINTER(c.c_int), c.POINTER(c.c_int)]
lib.ffpscl.argtypes = [c.c_void_p, c.c_double, c.c_double, c.POINTER(c.c_int)]
lib.ffgpxv.argtypes = [
    c.c_void_p,
    c.c_int,
    c.POINTER(c.c_long),
    c.c_longlong,
    c.c_void_p,
    c.c_void_p,
    c.POINTER(c.c_int),
    c.POINTER(c.c_int),
]
lib.ffclos.argtypes = [c.c_void_p, c.POINTER(c.c_int)]
rows = []
for path in sorted(Path("extra/test/data").iterdir()):
    if path.suffix.lower() not in (".fits", ".fit", ".fts"):
        continue
    with fits.open(path, do_not_scale_image_data=True, uint=False) as hs:
        for idx, h in enumerate(hs):
            if not isinstance(
                h, (fits.PrimaryHDU, fits.ImageHDU, fits.CompImageHDU)
            ) or h.header.get("NAXIS", 0) not in (2, 3):
                continue
            hdr = h.header
            w, ht = hdr["NAXIS1"], hdr["NAXIS2"]
            planes = hdr.get("NAXIS3", 1)
            b = hdr["BITPIX"]
            tiles = (
                int(h._bintable.header.get("ZTILE2", 1))
                if isinstance(h, fits.CompImageHDU)
                else ht
            )
            f = c.c_void_p()
            s = c.c_int()
            kind = c.c_int()
            lib.ffdkopn(c.byref(f), str(path.resolve()).encode(), 0, c.byref(s))
            lib.ffmahd(f, idx + 1, c.byref(kind), c.byref(s))
            lib.ffpscl(f, 1.0, 0.0, c.byref(s))
            dtype = {8: "u1", 16: "i2", 32: "i4", 64: "i8", -32: "f4", -64: "f8"}[b]
            datatype = {8: 11, 16: 21, 32: 31, 64: 81, -32: 42, -64: 82}[b]
            null = c.c_float(float("nan")) if b == -32 else c.c_double(float("nan"))
            anynull = c.c_int()
            for plane in range(planes):
                raw = np.empty(w * ht, dtype=dtype)
                first = (c.c_long * 3)(1, 1, plane + 1)
                lib.ffgpxv(
                    f,
                    datatype,
                    first,
                    len(raw),
                    c.byref(null) if b < 0 else None,
                    raw.ctypes.data,
                    c.byref(anynull),
                    c.byref(s),
                )
                assert not s.value, (path, idx, s.value)
                if b < 0:
                    raw[np.isnan(raw)] = np.nan
                digest = hashlib.sha256(
                    raw.astype(raw.dtype.newbyteorder("<")).tobytes()
                ).hexdigest()
                rows.append([path.name, idx + 1, b, w, ht, plane, tiles, digest])
                print(path.name, idx + 1, plane, len(raw), flush=True)
            lib.ffclos(f, c.byref(s))
            assert not s.value
Path("extra/test/data/fits/wcs-pixels.tsv").write_text(
    "\n".join("\t".join(map(str, row)) for row in rows) + "\n"
)
print("REFERENCE planes", len(rows), flush=True)
