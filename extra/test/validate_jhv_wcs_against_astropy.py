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
from PIL import Image


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
    projection: str
    pv2: tuple[float, float, float, float, float, float]


def unit_scale_from_cunit(cunit: str | None) -> float:
    if cunit and cunit.lower() == "deg":
        return 3600.0
    return 1.0


def angular_header_value_to_deg(value: float, cunit: str | None) -> float:
    if cunit and cunit.lower() == "deg":
        return float(value)
    return float(value) / 3600.0


def wrap_angle_diff_deg(a: float, b: float) -> float:
    return abs((a - b + 180.0) % 360.0 - 180.0)


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
        projection=str(header.get("CTYPE1", ""))[-3:],
        pv2=tuple(float(header.get(f"PV2_{i}", 0.0)) for i in range(6)),
    )


def ensure_supported_projection(header) -> None:
    ctype1 = str(header.get("CTYPE1", ""))
    ctype2 = str(header.get("CTYPE2", ""))
    if ctype1[-3:] != ctype2[-3:]:
        raise ValueError(f"Mismatched projection types: {ctype1!r} / {ctype2!r}")
    if not (ctype1.endswith("TAN") or ctype1.endswith("AZP") or ctype1.endswith("ZPN")):
        raise ValueError(f"Only TAN, AZP, and ZPN FITS files are supported right now, got {ctype1!r} / {ctype2!r}")


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


def azp_world_to_plane_internal(world_rad: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    lon, lat = world_rad
    lon0 = meta.crval_internal_x / meta.plane_units_per_rad
    lat0 = meta.crval_internal_y / meta.plane_units_per_rad
    mu = meta.pv2[1]
    gamma = math.radians(meta.pv2[2])

    sin_lat = math.sin(lat)
    cos_lat = math.cos(lat)
    sin_lat0 = math.sin(lat0)
    cos_lat0 = math.cos(lat0)
    delta_lon = lon - lon0
    sin_delta_lon = math.sin(delta_lon)
    cos_delta_lon = math.cos(delta_lon)

    a = cos_lat * sin_delta_lon
    b = cos_lat0 * sin_lat - sin_lat0 * cos_lat * cos_delta_lon
    c = math.hypot(a, b)
    if c == 0.0:
        return (0.0, 0.0)

    denom = mu + sin_lat0 * sin_lat + cos_lat0 * cos_lat * cos_delta_lon - b * math.tan(gamma)
    if denom == 0.0:
        raise ValueError("Point is on the AZP singularity")

    radial = (mu + 1.0) * c / denom
    return (
        meta.plane_units_per_rad * radial * a / c,
        meta.plane_units_per_rad * radial * b / (c * math.cos(gamma)),
    )


def zpn_radial(meta: JHVMeta, eta_rad: float) -> float:
    radial = 0.0
    power = 1.0
    for coefficient in meta.pv2:
        radial += coefficient * power
        power *= eta_rad
    return radial


def zpn_radial_derivative(meta: JHVMeta, eta_rad: float) -> float:
    derivative = 0.0
    power = 1.0
    for index, coefficient in enumerate(meta.pv2[1:], start=1):
        derivative += index * coefficient * power
        power *= eta_rad
    return derivative


def zpn_primary_branch_upper_eta(meta: JHVMeta) -> float:
    max_eta = math.pi
    prev_eta = 0.0
    prev_derivative = zpn_radial_derivative(meta, prev_eta)
    if prev_derivative <= 0.0:
        return 0.0

    for step in range(1, 513):
        eta = max_eta * step / 512.0
        derivative = zpn_radial_derivative(meta, eta)
        if derivative <= 0.0:
            lo = prev_eta
            hi = eta
            for _ in range(64):
                mid = 0.5 * (lo + hi)
                if zpn_radial_derivative(meta, mid) > 0.0:
                    lo = mid
                else:
                    hi = mid
            return 0.5 * (lo + hi)
        prev_eta = eta
        prev_derivative = derivative
    return max_eta


def zpn_world_to_plane_internal(world_rad: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
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

    a = cos_lat * sin_delta_lon
    b = cos_lat0 * sin_lat - sin_lat0 * cos_lat * cos_delta_lon
    c = math.hypot(a, b)
    if c == 0.0:
        return (0.0, 0.0)

    cos_native_distance = sin_lat0 * sin_lat + cos_lat0 * cos_lat * cos_delta_lon
    eta = math.acos(max(-1.0, min(1.0, cos_native_distance)))
    radial = zpn_radial(meta, eta)
    derivative = zpn_radial_derivative(meta, eta)
    if radial < 0.0 or derivative <= 0.0:
        raise ValueError("Point is outside the primary forward ZPN branch")

    return (
        meta.plane_units_per_rad * radial * a / c,
        meta.plane_units_per_rad * radial * b / c,
    )


def azp_plane_internal_to_world(plane_internal: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    gamma = math.radians(meta.pv2[2])
    if abs(gamma) > 1e-12:
        raise ValueError("Inverse AZP validator currently supports only the non-slanted gamma=0 case")

    x = plane_internal[0] / meta.plane_units_per_rad
    y = plane_internal[1] / meta.plane_units_per_rad
    r = math.hypot(x, y)

    lon0 = meta.crval_internal_x / meta.plane_units_per_rad
    lat0 = meta.crval_internal_y / meta.plane_units_per_rad
    mu = meta.pv2[1]

    if r == 0.0:
        return (lon0, lat0)

    mu_plus_1 = mu + 1.0
    if mu == 1.0:
        t = 0.5 * r
    else:
        discriminant = mu_plus_1 * mu_plus_1 - r * r * (mu * mu - 1.0)
        if discriminant < 0.0:
            raise ValueError("Plane point is outside the real AZP inverse branch")
        t = r * mu_plus_1 / (mu_plus_1 + math.sqrt(discriminant))
    eta = 2.0 * math.atan(t)

    alpha = math.atan2(x, y)
    sin_eta = math.sin(eta)
    cos_eta = math.cos(eta)
    a = sin_eta * math.sin(alpha)
    b = sin_eta * math.cos(alpha)

    sin_lat0 = math.sin(lat0)
    cos_lat0 = math.cos(lat0)
    lat = math.asin(cos_eta * sin_lat0 + b * cos_lat0)
    lon = lon0 + math.atan2(a, cos_eta * cos_lat0 - b * sin_lat0)
    return (lon, lat)


def zpn_plane_internal_to_world(plane_internal: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    x = plane_internal[0] / meta.plane_units_per_rad
    y = plane_internal[1] / meta.plane_units_per_rad
    radial_target = math.hypot(x, y)

    lon0 = meta.crval_internal_x / meta.plane_units_per_rad
    lat0 = meta.crval_internal_y / meta.plane_units_per_rad
    upper = zpn_primary_branch_upper_eta(meta)
    lo = 0.0
    hi = upper
    target = min(max(radial_target, zpn_radial(meta, lo)), zpn_radial(meta, hi))
    for _ in range(64):
        mid = 0.5 * (lo + hi)
        if zpn_radial(meta, mid) < target:
            lo = mid
        else:
            hi = mid
    eta = 0.5 * (lo + hi)
    if eta == 0.0:
        return (lon0, lat0)

    alpha = math.atan2(x, y)
    sin_eta = math.sin(eta)
    cos_eta = math.cos(eta)
    a = sin_eta * math.sin(alpha)
    b = sin_eta * math.cos(alpha)

    sin_lat0 = math.sin(lat0)
    cos_lat0 = math.cos(lat0)
    lat = math.asin(cos_eta * sin_lat0 + b * cos_lat0)
    lon = lon0 + math.atan2(a, cos_eta * cos_lat0 - b * sin_lat0)
    return (lon, lat)


def project_world_to_plane_internal(world_rad: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    if meta.projection == "TAN":
        return tan_world_to_plane_internal(world_rad, meta)
    if meta.projection == "AZP":
        return azp_world_to_plane_internal(world_rad, meta)
    if meta.projection == "ZPN":
        return zpn_world_to_plane_internal(world_rad, meta)
    raise ValueError(f"Unsupported projection {meta.projection!r}")


def project_plane_internal_to_world(plane_internal: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    if meta.projection == "TAN":
        return project_plane_internal_to_world_tan(plane_internal, meta)
    if meta.projection == "AZP":
        return azp_plane_internal_to_world(plane_internal, meta)
    if meta.projection == "ZPN":
        return zpn_plane_internal_to_world(plane_internal, meta)
    raise ValueError(f"Inverse projection is unsupported for {meta.projection!r}")


def helioprojective_to_observer_ray(world_rad: tuple[float, float]) -> np.ndarray:
    lon, lat = world_rad
    ray = np.array([
        math.tan(lon),
        math.tan(lat) / math.cos(lon),
        -1.0,
    ], dtype=np.float64)
    return ray / np.linalg.norm(ray)


def project_world_to_plane_internal_array(world_deg: np.ndarray, meta: JHVMeta) -> np.ndarray:
    lon = np.deg2rad(world_deg[:, 0])
    lat = np.deg2rad(world_deg[:, 1])
    lon0 = meta.crval_internal_x / meta.plane_units_per_rad
    lat0 = meta.crval_internal_y / meta.plane_units_per_rad

    sin_lat = np.sin(lat)
    cos_lat = np.cos(lat)
    sin_lat0 = math.sin(lat0)
    cos_lat0 = math.cos(lat0)
    delta_lon = lon - lon0
    sin_delta_lon = np.sin(delta_lon)
    cos_delta_lon = np.cos(delta_lon)

    if meta.projection == "TAN":
        cosc = sin_lat0 * sin_lat + cos_lat0 * cos_lat * cos_delta_lon
        x = meta.plane_units_per_rad * (cos_lat * sin_delta_lon / cosc)
        y = meta.plane_units_per_rad * ((cos_lat0 * sin_lat - sin_lat0 * cos_lat * cos_delta_lon) / cosc)
        return np.column_stack((x, y))

    if meta.projection == "AZP":
        mu = meta.pv2[1]
        gamma = math.radians(meta.pv2[2])
        a = cos_lat * sin_delta_lon
        b = cos_lat0 * sin_lat - sin_lat0 * cos_lat * cos_delta_lon
        c = np.hypot(a, b)
        denom = mu + sin_lat0 * sin_lat + cos_lat0 * cos_lat * cos_delta_lon - b * math.tan(gamma)
        radial = (mu + 1.0) * c / denom
        x = meta.plane_units_per_rad * radial * a / c
        y = meta.plane_units_per_rad * radial * b / (c * math.cos(gamma))
        x = np.where(c == 0.0, 0.0, x)
        y = np.where(c == 0.0, 0.0, y)
        return np.column_stack((x, y))

    if meta.projection == "ZPN":
        a = cos_lat * sin_delta_lon
        b = cos_lat0 * sin_lat - sin_lat0 * cos_lat * cos_delta_lon
        c = np.hypot(a, b)
        eta = np.arccos(np.clip(sin_lat0 * sin_lat + cos_lat0 * cos_lat * cos_delta_lon, -1.0, 1.0))
        radial = np.zeros_like(eta)
        power = np.ones_like(eta)
        for coefficient in meta.pv2:
            radial += coefficient * power
            power *= eta
        x = meta.plane_units_per_rad * radial * a / c
        y = meta.plane_units_per_rad * radial * b / c
        x = np.where(c == 0.0, 0.0, x)
        y = np.where(c == 0.0, 0.0, y)
        return np.column_stack((x, y))

    raise ValueError(f"Unsupported projection {meta.projection!r}")


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
    plane_internal = project_world_to_plane_internal(world_rad, meta)
    rotated_internal = rotate_inverse_z(plane_internal, meta.crota_rad)
    return (
        rotated_internal[0] / meta.unit_per_pixel_x + meta.crpix1_gl,
        -rotated_internal[1] / meta.unit_per_pixel_y + meta.crpix2_gl,
    )


def jhv_world_array_to_pixel_center(world_deg: np.ndarray, meta: JHVMeta) -> np.ndarray:
    plane_internal = project_world_to_plane_internal_array(world_deg, meta)
    c = math.cos(meta.crota_rad)
    s = math.sin(meta.crota_rad)
    rotated_x = c * plane_internal[:, 0] + s * plane_internal[:, 1]
    rotated_y = -s * plane_internal[:, 0] + c * plane_internal[:, 1]
    return np.column_stack((
        rotated_x / meta.unit_per_pixel_x + meta.crpix1_gl,
        -rotated_y / meta.unit_per_pixel_y + meta.crpix2_gl,
    ))


def old_tan_world_array_to_pixel_center(world_xyz: np.ndarray, meta: JHVMeta) -> np.ndarray:
    dx = world_xyz[:, 0] - meta.crval_internal_x
    dy = world_xyz[:, 1] - meta.crval_internal_y
    c = math.cos(meta.crota_rad)
    s = math.sin(meta.crota_rad)
    rotated_x = c * dx + s * dy
    rotated_y = -s * dx + c * dy
    return np.column_stack((
        rotated_x / meta.unit_per_pixel_x + meta.crpix1_gl,
        -rotated_y / meta.unit_per_pixel_y + meta.crpix2_gl,
    ))


def ortho_carrier_world_array_from_hpc_world_deg(world_deg: np.ndarray, meta: JHVMeta) -> np.ndarray:
    lon = np.deg2rad(world_deg[:, 0])
    lat = np.deg2rad(world_deg[:, 1])
    tx = np.tan(lon)
    ty = np.tan(lat) / np.cos(lon)

    a = tx * tx + ty * ty + 1.0
    disc = meta.observer_distance * meta.observer_distance - a * (meta.observer_distance * meta.observer_distance - 1.0)
    hits_sphere = disc >= 0.0

    s = np.empty_like(tx)
    s[hits_sphere] = (meta.observer_distance - np.sqrt(disc[hits_sphere])) / a[hits_sphere]
    s[~hits_sphere] = meta.observer_distance

    z = meta.observer_distance - s
    z[~hits_sphere] = 0.0
    return np.column_stack((s * tx, s * ty, z))


def build_projection_only_wcs(header) -> WCS:
    crval1_deg = angular_header_value_to_deg(header.get("CRVAL1", 0.0), header.get("CUNIT1"))
    crval2_deg = angular_header_value_to_deg(header.get("CRVAL2", 0.0), header.get("CUNIT2"))
    projection = str(header.get("CTYPE1", ""))[-3:]

    wcs = WCS(naxis=2)
    wcs.wcs.ctype = [f"RA---{projection}", f"DEC--{projection}"]
    wcs.wcs.crval = [crval1_deg, crval2_deg]
    wcs.wcs.crpix = [1.0, 1.0]
    wcs.wcs.cdelt = [1.0, 1.0]
    wcs.wcs.pc = [[1.0, 0.0], [0.0, 1.0]]
    if projection in {"AZP", "ZPN"}:
        wcs.wcs.set_pv([(2, i, float(header.get(f"PV2_{i}", 0.0))) for i in range(6) if f"PV2_{i}" in header])
    return wcs


def build_jhv_equivalent_astropy_wcs(header, meta: JHVMeta) -> WCS:
    crval1_deg = angular_header_value_to_deg(header.get("CRVAL1", 0.0), header.get("CUNIT1"))
    crval2_deg = angular_header_value_to_deg(header.get("CRVAL2", 0.0), header.get("CUNIT2"))
    projection = str(header.get("CTYPE1", ""))[-3:]

    sx = abs(meta.arcsec_per_pixel_x) / 3600.0
    sy = abs(meta.arcsec_per_pixel_y) / 3600.0
    c = math.cos(meta.crota_rad)
    s = math.sin(meta.crota_rad)

    wcs = WCS(naxis=2)
    wcs.wcs.ctype = [f"RA---{projection}", f"DEC--{projection}"]
    wcs.wcs.crval = [crval1_deg, crval2_deg]
    wcs.wcs.crpix = [meta.crpix1_gl + 0.5, meta.crpix2_gl + 0.5]
    wcs.wcs.cd = np.array([
        [c * sx, s * sy],
        [s * sx, -c * sy],
    ])
    if projection in {"AZP", "ZPN"}:
        wcs.wcs.set_pv([(2, i, float(header.get(f"PV2_{i}", 0.0))) for i in range(6) if f"PV2_{i}" in header])
    return wcs


def raw_hpc_footprint_bounds_degrees(meta: JHVMeta) -> tuple[float, float, float, float]:
    samples = [
        (-meta.crpix1_gl * meta.unit_per_pixel_x, -meta.crpix2_gl * meta.unit_per_pixel_y),
        ((meta.pixel_width - meta.crpix1_gl) * meta.unit_per_pixel_x, -meta.crpix2_gl * meta.unit_per_pixel_y),
        (-meta.crpix1_gl * meta.unit_per_pixel_x, (meta.pixel_height - meta.crpix2_gl) * meta.unit_per_pixel_y),
        ((meta.pixel_width - meta.crpix1_gl) * meta.unit_per_pixel_x, (meta.pixel_height - meta.crpix2_gl) * meta.unit_per_pixel_y),
        (0.0, -meta.crpix2_gl * meta.unit_per_pixel_y),
        (0.0, (meta.pixel_height - meta.crpix2_gl) * meta.unit_per_pixel_y),
        (-meta.crpix1_gl * meta.unit_per_pixel_x, 0.0),
        ((meta.pixel_width - meta.crpix1_gl) * meta.unit_per_pixel_x, 0.0),
    ]
    min_x = math.inf
    max_x = -math.inf
    min_y = math.inf
    max_y = -math.inf
    c = math.cos(meta.crota_rad)
    s = math.sin(meta.crota_rad)
    for x, y in samples:
        plane_x = c * x - s * y
        plane_y = s * x + c * y
        world_rad = project_plane_internal_to_world((plane_x, plane_y), meta) if meta.projection != "TAN" else project_plane_internal_to_world_tan((plane_x, plane_y), meta)
        lon_deg = math.degrees(world_rad[0])
        lat_deg = math.degrees(world_rad[1])
        min_x = min(min_x, lon_deg)
        max_x = max(max_x, lon_deg)
        min_y = min(min_y, lat_deg)
        max_y = max(max_y, lat_deg)
    return (min_x, max_x, min_y, max_y)


def hpc_bounds_degrees(meta: JHVMeta, aspect: float) -> tuple[float, float, float, float]:
    min_x, max_x, min_y, max_y = raw_hpc_footprint_bounds_degrees(meta)
    half_width = max(abs(min_x), abs(max_x))
    half_height = max(abs(min_y), abs(max_y), half_width / aspect)
    half_width = half_height * aspect
    return (-half_width, half_width, -half_height, half_height)


def project_plane_internal_to_world_tan(plane_internal: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    x = plane_internal[0] / meta.plane_units_per_rad
    y = plane_internal[1] / meta.plane_units_per_rad
    rho = math.hypot(x, y)
    lon0 = meta.crval_internal_x / meta.plane_units_per_rad
    lat0 = meta.crval_internal_y / meta.plane_units_per_rad
    if rho == 0.0:
        return (lon0, lat0)
    c = math.atan(rho)
    sinc = math.sin(c)
    cosc = math.cos(c)
    return (
        lon0 + math.atan2(x * sinc, rho * math.cos(lat0) * cosc - y * math.sin(lat0) * sinc),
        math.asin(cosc * math.sin(lat0) + y * sinc * math.cos(lat0) / rho),
    )


def hpc_screen_to_world_rad(scrpos: tuple[float, float], bounds_deg: tuple[float, float, float, float]) -> tuple[float, float]:
    sx, sy = scrpos
    x0, x1, y0, y1 = bounds_deg
    return (
        math.radians(x0 + sx * (x1 - x0)),
        math.radians(y0 + sy * (y1 - y0)),
    )


def jhv_hpc_world_to_pixel_center(world_rad: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    plane_internal = project_world_to_plane_internal(world_rad, meta)
    rotated_internal = rotate_inverse_z(plane_internal, meta.crota_rad)
    return (
        rotated_internal[0] / meta.unit_per_pixel_x + meta.crpix1_gl,
        -rotated_internal[1] / meta.unit_per_pixel_y + meta.crpix2_gl,
    )


def ortho_screen_to_world(screen_xy: tuple[float, float]) -> tuple[float, float, float]:
    x, y = screen_xy
    radius2 = x * x + y * y
    return (x, y, math.sqrt(max(0.0, 1.0 - radius2)))


def orthographic_vs_hpc_screen_pixel_centers(screen_xy: tuple[float, float], meta: JHVMeta) -> tuple[tuple[float, float], tuple[float, float]]:
    world_xyz = ortho_screen_to_world(screen_xy)

    ortho_px = jhv_world_to_pixel_center(world_xyz, meta)

    solar_limb_angle = math.atan2(1.0, meta.observer_distance)
    hpc_world_rad = (
        screen_xy[0] * solar_limb_angle,
        screen_xy[1] * solar_limb_angle,
    )
    hpc_px = jhv_hpc_world_to_pixel_center(hpc_world_rad, meta)
    return ortho_px, hpc_px


def normalize_image_for_png(img: np.ndarray) -> np.ndarray:
    finite = np.isfinite(img)
    if not np.any(finite):
        return np.zeros(img.shape, dtype=np.uint8)
    lo = float(np.nanpercentile(img[finite], 1.0))
    hi = float(np.nanpercentile(img[finite], 99.0))
    if hi <= lo:
        hi = lo + 1.0
    scaled = np.clip((img - lo) / (hi - lo), 0.0, 1.0)
    scaled[~finite] = 0.0
    return np.round(255.0 * scaled).astype(np.uint8)


def sample_nearest(image2d: np.ndarray, px: float, py: float) -> float:
    if not math.isfinite(px) or not math.isfinite(py):
        return math.nan
    ix = int(round(px))
    iy = int(round(py))
    if ix < 0 or iy < 0 or ix >= image2d.shape[1] or iy >= image2d.shape[0]:
        return math.nan
    return float(image2d[iy, ix])


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


def sample_worlds_from_pixels(pixel_wcs: WCS, meta: JHVMeta, sample_count: int) -> list[tuple[float, float]]:
    grid = max(2, int(math.ceil(math.sqrt(sample_count))))
    xs = np.linspace(1.0, float(meta.pixel_width), grid)
    ys = np.linspace(1.0, float(meta.pixel_height), grid)
    worlds: list[tuple[float, float]] = []
    for y in ys:
        points = np.column_stack((xs, np.full(xs.shape, y, dtype=np.float64)))
        world_deg = pixel_wcs.wcs_pix2world(points, 1)
        for lon_deg, lat_deg in world_deg:
            worlds.append((math.radians(float(lon_deg)), math.radians(float(lat_deg))))
    return worlds


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Validate the current JHV image/WCS projection code paths against astropy.wcs. "
            "The script covers the formal TAN/AZP/ZPN image path, the inverse TAN/AZP/ZPN branches "
            "implemented by JHV where the validator has coverage, the centered HPC display-bounds logic, and the existing orthographic/HPC/TAN "
            "comparison modes already used by this branch. It does not validate newer Java overlay-only behavior "
            "such as viewpoint-space external-point projection or visible-hemisphere clipping."
        )
    )
    parser.add_argument("fits_file", type=Path)
    parser.add_argument("--hdu", type=int, default=None, help="Explicit FITS HDU index to use")
    parser.add_argument("--samples", type=int, default=1000, help="Number of random 3D samples")
    parser.add_argument("--seed", type=int, default=0, help="Random seed")
    parser.add_argument("--report-worst", type=int, default=5, help="How many worst samples to print")
    parser.add_argument("--all-pixels", action="store_true", help="Validate all pixel centers instead of random 3D samples")
    parser.add_argument("--inverse-tan", action="store_true", help="Validate the TAN inverse plane->world mapping")
    parser.add_argument("--inverse-azp", action="store_true", help="Validate the non-slanted AZP inverse plane->world mapping")
    parser.add_argument("--inverse-zpn", action="store_true", help="Validate the primary-branch ZPN inverse plane->world mapping")
    parser.add_argument("--hpc-render-compare", action="store_true", help="Render a bounded HPC screen through JHV and Astropy mappings and write diagnostic PNGs")
    parser.add_argument("--hpc-bounds-compare", action="store_true", help="Report the raw and centered HPC bounds used by the current JHV display logic")
    parser.add_argument("--ortho-vs-hpc-screen-compare", action="store_true", help="Compare formal-TAN in Orthographic mode against JHV HPC over the full rendered comparison frame")
    parser.add_argument("--compare-initial-tan-image-frame", action="store_true", help="Compare simple-TAN against formal-TAN over the full image frame")
    parser.add_argument("--compare-initial-tan-vs-hpc", action="store_true", help="Compare simple-TAN against the JHV HPC display sampling over the full rendered comparison frame")
    parser.add_argument("--render-size", type=int, default=512, help="Square output size for HPC diagnostic renderings")
    parser.add_argument("--output-dir", type=Path, default=Path("extra/test/out"), help="Directory for diagnostic PNGs")
    args = parser.parse_args()

    with fits.open(args.fits_file) as hdul:
        hdu = find_image_hdu(hdul, args.hdu)
        header = hdu.header
        image_data = np.squeeze(np.asarray(hdu.data, dtype=np.float64))

    ensure_supported_projection(header)
    meta = build_jhv_meta(header)
    projection_wcs = build_projection_only_wcs(header)
    pixel_wcs = build_jhv_equivalent_astropy_wcs(header, meta)

    if args.hpc_bounds_compare:
        raw_bounds_deg = raw_hpc_footprint_bounds_degrees(meta)
        centered_bounds_deg = hpc_bounds_degrees(meta, 1.0)
        print(f"file={args.fits_file}")
        print("mode=hpc_bounds_compare")
        print(f"projection={meta.projection}")
        print(f"observer_distance={meta.observer_distance:.12f}")
        print(
            "raw_bounds_deg=("
            f"{raw_bounds_deg[0]:.12f}, {raw_bounds_deg[1]:.12f}, "
            f"{raw_bounds_deg[2]:.12f}, {raw_bounds_deg[3]:.12f})"
        )
        print(
            "centered_bounds_deg=("
            f"{centered_bounds_deg[0]:.12f}, {centered_bounds_deg[1]:.12f}, "
            f"{centered_bounds_deg[2]:.12f}, {centered_bounds_deg[3]:.12f})"
        )
        print(f"centered_half_width_deg={centered_bounds_deg[1]:.12f}")
        print(f"centered_half_height_deg={centered_bounds_deg[3]:.12f}")
        return 0

    if args.hpc_render_compare:
        if image_data.ndim != 2:
            raise ValueError(f"HPC render compare expects 2D image data, got shape {image_data.shape!r}")

        raw_bounds_deg = raw_hpc_footprint_bounds_degrees(meta)
        bounds_deg = hpc_bounds_degrees(meta, 1.0)
        size = args.render_size
        jhv_img = np.full((size, size), np.nan, dtype=np.float64)
        astro_img = np.full((size, size), np.nan, dtype=np.float64)
        diff_px = np.full((size, size), np.nan, dtype=np.float64)
        max_px_err = 0.0
        sum_px_err2 = 0.0
        count = 0

        for iy in range(size):
            sy = iy / (size - 1) if size > 1 else 0.5
            for ix in range(size):
                sx = ix / (size - 1) if size > 1 else 0.5
                world_rad = hpc_screen_to_world_rad((sx, sy), bounds_deg)
                world_deg = [math.degrees(world_rad[0]), math.degrees(world_rad[1])]

                try:
                    jhv_px = jhv_hpc_world_to_pixel_center(world_rad, meta)
                except ValueError:
                    diff_px[iy, ix] = math.nan
                    jhv_img[iy, ix] = math.nan
                    astro_img[iy, ix] = math.nan
                    continue
                astro_px_raw = pixel_wcs.wcs_world2pix([world_deg], 1)[0]
                astro_px = (float(astro_px_raw[0] - 0.5), float(astro_px_raw[1] - 0.5))

                if math.isfinite(astro_px[0]) and math.isfinite(astro_px[1]) and math.isfinite(jhv_px[0]) and math.isfinite(jhv_px[1]):
                    err = max(abs(jhv_px[0] - astro_px[0]), abs(jhv_px[1] - astro_px[1]))
                    diff_px[iy, ix] = err
                    max_px_err = max(max_px_err, err)
                    sum_px_err2 += err * err
                    count += 1
                else:
                    diff_px[iy, ix] = math.nan

                jhv_img[iy, ix] = sample_nearest(image_data, jhv_px[0], jhv_px[1])
                astro_img[iy, ix] = sample_nearest(image_data, astro_px[0], astro_px[1])

        intensity_diff = np.abs(jhv_img - astro_img)
        args.output_dir.mkdir(parents=True, exist_ok=True)
        stem = args.fits_file.stem
        jhv_path = args.output_dir / f"{stem}_hpc_jhv.png"
        astro_path = args.output_dir / f"{stem}_hpc_astropy.png"
        diff_path = args.output_dir / f"{stem}_hpc_diff.png"

        Image.fromarray(normalize_image_for_png(jhv_img), mode="L").save(jhv_path)
        Image.fromarray(normalize_image_for_png(astro_img), mode="L").save(astro_path)
        Image.fromarray(normalize_image_for_png(intensity_diff), mode="L").save(diff_path)

        print(f"file={args.fits_file}")
        print(f"mode=hpc_render_compare size={size}")
        print(f"raw_bounds_deg=({raw_bounds_deg[0]:.12f}, {raw_bounds_deg[1]:.12f}, {raw_bounds_deg[2]:.12f}, {raw_bounds_deg[3]:.12f})")
        print(f"bounds_deg=({bounds_deg[0]:.12f}, {bounds_deg[1]:.12f}, {bounds_deg[2]:.12f}, {bounds_deg[3]:.12f})")
        print(f"pixel_center_max_error_px={max_px_err:.6e}")
        print(f"pixel_center_rms_error_px={math.sqrt(sum_px_err2 / count):.6e}" if count > 0 else "pixel_center_rms_error_px=nan")
        print(f"jhv_png={jhv_path}")
        print(f"astropy_png={astro_path}")
        print(f"diff_png={diff_path}")
        return 0

    if args.ortho_vs_hpc_screen_compare:
        if image_data.ndim != 2:
            raise ValueError(f"Ortho/HPC screen compare expects 2D image data, got shape {image_data.shape!r}")

        size = args.render_size
        ortho_img = np.full((size, size), np.nan, dtype=np.float64)
        hpc_img = np.full((size, size), np.nan, dtype=np.float64)
        diff_px = np.full((size, size), np.nan, dtype=np.float64)
        max_px_err = 0.0
        sum_px_err2 = 0.0
        count = 0

        for iy in range(size):
            sy = -1.0 + 2.0 * (iy / (size - 1) if size > 1 else 0.5)
            for ix in range(size):
                sx = -1.0 + 2.0 * (ix / (size - 1) if size > 1 else 0.5)
                ortho_px, hpc_px = orthographic_vs_hpc_screen_pixel_centers((sx, sy), meta)
                err = max(abs(ortho_px[0] - hpc_px[0]), abs(ortho_px[1] - hpc_px[1]))
                diff_px[iy, ix] = err
                max_px_err = max(max_px_err, err)
                sum_px_err2 += err * err
                count += 1

                ortho_img[iy, ix] = sample_nearest(image_data, ortho_px[0], ortho_px[1])
                hpc_img[iy, ix] = sample_nearest(image_data, hpc_px[0], hpc_px[1])

        intensity_diff = np.abs(ortho_img - hpc_img)
        args.output_dir.mkdir(parents=True, exist_ok=True)
        stem = args.fits_file.stem
        ortho_path = args.output_dir / f"{stem}_ortho_screen.png"
        hpc_path = args.output_dir / f"{stem}_hpc_screen.png"
        diff_path = args.output_dir / f"{stem}_ortho_vs_hpc_diff.png"

        Image.fromarray(normalize_image_for_png(ortho_img), mode="L").save(ortho_path)
        Image.fromarray(normalize_image_for_png(hpc_img), mode="L").save(hpc_path)
        Image.fromarray(normalize_image_for_png(intensity_diff), mode="L").save(diff_path)

        print(f"file={args.fits_file}")
        print(f"mode=ortho_vs_hpc_screen_compare size={size}")
        print(f"observer_distance={meta.observer_distance:.12f}")
        print(f"solar_limb_angle_deg={math.degrees(math.atan2(1.0, meta.observer_distance)):.12f}")
        print(f"pixel_center_max_error_px={max_px_err:.6e}")
        print(f"pixel_center_rms_error_px={math.sqrt(sum_px_err2 / count):.6e}" if count > 0 else "pixel_center_rms_error_px=nan")
        print(f"ortho_png={ortho_path}")
        print(f"hpc_png={hpc_path}")
        print(f"diff_png={diff_path}")
        return 0

    if args.compare_initial_tan_image_frame:
        if image_data.ndim != 2:
            raise ValueError("TAN implementation comparison expects 2D image data, got shape {!r}".format(image_data.shape))
        if meta.projection != "TAN":
            raise ValueError("TAN implementation comparison requires a TAN FITS file")

        size = max(meta.pixel_width, meta.pixel_height)
        max_old = 0.0
        max_new = 0.0
        max_old_new = 0.0
        sum_old2 = 0.0
        sum_new2 = 0.0
        sum_old_new2 = 0.0
        count = 0
        old_img = np.full((meta.pixel_height, meta.pixel_width), np.nan, dtype=np.float64)
        new_img = np.full((meta.pixel_height, meta.pixel_width), np.nan, dtype=np.float64)
        diff_img = np.full((meta.pixel_height, meta.pixel_width), np.nan, dtype=np.float64)

        for iy in range(meta.pixel_height):
            fits_y = np.full(meta.pixel_width, iy + 1.0, dtype=np.float64)
            fits_x = np.arange(meta.pixel_width, dtype=np.float64) + 1.0
            world_deg = pixel_wcs.wcs_pix2world(np.column_stack((fits_x, fits_y)), 1)
            astro_px = np.column_stack((fits_x - 0.5, fits_y - 0.5))
            world_xyz = ortho_carrier_world_array_from_hpc_world_deg(world_deg, meta)
            new_px = jhv_world_array_to_pixel_center(world_deg, meta)
            old_px = old_tan_world_array_to_pixel_center(world_xyz, meta)

            old_err = np.maximum(np.abs(old_px[:, 0] - astro_px[:, 0]), np.abs(old_px[:, 1] - astro_px[:, 1]))
            new_err = np.maximum(np.abs(new_px[:, 0] - astro_px[:, 0]), np.abs(new_px[:, 1] - astro_px[:, 1]))
            old_new_err = np.maximum(np.abs(old_px[:, 0] - new_px[:, 0]), np.abs(old_px[:, 1] - new_px[:, 1]))

            max_old = max(max_old, float(np.max(old_err)))
            max_new = max(max_new, float(np.max(new_err)))
            max_old_new = max(max_old_new, float(np.max(old_new_err)))
            sum_old2 += float(np.sum(old_err * old_err))
            sum_new2 += float(np.sum(new_err * new_err))
            sum_old_new2 += float(np.sum(old_new_err * old_new_err))
            count += int(old_err.size)

            old_samples = np.array([sample_nearest(image_data, px, py) for px, py in old_px], dtype=np.float64)
            new_samples = np.array([sample_nearest(image_data, px, py) for px, py in new_px], dtype=np.float64)
            diff_samples = np.abs(old_samples - new_samples)
            old_img[iy, :] = old_samples
            new_img[iy, :] = new_samples
            diff_img[iy, :] = diff_samples

        args.output_dir.mkdir(parents=True, exist_ok=True)
        stem = args.fits_file.stem
        suffix = "_image_frame"
        old_path = args.output_dir / f"{stem}_initial_tan{suffix}.png"
        new_path = args.output_dir / f"{stem}_formal_tan{suffix}.png"
        diff_path = args.output_dir / f"{stem}_initial_vs_formal_tan{suffix}_diff.png"
        Image.fromarray(normalize_image_for_png(old_img), mode="L").save(old_path)
        Image.fromarray(normalize_image_for_png(new_img), mode="L").save(new_path)
        Image.fromarray(normalize_image_for_png(diff_img), mode="L").save(diff_path)

        print(f"file={args.fits_file}")
        print(f"mode=compare_initial_tan_image_frame size={size}")
        print("domain=image_frame")
        print(f"observer_distance={meta.observer_distance:.12f}")
        print(f"samples={count}")
        print(f"old_max_px_vs_astropy={max_old:.6e}")
        print(f"old_rms_px_vs_astropy={math.sqrt(sum_old2 / count):.6e}")
        print(f"new_max_px_vs_astropy={max_new:.6e}")
        print(f"new_rms_px_vs_astropy={math.sqrt(sum_new2 / count):.6e}")
        print(f"old_new_max_px={max_old_new:.6e}")
        print(f"old_new_rms_px={math.sqrt(sum_old_new2 / count):.6e}")
        print(f"initial_tan_png={old_path}")
        print(f"formal_tan_png={new_path}")
        print(f"initial_vs_formal_diff_png={diff_path}")
        return 0

    if args.compare_initial_tan_vs_hpc:
        if image_data.ndim != 2:
            raise ValueError(f"--compare-initial-tan-vs-hpc expects 2D image data, got shape {image_data.shape!r}")
        if meta.projection != "TAN":
            raise ValueError("--compare-initial-tan-vs-hpc requires a TAN FITS file")

        size = max(meta.pixel_width, meta.pixel_height)
        old_img = np.full((size, size), np.nan, dtype=np.float64)
        hpc_img = np.full((size, size), np.nan, dtype=np.float64)
        diff_img = np.full((size, size), np.nan, dtype=np.float64)
        max_px_err = 0.0
        sum_px_err2 = 0.0
        count = 0

        xs = np.linspace(-1.0, 1.0, size, dtype=np.float64)
        ys = np.linspace(-1.0, 1.0, size, dtype=np.float64)
        solar_limb_angle = math.atan2(1.0, meta.observer_distance)

        for iy, y in enumerate(ys):
            radius2 = xs * xs + y * y
            valid_indices = np.arange(size)
            z_valid = np.sqrt(np.maximum(0.0, 1.0 - radius2))
            world_xyz = np.column_stack((xs, np.full(xs.shape, y, dtype=np.float64), z_valid))

            old_px = old_tan_world_array_to_pixel_center(world_xyz, meta)
            hpc_world_deg = np.rad2deg(np.column_stack((
                xs * solar_limb_angle,
                np.full(xs.shape, y * solar_limb_angle, dtype=np.float64),
            )))
            hpc_px = jhv_world_array_to_pixel_center(hpc_world_deg, meta)

            px_err = np.maximum(np.abs(old_px[:, 0] - hpc_px[:, 0]), np.abs(old_px[:, 1] - hpc_px[:, 1]))
            max_px_err = max(max_px_err, float(np.max(px_err)))
            sum_px_err2 += float(np.sum(px_err * px_err))
            count += int(px_err.size)

            old_samples = np.array([sample_nearest(image_data, px, py) for px, py in old_px], dtype=np.float64)
            hpc_samples = np.array([sample_nearest(image_data, px, py) for px, py in hpc_px], dtype=np.float64)
            diff_samples = np.abs(old_samples - hpc_samples)
            old_img[iy, valid_indices] = old_samples
            hpc_img[iy, valid_indices] = hpc_samples
            diff_img[iy, valid_indices] = diff_samples

        args.output_dir.mkdir(parents=True, exist_ok=True)
        stem = args.fits_file.stem
        old_path = args.output_dir / f"{stem}_initial_tan_screen.png"
        hpc_path = args.output_dir / f"{stem}_hpc_screen_from_initial_tan_compare.png"
        diff_path = args.output_dir / f"{stem}_initial_tan_vs_hpc_diff.png"
        Image.fromarray(normalize_image_for_png(old_img), mode="L").save(old_path)
        Image.fromarray(normalize_image_for_png(hpc_img), mode="L").save(hpc_path)
        Image.fromarray(normalize_image_for_png(diff_img), mode="L").save(diff_path)

        print(f"file={args.fits_file}")
        print(f"mode=compare_initial_tan_vs_hpc size={size}")
        print(f"observer_distance={meta.observer_distance:.12f}")
        print(f"samples={count}")
        print(f"initial_tan_vs_hpc_max_px={max_px_err:.6e}")
        print(f"initial_tan_vs_hpc_rms_px={math.sqrt(sum_px_err2 / count):.6e}")
        print(f"initial_tan_screen_png={old_path}")
        print(f"hpc_screen_png={hpc_path}")
        print(f"initial_tan_vs_hpc_diff_png={diff_path}")
        return 0

    if args.inverse_tan or args.inverse_azp or args.inverse_zpn:
        inverse_mode_count = sum(1 for enabled in (args.inverse_tan, args.inverse_azp, args.inverse_zpn) if enabled)
        if inverse_mode_count > 1:
            raise ValueError("Choose at most one of --inverse-tan, --inverse-azp, or --inverse-zpn")

        if args.inverse_tan:
            expected_projection = "TAN"
            mode_name = "inverse_tan"
        elif args.inverse_azp:
            expected_projection = "AZP"
            mode_name = "inverse_azp"
        else:
            expected_projection = "ZPN"
            mode_name = "inverse_zpn"

        if meta.projection != expected_projection:
            raise ValueError(f"--{mode_name.replace('_', '-')} requires a {expected_projection} FITS file")

        inverse_err_max_deg = 0.0
        roundtrip_err_max_internal = 0.0
        valid_inverse_samples = 0
        skipped_inverse_samples = 0
        worst_inverse: list[tuple[float, tuple[float, float], tuple[float, float], tuple[float, float]]] = []
        if expected_projection == "ZPN":
            inverse_samples = sample_worlds_from_pixels(pixel_wcs, meta, args.samples)
        else:
            inverse_samples = [world2helioprojective(point_xyz, meta.observer_distance) for point_xyz in sample_points(args.samples, args.seed)]

        for world_rad in inverse_samples:
            try:
                plane_internal = project_world_to_plane_internal(world_rad, meta)
            except ValueError:
                skipped_inverse_samples += 1
                continue

            inverse_world_rad = project_plane_internal_to_world(plane_internal, meta)
            inverse_world_deg = (math.degrees(inverse_world_rad[0]), math.degrees(inverse_world_rad[1]))

            plane_deg = (
                math.degrees(plane_internal[0] / meta.plane_units_per_rad),
                math.degrees(plane_internal[1] / meta.plane_units_per_rad),
            )
            astro_world_deg = projection_wcs.wcs_pix2world([plane_deg], 0)[0]
            inverse_err_deg = max(
                wrap_angle_diff_deg(inverse_world_deg[0], float(astro_world_deg[0])),
                abs(inverse_world_deg[1] - astro_world_deg[1]),
            )
            inverse_err_max_deg = max(inverse_err_max_deg, inverse_err_deg)

            roundtrip_plane_internal = project_world_to_plane_internal(inverse_world_rad, meta)
            roundtrip_err_internal = max(
                abs(roundtrip_plane_internal[0] - plane_internal[0]),
                abs(roundtrip_plane_internal[1] - plane_internal[1]),
            )
            roundtrip_err_max_internal = max(roundtrip_err_max_internal, roundtrip_err_internal)

            worst_inverse.append((
                inverse_err_deg,
                plane_internal,
                inverse_world_deg,
                (float(astro_world_deg[0]), float(astro_world_deg[1])),
            ))
            valid_inverse_samples += 1

        worst_inverse.sort(key=lambda item: item[0], reverse=True)
        print(f"file={args.fits_file}")
        print(f"mode={mode_name} samples={args.samples} seed={args.seed}")
        print(f"valid_samples={valid_inverse_samples}")
        print(f"skipped_samples={skipped_inverse_samples}")
        print(f"inverse_world_max_error_deg={inverse_err_max_deg:.6e}")
        print(f"roundtrip_plane_max_error_internal={roundtrip_err_max_internal:.6e}")
        print("worst_inverse_samples:")
        for inverse_err_deg, plane_internal, inverse_world_deg, astro_world_deg in worst_inverse[: args.report_worst]:
            print(
                f"  err={inverse_err_deg:.6e} plane_internal={plane_internal!r} "
                f"inverse={inverse_world_deg!r} astropy={astro_world_deg!r}"
            )
        return 0

    proj_err_max = 0.0
    pixel_err_max = 0.0
    worst: list[tuple[float, str, tuple[float, float], tuple[float, float]]] = []

    if args.all_pixels:
        pixel_err_max = 0.0
        worst_pixels: list[tuple[float, tuple[int, int], tuple[float, float], tuple[float, float]]] = []
        for y in range(meta.pixel_height):
            fits_y = np.full(meta.pixel_width, y + 1.0, dtype=np.float64)
            fits_x = np.arange(meta.pixel_width, dtype=np.float64) + 1.0
            world = pixel_wcs.wcs_pix2world(np.column_stack((fits_x, fits_y)), 1)
            jhv_pixel_center = jhv_world_array_to_pixel_center(world, meta)
            astro_pixel_center = np.column_stack((fits_x - 0.5, fits_y - 0.5))
            errors = np.max(np.abs(jhv_pixel_center - astro_pixel_center), axis=1)
            row_max = float(np.max(errors))
            pixel_err_max = max(pixel_err_max, row_max)
            if args.report_worst > 0:
                idx = int(np.argmax(errors))
                worst_pixels.append((
                    float(errors[idx]),
                    (int(idx), y),
                    (float(jhv_pixel_center[idx, 0]), float(jhv_pixel_center[idx, 1])),
                    (float(astro_pixel_center[idx, 0]), float(astro_pixel_center[idx, 1])),
                ))

        worst_pixels.sort(key=lambda item: item[0], reverse=True)
        print(f"file={args.fits_file}")
        print("mode=all_pixels")
        print(f"pixel_center_max_error_px={pixel_err_max:.6e}")
        print("worst_pixels:")
        for pixel_err, pixel_xy, jhv_pixel_center, astro_pixel_center in worst_pixels[: args.report_worst]:
            print(
                f"  err={pixel_err:.6e} pixel={pixel_xy!r} "
                f"jhv={jhv_pixel_center!r} astropy={astro_pixel_center!r}"
            )
        return 0

    valid_samples = 0
    skipped_samples = 0
    if meta.projection == "ZPN":
        world_samples = sample_worlds_from_pixels(pixel_wcs, meta, args.samples)
    else:
        world_samples = [world2helioprojective(point_xyz, meta.observer_distance) for point_xyz in sample_points(args.samples, args.seed)]

    for world_rad in world_samples:
        world_deg = [math.degrees(world_rad[0]), math.degrees(world_rad[1])]

        try:
            jhv_plane_internal = project_world_to_plane_internal(world_rad, meta)
        except ValueError:
            skipped_samples += 1
            continue

        jhv_pixel_center_array = jhv_world_array_to_pixel_center(np.array([world_deg], dtype=np.float64), meta)
        jhv_pixel_center = (float(jhv_pixel_center_array[0, 0]), float(jhv_pixel_center_array[0, 1]))

        astro_plane_deg = projection_wcs.wcs_world2pix([world_deg], 0)[0]
        if not np.all(np.isfinite(astro_plane_deg)):
            skipped_samples += 1
            continue
        astro_plane_internal = (
            astro_plane_deg[0] * meta.plane_units_per_rad / (180.0 / math.pi),
            astro_plane_deg[1] * meta.plane_units_per_rad / (180.0 / math.pi),
        )
        proj_err = max(
            abs(jhv_plane_internal[0] - astro_plane_internal[0]),
            abs(jhv_plane_internal[1] - astro_plane_internal[1]),
        )
        proj_err_max = max(proj_err_max, proj_err)

        astro_pixel_center = pixel_wcs.wcs_world2pix([world_deg], 1)[0]
        if not np.all(np.isfinite(astro_pixel_center)):
            skipped_samples += 1
            continue
        astro_pixel_center = (astro_pixel_center[0] - 0.5, astro_pixel_center[1] - 0.5)
        pixel_err = max(
            abs(jhv_pixel_center[0] - astro_pixel_center[0]),
            abs(jhv_pixel_center[1] - astro_pixel_center[1]),
        )
        pixel_err_max = max(pixel_err_max, pixel_err)

        worst.append((
            pixel_err,
            f"world_deg=({world_deg[0]:.12f}, {world_deg[1]:.12f})",
            jhv_pixel_center,
            astro_pixel_center,
        ))
        valid_samples += 1

    worst.sort(key=lambda item: item[0], reverse=True)

    print(f"file={args.fits_file}")
    print(f"samples={args.samples} seed={args.seed}")
    print(f"valid_samples={valid_samples}")
    print(f"skipped_samples={skipped_samples}")
    print(f"projection_max_error_internal={proj_err_max:.6e}")
    print(f"pixel_center_max_error_px={pixel_err_max:.6e}")
    print("worst_samples:")
    for pixel_err, sample_desc, jhv_pixel_center, astro_pixel_center in worst[: args.report_worst]:
        print(
            f"  err={pixel_err:.6e} {sample_desc} "
            f"jhv={jhv_pixel_center!r} astropy={astro_pixel_center!r}"
        )

    return 0


if __name__ == "__main__":
    sys.exit(main())
