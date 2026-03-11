#!/usr/bin/env python3

import argparse
import math
import random
import sys
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from astropy.io import fits
from astropy.wcs import WCS


SUN_RADIUS_METER = 695700.0 * 1e3
SUN_MEAN_EARTH_DISTANCE_METER = 149_597_870_700.0
SUN_MEAN_EARTH_DISTANCE = SUN_MEAN_EARTH_DISTANCE_METER / SUN_RADIUS_METER
ARCSEC_PER_RAD = 180.0 * 3600.0 / math.pi


@dataclass(frozen=True)
class JHVMeta:
    pixel_width: int
    pixel_height: int
    arcsec_per_pixel_x: float
    arcsec_per_pixel_y: float
    unit_per_arcsec: float
    unit_per_pixel_x: float
    unit_per_pixel_y: float
    plane_units_per_rad: float
    crpix1_gl: float
    crpix2_gl: float
    crval_internal_x: float
    crval_internal_y: float
    crota_rad: float
    observer_distance: float


def unit_scale_from_cunit(cunit: str | None) -> float:
    if cunit and cunit.lower() == "deg":
        return 3600.0
    return 1.0


def angular_header_value_to_deg(value: float, cunit: str | None) -> float:
    if cunit and cunit.lower() == "deg":
        return float(value)
    return float(value) / 3600.0


def find_image_hdu(hdul: fits.HDUList, hdu_index: int | None):
    if hdu_index is not None:
        hdu = hdul[hdu_index]
        if hdu.data is None or getattr(hdu.data, "ndim", 0) < 2:
            raise ValueError(f"HDU {hdu_index} is not an image HDU")
        return hdu

    for hdu in hdul:
        if hdu.data is not None and getattr(hdu.data, "ndim", 0) >= 2:
            return hdu
    raise ValueError("No image HDU found")


def build_jhv_meta(header) -> JHVMeta:
    pixel_width = int(header.get("ZNAXIS1", header.get("NAXIS1")))
    pixel_height = int(header.get("ZNAXIS2", header.get("NAXIS2")))

    arcsec_x = unit_scale_from_cunit(header.get("CUNIT1"))
    arcsec_y = unit_scale_from_cunit(header.get("CUNIT2"))
    arcsec_per_pixel_x = float(header["CDELT1"]) * arcsec_x
    arcsec_per_pixel_y = float(header["CDELT2"]) * arcsec_y

    dsun_obs = header.get("DSUN_OBS")
    observer_distance = dsun_obs / SUN_RADIUS_METER if dsun_obs is not None else SUN_MEAN_EARTH_DISTANCE

    radius_sun_in_arcsec = math.degrees(math.atan2(1.0, observer_distance)) * 3600.0
    unit_per_arcsec = 1.0 / radius_sun_in_arcsec
    plane_units_per_rad = unit_per_arcsec * ARCSEC_PER_RAD

    unit_per_pixel_x = abs(arcsec_per_pixel_x * unit_per_arcsec)
    unit_per_pixel_y = abs(arcsec_per_pixel_y * unit_per_arcsec)

    crpix1_gl = float(header.get("CRPIX1", (pixel_width + 1) / 2.0)) - 0.5
    crpix2_gl = float(header.get("CRPIX2", (pixel_height + 1) / 2.0)) - 0.5

    crval_internal_x = float(header.get("CRVAL1", 0.0)) * arcsec_x * unit_per_arcsec
    crval_internal_y = float(header.get("CRVAL2", 0.0)) * arcsec_y * unit_per_arcsec

    try:
        pc2_1 = float(header["PC2_1"])
        pc1_1 = float(header["PC1_1"])
        crota_rad = math.atan2(pc2_1 / (arcsec_per_pixel_x / arcsec_per_pixel_y), pc1_1)
    except Exception:
        crota_deg = (
            header.get("CROTA")
            or header.get("CROTA1")
            or header.get("CROTA2")
            or 0.0
        )
        crota_rad = math.radians(float(crota_deg))

    return JHVMeta(
        pixel_width=pixel_width,
        pixel_height=pixel_height,
        arcsec_per_pixel_x=arcsec_per_pixel_x,
        arcsec_per_pixel_y=arcsec_per_pixel_y,
        unit_per_arcsec=unit_per_arcsec,
        unit_per_pixel_x=unit_per_pixel_x,
        unit_per_pixel_y=unit_per_pixel_y,
        plane_units_per_rad=plane_units_per_rad,
        crpix1_gl=crpix1_gl,
        crpix2_gl=crpix2_gl,
        crval_internal_x=crval_internal_x,
        crval_internal_y=crval_internal_y,
        crota_rad=crota_rad,
        observer_distance=observer_distance,
    )


def ensure_tan_projection(header) -> None:
    ctype1 = str(header.get("CTYPE1", ""))
    ctype2 = str(header.get("CTYPE2", ""))
    if not (ctype1.endswith("TAN") and ctype2.endswith("TAN")):
        raise ValueError(f"Only TAN FITS files are supported right now, got {ctype1!r} / {ctype2!r}")


def world2helioprojective(point_xyz: tuple[float, float, float], observer_distance: float) -> tuple[float, float]:
    x, y, z = point_xyz
    zeta = observer_distance - z
    return (
        math.atan2(x, zeta),
        math.atan2(y, math.sqrt(x * x + zeta * zeta)),
    )


def tan_world_to_plane_internal(world_rad: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    lon, lat = world_rad
    lon0 = meta.crval_internal_x / meta.plane_units_per_rad
    lat0 = meta.crval_internal_y / meta.plane_units_per_rad

    sin_lat = math.sin(lat)
    cos_lat = math.cos(lat)
    sin_lat0 = math.sin(lat0)
    cos_lat0 = math.cos(lat0)
    delta_lon = lon - lon0
    sin_delta_lon = math.sin(delta_lon)
    cos_delta_lon = math.cos(delta_lon)

    cosc = sin_lat0 * sin_lat + cos_lat0 * cos_lat * cos_delta_lon
    if cosc <= 0.0:
        raise ValueError("Point is outside the visible TAN hemisphere")

    return (
        meta.plane_units_per_rad * (cos_lat * sin_delta_lon / cosc),
        meta.plane_units_per_rad * ((cos_lat0 * sin_lat - sin_lat0 * cos_lat * cos_delta_lon) / cosc),
    )


def rotate_inverse_z(vec_xy: tuple[float, float], angle_rad: float) -> tuple[float, float]:
    x, y = vec_xy
    c = math.cos(angle_rad)
    s = math.sin(angle_rad)
    return (
        c * x + s * y,
        -s * x + c * y,
    )


def jhv_world_to_pixel_center(point_xyz: tuple[float, float, float], meta: JHVMeta) -> tuple[float, float]:
    world_rad = world2helioprojective(point_xyz, meta.observer_distance)
    plane_internal = tan_world_to_plane_internal(world_rad, meta)
    rotated_internal = rotate_inverse_z(plane_internal, meta.crota_rad)
    return (
        rotated_internal[0] / meta.unit_per_pixel_x + meta.crpix1_gl,
        -rotated_internal[1] / meta.unit_per_pixel_y + meta.crpix2_gl,
    )


def build_projection_only_wcs(header) -> WCS:
    crval1_deg = angular_header_value_to_deg(header.get("CRVAL1", 0.0), header.get("CUNIT1"))
    crval2_deg = angular_header_value_to_deg(header.get("CRVAL2", 0.0), header.get("CUNIT2"))

    wcs = WCS(naxis=2)
    wcs.wcs.ctype = ["RA---TAN", "DEC--TAN"]
    wcs.wcs.crval = [crval1_deg, crval2_deg]
    wcs.wcs.crpix = [1.0, 1.0]
    wcs.wcs.cdelt = [1.0, 1.0]
    wcs.wcs.pc = [[1.0, 0.0], [0.0, 1.0]]
    return wcs


def build_jhv_equivalent_astropy_wcs(header, meta: JHVMeta) -> WCS:
    crval1_deg = angular_header_value_to_deg(header.get("CRVAL1", 0.0), header.get("CUNIT1"))
    crval2_deg = angular_header_value_to_deg(header.get("CRVAL2", 0.0), header.get("CUNIT2"))

    sx = abs(meta.arcsec_per_pixel_x) / 3600.0
    sy = abs(meta.arcsec_per_pixel_y) / 3600.0
    c = math.cos(meta.crota_rad)
    s = math.sin(meta.crota_rad)

    wcs = WCS(naxis=2)
    wcs.wcs.ctype = ["RA---TAN", "DEC--TAN"]
    wcs.wcs.crval = [crval1_deg, crval2_deg]
    wcs.wcs.crpix = [meta.crpix1_gl + 0.5, meta.crpix2_gl + 0.5]
    wcs.wcs.cd = np.array([
        [c * sx, s * sy],
        [s * sx, -c * sy],
    ])
    return wcs


def sample_points(sample_count: int, seed: int) -> list[tuple[float, float, float]]:
    rng = random.Random(seed)
    points: list[tuple[float, float, float]] = []

    half = sample_count // 2
    for _ in range(half):
        radius = math.sqrt(rng.random()) * 0.98
        angle = rng.random() * 2.0 * math.pi
        x = radius * math.cos(angle)
        y = radius * math.sin(angle)
        z = math.sqrt(max(0.0, 1.0 - x * x - y * y))
        points.append((x, y, z))

    for _ in range(sample_count - half):
        radius = 1.05 + rng.random() * 2.0
        angle = rng.random() * 2.0 * math.pi
        x = radius * math.cos(angle)
        y = radius * math.sin(angle)
        z = -0.5 + rng.random() * 2.0
        points.append((x, y, z))

    return points


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate the JHV TAN orthographic code path against astropy.wcs.")
    parser.add_argument("fits_file", type=Path)
    parser.add_argument("--hdu", type=int, default=None, help="Explicit FITS HDU index to use")
    parser.add_argument("--samples", type=int, default=1000, help="Number of random 3D samples")
    parser.add_argument("--seed", type=int, default=0, help="Random seed")
    parser.add_argument("--report-worst", type=int, default=5, help="How many worst samples to print")
    args = parser.parse_args()

    with fits.open(args.fits_file) as hdul:
        hdu = find_image_hdu(hdul, args.hdu)
        header = hdu.header

    ensure_tan_projection(header)
    meta = build_jhv_meta(header)
    projection_wcs = build_projection_only_wcs(header)
    pixel_wcs = build_jhv_equivalent_astropy_wcs(header, meta)

    proj_err_max = 0.0
    pixel_err_max = 0.0
    worst: list[tuple[float, tuple[float, float, float], tuple[float, float], tuple[float, float]]] = []

    for point_xyz in sample_points(args.samples, args.seed):
        world_rad = world2helioprojective(point_xyz, meta.observer_distance)
        world_deg = [math.degrees(world_rad[0]), math.degrees(world_rad[1])]

        jhv_plane_internal = tan_world_to_plane_internal(world_rad, meta)
        astro_plane_deg = projection_wcs.wcs_world2pix([world_deg], 0)[0]
        astro_plane_internal = (
            astro_plane_deg[0] * meta.plane_units_per_rad / (180.0 / math.pi),
            astro_plane_deg[1] * meta.plane_units_per_rad / (180.0 / math.pi),
        )
        proj_err = max(
            abs(jhv_plane_internal[0] - astro_plane_internal[0]),
            abs(jhv_plane_internal[1] - astro_plane_internal[1]),
        )
        proj_err_max = max(proj_err_max, proj_err)

        jhv_pixel_center = jhv_world_to_pixel_center(point_xyz, meta)
        astro_pixel_center = pixel_wcs.wcs_world2pix([world_deg], 1)[0]
        astro_pixel_center = (astro_pixel_center[0] - 0.5, astro_pixel_center[1] - 0.5)
        pixel_err = max(
            abs(jhv_pixel_center[0] - astro_pixel_center[0]),
            abs(jhv_pixel_center[1] - astro_pixel_center[1]),
        )
        pixel_err_max = max(pixel_err_max, pixel_err)

        worst.append((pixel_err, point_xyz, jhv_pixel_center, astro_pixel_center))

    worst.sort(key=lambda item: item[0], reverse=True)

    print(f"file={args.fits_file}")
    print(f"samples={args.samples} seed={args.seed}")
    print(f"projection_max_error_internal={proj_err_max:.6e}")
    print(f"pixel_center_max_error_px={pixel_err_max:.6e}")
    print("worst_samples:")
    for pixel_err, point_xyz, jhv_pixel_center, astro_pixel_center in worst[: args.report_worst]:
        print(
            f"  err={pixel_err:.6e} point={point_xyz!r} "
            f"jhv={jhv_pixel_center!r} astropy={astro_pixel_center!r}"
        )

    return 0


if __name__ == "__main__":
    sys.exit(main())
