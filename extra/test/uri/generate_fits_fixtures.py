"""Regenerate analytic FITS fixtures and released CFITSIO reference pixels."""

import argparse
import ctypes as c
import hashlib
import json
from pathlib import Path

import astropy
import numpy as np
from astropy.io import fits

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument(
    "--cfitsio-library",
    required=True,
    help="CFITSIO 4.7.0 built with -ffp-contract=off",
)
args = parser.parse_args()
out = Path("extra/test/data/fits")
out.mkdir(exist_ok=True)
manifest = []


def add(name, data, compression=None, tile=None, **kw):
    path = out / (name + ".fits")
    h = fits.Header()
    h["DATE-AVG"] = "2026-08-21T09:00:00"
    h["INSTRUME"] = "TEST"
    h["TELESCOP"] = "JHV"
    h["CTYPE1"] = "HPLN-TAN"
    h["CTYPE2"] = "HPLT-TAN"
    h["CUNIT1"] = "arcsec"
    h["CUNIT2"] = "arcsec"
    h["CRPIX1"] = 1
    h["CRPIX2"] = 1
    h["CDELT1"] = 2
    h["CDELT2"] = 2
    h["CRVAL1"] = 0
    h["CRVAL2"] = 0
    if compression:
        image = fits.CompImageHDU(
            data, header=h, compression_type=compression, tile_shape=tile, **kw
        )
        fits.HDUList([fits.PrimaryHDU(), image]).writeto(path, overwrite=True)
        hdu = 2
    else:
        fits.PrimaryHDU(data, header=h).writeto(path, overwrite=True)
        hdu = 1
    with fits.open(path, do_not_scale_image_data=True) as hdus:
        ref = hdus[hdu - 1].data.copy()
        hdr = hdus[hdu - 1].header
        bitpix = hdr["BITPIX"]
        # Canonicalize NaNs for reference comparison across implementations.
        if ref.dtype.kind == "f":
            ref[np.isnan(ref)] = np.nan
        raw = ref.astype(ref.dtype.newbyteorder("<")).tobytes()
        (out / (name + ".raw")).write_bytes(raw)
        manifest.append(
            {
                "file": path.name,
                "hdu": hdu,
                "bitpix": bitpix,
                "width": data.shape[-1],
                "height": data.shape[-2],
                "planes": data.shape[0] if data.ndim == 3 else 1,
                "sha256": hashlib.sha256(raw).hexdigest(),
            }
        )


a = ((np.arange(3 * 35 * 70).reshape(3, 35, 70) * 47) % 60000 - 30000).astype("i2")
for name, tile in [
    ("rice-row", (1, 1, 70)),
    ("rice-block", (1, 8, 16)),
    ("rice-whole", (1, 35, 70)),
]:
    add(name, a, "RICE_1", tile)
add(
    "integer32",
    (np.arange(35 * 70).reshape(35, 70) * 5001 - 4000000).astype("i4"),
    "RICE_1",
    (8, 16),
)
add("integer64", (np.arange(35 * 70).reshape(35, 70) + 9007199254740992).astype("i8"))
add("byte", (np.arange(35 * 70).reshape(35, 70) % 256).astype("u1"))
f = (np.sin(np.arange(3 * 35 * 70).reshape(3, 35, 70) * 0.12) * 0.003 + 2).astype("f4")
f[:, 5:9, 12:17] = 0
f[0, 1, 1] = np.nan
add(
    "dither2-float",
    f,
    "RICE_1",
    (1, 8, 16),
    quantize_method=2,
    quantize_level=16,
    dither_seed=1,
)
add(
    "dither2-double",
    f.astype("f8") * 0.97,
    "RICE_1",
    (1, 8, 16),
    quantize_method=2,
    quantize_level=16,
    dither_seed=1,
)
# Lossless gzip tiles exercise a different CFITSIO image decompressor.
add("gzip-float", f, "GZIP_2", (1, 8, 16), quantize_level=0)
mixed = f.copy()
mixed[:, 0:8, 0:16] = np.nan
mixed[:, 0:8, 16:32] = 0
add(
    "mixed-float",
    mixed,
    "RICE_1",
    (1, 8, 16),
    quantize_method=2,
    quantize_level=16,
    dither_seed=1,
)
add(
    "mixed-double",
    mixed.astype("f8") * 0.97,
    "RICE_1",
    (1, 8, 16),
    quantize_method=2,
    quantize_level=16,
    dither_seed=1,
)
add("hcompress", a[0], "HCOMPRESS_1", (20, 70), hcomp_scale=0)
add("plio", (np.arange(35 * 70).reshape(35, 70) % 3).astype("i4"), "PLIO_1", (1, 70))
# Several image HDUs with distinct dimensions, metadata and clipping.
with fits.open(out / "byte.fits") as hdus:
    primary = hdus[0].copy()
    second = fits.ImageHDU(
        np.arange(2 * 9 * 11, dtype="f4").reshape(2, 9, 11),
        header=primary.header.copy(),
        name="UNCERTAINTY",
    )
    fits.HDUList([primary, second]).writeto(out / "multi.fits", overwrite=True)
(out / "manifest.json").write_text(
    json.dumps({"astropy": astropy.__version__, "images": manifest}, indent=2) + "\n"
)
print(len(manifest), "fixtures")

root = Path("extra/test/data/fits")
manifest = json.loads((root / "manifest.json").read_text())
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
for item in manifest["images"]:
    path = root / item["file"]
    f = c.c_void_p()
    s = c.c_int()
    kind = c.c_int()
    lib.ffdkopn(c.byref(f), str(path.resolve()).encode(), 0, c.byref(s))
    lib.ffmahd(f, item["hdu"], c.byref(kind), c.byref(s))
    lib.ffpscl(f, 1.0, 0.0, c.byref(s))
    b = item["bitpix"]
    dtype = {8: "u1", 16: "i2", 32: "i4", 64: "i8", -32: "f4", -64: "f8"}[b]
    raw = np.empty(item["width"] * item["height"] * item["planes"], dtype=dtype)
    first = (c.c_long * 3)(1, 1, 1)
    anynull = c.c_int()
    null = c.c_float(float("nan")) if b == -32 else c.c_double(float("nan"))
    datatype = {8: 11, 16: 21, 32: 31, 64: 81, -32: 42, -64: 82}[b]
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
    lib.ffclos(f, c.byref(s))
    assert not s.value, (path, s.value)
    if b < 0:
        raw[np.isnan(raw)] = np.nan
    actual = raw.astype(raw.dtype.newbyteorder("<")).tobytes()
    (root / path.name.replace(".fits", ".raw")).write_bytes(actual)
    item["sha256"] = hashlib.sha256(actual).hexdigest()
    with fits.open(path, do_not_scale_image_data=True) as hdus:
        astro = hdus[item["hdu"] - 1].data.copy().reshape(-1)
    if b < 0:
        astro[np.isnan(astro)] = np.nan
    other = astro.astype(astro.dtype.newbyteorder("<")).tobytes()
    different = actual != other
    if different:
        assert b < 0
        (root / path.name.replace(".fits", ".astropy.raw")).write_bytes(other)
        item["astropy_reference"] = path.name.replace(".fits", ".astropy.raw")
        item["astropy_sha256"] = hashlib.sha256(other).hexdigest()
        print(path.name, "rounding_difference", np.nanmax(np.abs(raw - astro)))
    else:
        print(path.name, "exact both references")
manifest["cfitsio"] = {
    "version": "4.7.0",
    "revision": "d6d27653740864a0483e74cff23c53e34e1dceee",
    "arithmetic": "-ffp-contract=off",
    "api": "fits_open_diskfile / fits_read_pix, no JHV adapter",
}
(root / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
