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


def observer_position(observer_distance: float) -> np.ndarray:
    return np.array([0.0, 0.0, observer_distance], dtype=np.float64)


def helioprojective_to_observer_ray(world_rad: tuple[float, float]) -> np.ndarray:
    lon, lat = world_rad
    ray = np.array([
        math.tan(lon),
        math.tan(lat) / math.cos(lon),
        -1.0,
    ], dtype=np.float64)
    return ray / np.linalg.norm(ray)


def wcs_plane_internal_to_hpc_ray(plane_internal: tuple[float, float], meta: JHVMeta) -> np.ndarray:
    world_rad = project_plane_internal_to_world(plane_internal, meta)
    return helioprojective_to_observer_ray(world_rad)


def observer_plane_point(world_rad: tuple[float, float], center_ray: np.ndarray, plane_distance: float, observer_distance: float) -> np.ndarray:
    ray = helioprojective_to_observer_ray(world_rad)
    denom = float(np.dot(center_ray, ray))
    if denom <= 0.0:
        raise ValueError("Ray is outside the forward observer-plane hemisphere")
    return observer_position(observer_distance) + (plane_distance / denom) * ray


def observer_shell_point(world_rad: tuple[float, float], shell_radius: float, observer_distance: float) -> np.ndarray:
    return observer_position(observer_distance) + shell_radius * helioprojective_to_observer_ray(world_rad)


def observer_hpc_reference_point(world_rad: tuple[float, float], observer_distance: float) -> np.ndarray:
    ray = helioprojective_to_observer_ray(world_rad)
    if ray[2] >= 0.0:
        raise ValueError("Ray is outside the forward HPC reference plane")
    return observer_position(observer_distance) - (observer_distance / ray[2]) * ray


def hpc_plane_point_from_wcs_plane_internal(plane_internal: tuple[float, float], meta: JHVMeta) -> np.ndarray:
    world_rad = project_plane_internal_to_world(plane_internal, meta)
    return observer_hpc_reference_point(world_rad, meta.observer_distance)


def angular_error_rad(point: np.ndarray, world_rad: tuple[float, float], observer_distance: float) -> float:
    ray = helioprojective_to_observer_ray(world_rad)
    point_dir = point - observer_position(observer_distance)
    point_dir /= np.linalg.norm(point_dir)
    dotp = float(np.clip(np.dot(point_dir, ray), -1.0, 1.0))
    return math.acos(dotp)


def curvature_sign_2d(start: np.ndarray, mid: np.ndarray, end: np.ndarray) -> float:
    chord = end - start
    chord_len2 = float(np.dot(chord, chord))
    if chord_len2 == 0.0:
        return 0.0
    t = float(np.dot(mid - start, chord) / chord_len2)
    proj = start + t * chord
    normal = mid - proj
    center = 0.5 * (start + end)
    return float(np.dot(normal, mid - center))


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


def hpc_bounds_degrees(meta: JHVMeta, aspect: float) -> tuple[float, float, float, float]:
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


def ortho_screen_to_world(screen_xy: tuple[float, float]) -> tuple[float, float, float] | None:
    x, y = screen_xy
    radius2 = x * x + y * y
    if radius2 > 1.0:
        return None
    return (x, y, math.sqrt(max(0.0, 1.0 - radius2)))


def orthographic_vs_hpc_screen_pixel_centers(screen_xy: tuple[float, float], meta: JHVMeta) -> tuple[tuple[float, float], tuple[float, float]] | None:
    world_xyz = ortho_screen_to_world(screen_xy)
    if world_xyz is None:
        return None

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
    parser = argparse.ArgumentParser(description="Validate the JHV WCS orthographic code path against astropy.wcs.")
    parser.add_argument("fits_file", type=Path)
    parser.add_argument("--hdu", type=int, default=None, help="Explicit FITS HDU index to use")
    parser.add_argument("--samples", type=int, default=1000, help="Number of random 3D samples")
    parser.add_argument("--seed", type=int, default=0, help="Random seed")
    parser.add_argument("--report-worst", type=int, default=5, help="How many worst samples to print")
    parser.add_argument("--all-pixels", action="store_true", help="Validate all pixel centers instead of random 3D samples")
    parser.add_argument("--inverse-tan", action="store_true", help="Validate the TAN inverse plane->world mapping")
    parser.add_argument("--inverse-azp", action="store_true", help="Validate the non-slanted AZP inverse plane->world mapping")
    parser.add_argument("--inverse-zpn", action="store_true", help="Validate the primary-branch ZPN inverse plane->world mapping")
    parser.add_argument("--observer-hpc-prototype", action="store_true", help="Prototype the deterministic intermediate HPC image plane recovered from inverse WCS")
    parser.add_argument("--hpc-render-compare", action="store_true", help="Render a bounded HPC screen through JHV and Astropy mappings and write diagnostic PNGs")
    parser.add_argument("--ortho-vs-hpc-screen-compare", action="store_true", help="Compare ortho on-disk sampling against HPC sampling at the same displayed screen radius")
    parser.add_argument("--render-size", type=int, default=512, help="Square output size for HPC diagnostic renderings")
    parser.add_argument("--output-dir", type=Path, default=Path("extra/test/out"), help="Directory for diagnostic PNGs")
    parser.add_argument("--observer-plane-prototype", action="store_true", help="Prototype constant-plane observer embedding on the image grid")
    parser.add_argument("--plane-distance-scale", type=float, default=1.0, help="Scale factor k for plane distance L = k * observerDistance in observer-plane prototype mode")
    parser.add_argument("--observer-shell-prototype", action="store_true", help="Prototype constant-radius observer-shell embedding on the image grid")
    parser.add_argument("--shell-radius-scale", type=float, default=1.0, help="Scale factor k for shell radius S = k * observerDistance in observer-shell prototype mode")
    args = parser.parse_args()

    with fits.open(args.fits_file) as hdul:
        hdu = find_image_hdu(hdul, args.hdu)
        header = hdu.header
        image_data = np.squeeze(np.asarray(hdu.data, dtype=np.float64))

    ensure_supported_projection(header)
    meta = build_jhv_meta(header)
    projection_wcs = build_projection_only_wcs(header)
    pixel_wcs = build_jhv_equivalent_astropy_wcs(header, meta)

    if args.hpc_render_compare:
        if image_data.ndim != 2:
            raise ValueError(f"HPC render compare expects 2D image data, got shape {image_data.shape!r}")

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
                result = orthographic_vs_hpc_screen_pixel_centers((sx, sy), meta)
                if result is None:
                    continue

                ortho_px, hpc_px = result
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

    if args.observer_hpc_prototype:
        edge_samples = {
            "top": [(0, 0), (meta.pixel_width // 2, 0), (meta.pixel_width - 1, 0)],
            "bottom": [(0, meta.pixel_height - 1), (meta.pixel_width // 2, meta.pixel_height - 1), (meta.pixel_width - 1, meta.pixel_height - 1)],
            "left": [(0, 0), (0, meta.pixel_height // 2), (0, meta.pixel_height - 1)],
            "right": [(meta.pixel_width - 1, 0), (meta.pixel_width - 1, meta.pixel_height // 2), (meta.pixel_width - 1, meta.pixel_height - 1)],
        }
        sample_pixels = [
            (0, 0),
            (meta.pixel_width - 1, 0),
            (0, meta.pixel_height - 1),
            (meta.pixel_width - 1, meta.pixel_height - 1),
            (meta.pixel_width // 2, meta.pixel_height // 2),
        ]
        embedded_points: list[tuple[tuple[int, int], np.ndarray]] = []
        mins = np.array([math.inf, math.inf, math.inf], dtype=np.float64)
        maxs = np.array([-math.inf, -math.inf, -math.inf], dtype=np.float64)
        native_angle_err_max = 0.0
        native_angle_err_sum2 = 0.0
        native_angle_err_count = 0
        edge_points: dict[str, list[np.ndarray]] = {name: [] for name in edge_samples}

        for y in range(meta.pixel_height):
            fits_y = np.full(meta.pixel_width, y + 1.0, dtype=np.float64)
            fits_x = np.arange(meta.pixel_width, dtype=np.float64) + 1.0
            world = pixel_wcs.wcs_pix2world(np.column_stack((fits_x, fits_y)), 1)
            for x in range(meta.pixel_width):
                world_rad = (math.radians(float(world[x, 0])), math.radians(float(world[x, 1])))
                point = observer_hpc_reference_point(world_rad, meta.observer_distance)
                mins = np.minimum(mins, point)
                maxs = np.maximum(maxs, point)
                native_angle_err = angular_error_rad(point, world_rad, meta.observer_distance)
                native_angle_err_max = max(native_angle_err_max, native_angle_err)
                native_angle_err_sum2 += native_angle_err * native_angle_err
                native_angle_err_count += 1

            if y in {0, meta.pixel_height // 2, meta.pixel_height - 1}:
                for px, py in sample_pixels:
                    if py != y:
                        continue
                    world_rad = (math.radians(float(world[px, 0])), math.radians(float(world[px, 1])))
                    embedded_points.append(((px, py), observer_hpc_reference_point(world_rad, meta.observer_distance)))

            for edge_name, pixels in edge_samples.items():
                for px, py in pixels:
                    if py != y:
                        continue
                    world_rad = (math.radians(float(world[px, 0])), math.radians(float(world[px, 1])))
                    edge_points[edge_name].append(observer_hpc_reference_point(world_rad, meta.observer_distance))

        print(f"file={args.fits_file}")
        print("mode=observer_hpc_prototype")
        print(f"observer_distance={meta.observer_distance:.12f}")
        print(f"bbox_min=({mins[0]:.12e}, {mins[1]:.12e}, {mins[2]:.12e})")
        print(f"bbox_max=({maxs[0]:.12e}, {maxs[1]:.12e}, {maxs[2]:.12e})")
        if native_angle_err_count > 0:
            print(f"native_ray_angle_max_error_rad={native_angle_err_max:.12e}")
            print(f"native_ray_angle_rms_error_rad={math.sqrt(native_angle_err_sum2 / native_angle_err_count):.12e}")
        print("edge_curvature_xy:")
        for edge_name, points in edge_points.items():
            if len(points) == 3:
                curvature = curvature_sign_2d(points[0][:2], points[1][:2], points[2][:2])
                print(f"  {edge_name}={curvature:.12e}")
        print("sample_points:")
        for (px, py), point in embedded_points:
            print(f"  pixel=({px}, {py}) point=({point[0]:.12e}, {point[1]:.12e}, {point[2]:.12e})")
        return 0

    if args.observer_plane_prototype:
        center_world_rad = (
            meta.crval_internal_x / meta.plane_units_per_rad,
            meta.crval_internal_y / meta.plane_units_per_rad,
        )
        center_ray = helioprojective_to_observer_ray(center_world_rad)
        plane_distance = args.plane_distance_scale * meta.observer_distance

        edge_samples = {
            "top": [(0, 0), (meta.pixel_width // 2, 0), (meta.pixel_width - 1, 0)],
            "bottom": [(0, meta.pixel_height - 1), (meta.pixel_width // 2, meta.pixel_height - 1), (meta.pixel_width - 1, meta.pixel_height - 1)],
            "left": [(0, 0), (0, meta.pixel_height // 2), (0, meta.pixel_height - 1)],
            "right": [(meta.pixel_width - 1, 0), (meta.pixel_width - 1, meta.pixel_height // 2), (meta.pixel_width - 1, meta.pixel_height - 1)],
        }
        sample_pixels = [
            (0, 0),
            (meta.pixel_width - 1, 0),
            (0, meta.pixel_height - 1),
            (meta.pixel_width - 1, meta.pixel_height - 1),
            (meta.pixel_width // 2, meta.pixel_height // 2),
        ]
        embedded_points: list[tuple[tuple[int, int], np.ndarray]] = []
        mins = np.array([math.inf, math.inf, math.inf], dtype=np.float64)
        maxs = np.array([-math.inf, -math.inf, -math.inf], dtype=np.float64)
        denom_min = math.inf
        denom_max = -math.inf
        skipped_pixels = 0
        native_angle_err_max = 0.0
        native_angle_err_sum2 = 0.0
        native_angle_err_count = 0
        edge_points: dict[str, list[np.ndarray]] = {name: [] for name in edge_samples}

        for y in range(meta.pixel_height):
            fits_y = np.full(meta.pixel_width, y + 1.0, dtype=np.float64)
            fits_x = np.arange(meta.pixel_width, dtype=np.float64) + 1.0
            world = pixel_wcs.wcs_pix2world(np.column_stack((fits_x, fits_y)), 1)
            for x in range(meta.pixel_width):
                world_rad = (math.radians(float(world[x, 0])), math.radians(float(world[x, 1])))
                ray = helioprojective_to_observer_ray(world_rad)
                denom = float(np.dot(center_ray, ray))
                if denom <= 0.0:
                    skipped_pixels += 1
                    continue
                point = observer_plane_point(world_rad, center_ray, plane_distance, meta.observer_distance)
                mins = np.minimum(mins, point)
                maxs = np.maximum(maxs, point)
                denom_min = min(denom_min, denom)
                denom_max = max(denom_max, denom)
                native_angle_err = angular_error_rad(point, world_rad, meta.observer_distance)
                native_angle_err_max = max(native_angle_err_max, native_angle_err)
                native_angle_err_sum2 += native_angle_err * native_angle_err
                native_angle_err_count += 1

            if y in {0, meta.pixel_height // 2, meta.pixel_height - 1}:
                for px, py in sample_pixels:
                    if py != y:
                        continue
                    world_rad = (math.radians(float(world[px, 0])), math.radians(float(world[px, 1])))
                    try:
                        point = observer_plane_point(world_rad, center_ray, plane_distance, meta.observer_distance)
                        embedded_points.append(((px, py), point))
                    except ValueError:
                        pass
            for edge_name, pixels in edge_samples.items():
                for px, py in pixels:
                    if py != y:
                        continue
                    world_rad = (math.radians(float(world[px, 0])), math.radians(float(world[px, 1])))
                    try:
                        edge_points[edge_name].append(observer_plane_point(world_rad, center_ray, plane_distance, meta.observer_distance))
                    except ValueError:
                        edge_points[edge_name].append(np.array([math.nan, math.nan, math.nan]))

        center_point = observer_position(meta.observer_distance) + plane_distance * center_ray
        print(f"file={args.fits_file}")
        print(f"mode=observer_plane_prototype plane_distance_scale={args.plane_distance_scale:.6f}")
        print(f"observer_distance={meta.observer_distance:.12f}")
        print(f"plane_distance={plane_distance:.12f}")
        print(f"center_ray=({center_ray[0]:.12e}, {center_ray[1]:.12e}, {center_ray[2]:.12e})")
        print(f"center_point=({center_point[0]:.12e}, {center_point[1]:.12e}, {center_point[2]:.12e})")
        print(f"skipped_pixels={skipped_pixels}")
        if skipped_pixels < meta.pixel_width * meta.pixel_height:
            print(f"denom_range=[{denom_min:.12e}, {denom_max:.12e}]")
            print(f"bbox_min=({mins[0]:.12e}, {mins[1]:.12e}, {mins[2]:.12e})")
            print(f"bbox_max=({maxs[0]:.12e}, {maxs[1]:.12e}, {maxs[2]:.12e})")
            if native_angle_err_count > 0:
                print(f"native_ray_angle_max_error_rad={native_angle_err_max:.12e}")
                print(f"native_ray_angle_rms_error_rad={math.sqrt(native_angle_err_sum2 / native_angle_err_count):.12e}")
        print("edge_curvature_xy:")
        for edge_name, points in edge_points.items():
            if len(points) == 3 and not any(np.isnan(p[0]) for p in points):
                curvature = curvature_sign_2d(points[0][:2], points[1][:2], points[2][:2])
                print(f"  {edge_name}={curvature:.12e}")
            else:
                print(f"  {edge_name}=nan")
        print("sample_points:")
        for (px, py), point in embedded_points:
            print(f"  pixel=({px}, {py}) point=({point[0]:.12e}, {point[1]:.12e}, {point[2]:.12e})")
        return 0

    if args.observer_shell_prototype:
        shell_radius = args.shell_radius_scale * meta.observer_distance
        edge_samples = {
            "top": [(0, 0), (meta.pixel_width // 2, 0), (meta.pixel_width - 1, 0)],
            "bottom": [(0, meta.pixel_height - 1), (meta.pixel_width // 2, meta.pixel_height - 1), (meta.pixel_width - 1, meta.pixel_height - 1)],
            "left": [(0, 0), (0, meta.pixel_height // 2), (0, meta.pixel_height - 1)],
            "right": [(meta.pixel_width - 1, 0), (meta.pixel_width - 1, meta.pixel_height // 2), (meta.pixel_width - 1, meta.pixel_height - 1)],
        }
        sample_pixels = [
            (0, 0),
            (meta.pixel_width - 1, 0),
            (0, meta.pixel_height - 1),
            (meta.pixel_width - 1, meta.pixel_height - 1),
            (meta.pixel_width // 2, meta.pixel_height // 2),
        ]
        embedded_points: list[tuple[tuple[int, int], np.ndarray]] = []
        mins = np.array([math.inf, math.inf, math.inf], dtype=np.float64)
        maxs = np.array([-math.inf, -math.inf, -math.inf], dtype=np.float64)
        native_angle_err_max = 0.0
        native_angle_err_sum2 = 0.0
        native_angle_err_count = 0
        edge_points: dict[str, list[np.ndarray]] = {name: [] for name in edge_samples}

        for y in range(meta.pixel_height):
            fits_y = np.full(meta.pixel_width, y + 1.0, dtype=np.float64)
            fits_x = np.arange(meta.pixel_width, dtype=np.float64) + 1.0
            world = pixel_wcs.wcs_pix2world(np.column_stack((fits_x, fits_y)), 1)
            for x in range(meta.pixel_width):
                world_rad = (math.radians(float(world[x, 0])), math.radians(float(world[x, 1])))
                point = observer_shell_point(world_rad, shell_radius, meta.observer_distance)
                mins = np.minimum(mins, point)
                maxs = np.maximum(maxs, point)
                native_angle_err = angular_error_rad(point, world_rad, meta.observer_distance)
                native_angle_err_max = max(native_angle_err_max, native_angle_err)
                native_angle_err_sum2 += native_angle_err * native_angle_err
                native_angle_err_count += 1

            if y in {0, meta.pixel_height // 2, meta.pixel_height - 1}:
                for px, py in sample_pixels:
                    if py != y:
                        continue
                    world_rad = (math.radians(float(world[px, 0])), math.radians(float(world[px, 1])))
                    point = observer_shell_point(world_rad, shell_radius, meta.observer_distance)
                    embedded_points.append(((px, py), point))
            for edge_name, pixels in edge_samples.items():
                for px, py in pixels:
                    if py != y:
                        continue
                    world_rad = (math.radians(float(world[px, 0])), math.radians(float(world[px, 1])))
                    edge_points[edge_name].append(observer_shell_point(world_rad, shell_radius, meta.observer_distance))

        print(f"file={args.fits_file}")
        print(f"mode=observer_shell_prototype shell_radius_scale={args.shell_radius_scale:.6f}")
        print(f"observer_distance={meta.observer_distance:.12f}")
        print(f"shell_radius={shell_radius:.12f}")
        print(f"bbox_min=({mins[0]:.12e}, {mins[1]:.12e}, {mins[2]:.12e})")
        print(f"bbox_max=({maxs[0]:.12e}, {maxs[1]:.12e}, {maxs[2]:.12e})")
        if native_angle_err_count > 0:
            print(f"native_ray_angle_max_error_rad={native_angle_err_max:.12e}")
            print(f"native_ray_angle_rms_error_rad={math.sqrt(native_angle_err_sum2 / native_angle_err_count):.12e}")
        print("edge_curvature_xy:")
        for edge_name, points in edge_points.items():
            if len(points) == 3:
                curvature = curvature_sign_2d(points[0][:2], points[1][:2], points[2][:2])
                print(f"  {edge_name}={curvature:.12e}")
        print("sample_points:")
        for (px, py), point in embedded_points:
            print(f"  pixel=({px}, {py}) point=({point[0]:.12e}, {point[1]:.12e}, {point[2]:.12e})")
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
