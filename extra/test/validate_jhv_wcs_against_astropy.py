#!/usr/bin/env python3

import argparse
import math
import random
import sys
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import astropy.units as u
from astropy.coordinates import get_sun
from astropy.io import fits
from astropy.time import Time
from astropy.wcs import WCS
from PIL import Image


SUN_RADIUS_METER = 695700.0 * 1e3
SUN_MEAN_EARTH_DISTANCE_METER = 149_597_870_700.0
SUN_MEAN_EARTH_DISTANCE = SUN_MEAN_EARTH_DISTANCE_METER / SUN_RADIUS_METER
ARCSEC_PER_RAD = 180.0 * 3600.0 / math.pi
LOGPOLAR_MIN_RADIUS = 0.05
IDENTITY_QUAT = (0.0, 0.0, 0.0, 1.0)
LATI_SURFACE_BOUNDS_DEG = (-180.0, 180.0, -90.0, 90.0)
LATI_ZENITHAL_BOUNDS_DEG = (0.0, 360.0, -90.0, 90.0)
SURFACE_MAP_PROJECTIONS = {"CAR", "CEA"}
PV2_PROJECTIONS = {"AZP", "ZPN", "CEA"}
PIXEL_SAMPLED_FORWARD_PROJECTIONS = {"ZPN", "CAR", "CEA"}
DISPLAY_SECTOR = (0.0, 0.0, 0.0)
DISPLAY_CUTOFF = (0.0, 0.0, -1.0)
DISPLAY_RADII = (0.0, math.inf)
DISPLAY_SLIT = (0.0, 1.0)
PLANE_Z_EPS = 1e-8


# Metadata / WCS interpretation.

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


def projection_suffix(header) -> str:
    return str(header.get("CTYPE1", ""))[-3:]


def ctype_pair(header) -> tuple[str, str]:
    return str(header.get("CTYPE1", "")), str(header.get("CTYPE2", ""))


def default_angular_cunit(header, axis: int) -> str | None:
    cunit = header.get(f"CUNIT{axis}")
    if cunit is not None:
        return cunit
    ctype = str(header.get(f"CTYPE{axis}", ""))
    if ctype.endswith("CAR") or ctype.endswith("CEA"):
        return "deg"
    return None


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


def header_observed_date(header) -> str:
    observed_date = (
        header.get("DATE-AVG")
        or header.get("DATE_AVG")
        or header.get("DATE_OBS")
        or header.get("DATE-OBS")
    )
    if observed_date is None:
        raise ValueError("Missing DATE-OBS-style keyword")
    observed_date = str(observed_date)
    if str(header.get("INSTRUME", "")) == "LASCO":
        observed_time = header.get("TIME_OBS") or header.get("TIME-OBS")
        if observed_time is None:
            raise ValueError("LASCO header missing TIME_OBS/TIME-OBS")
        observed_date = observed_date.replace("/", "-") + "T" + str(observed_time)
    if observed_date.endswith("Z"):
        observed_date = observed_date[:-1]
    if len(observed_date) == 10:
        observed_date += "T00:00:00"
    return observed_date


def earth_distance_solar_radii(header) -> float:
    time = Time(header_observed_date(header), format="isot", scale="utc")
    return float(get_sun(time).distance.to_value(u.m) / SUN_RADIUS_METER)


def car_effective_cd_rad(header) -> tuple[float, float, float, float]:
    pc11 = float(header.get("PC1_1", 1.0))
    pc12 = float(header.get("PC1_2", 0.0))
    pc21 = float(header.get("PC2_1", 0.0))
    pc22 = float(header.get("PC2_2", 1.0))
    cdelt1_rad = math.radians(angular_header_value_to_deg(float(header["CDELT1"]), default_angular_cunit(header, 1)))
    cdelt2_rad = math.radians(angular_header_value_to_deg(float(header["CDELT2"]), default_angular_cunit(header, 2)))
    return (
        cdelt1_rad * pc11,
        cdelt1_rad * pc12,
        cdelt2_rad * pc21,
        cdelt2_rad * pc22,
    )


def cea_effective_cd(header) -> tuple[float, float, float, float]:
    pc11 = float(header.get("PC1_1", 1.0))
    pc12 = float(header.get("PC1_2", 0.0))
    pc21 = float(header.get("PC2_1", 0.0))
    pc22 = float(header.get("PC2_2", 1.0))
    cdelt1_rad = math.radians(angular_header_value_to_deg(float(header["CDELT1"]), default_angular_cunit(header, 1)))
    cdelt2_eq = float(header["CDELT2"])
    return (
        cdelt1_rad * pc11,
        cdelt1_rad * pc12,
        cdelt2_eq * pc21,
        cdelt2_eq * pc22,
    )


def build_astropy_wcs_base(header, projection: str, crval1_deg: float, crval2_deg: float) -> WCS:
    ctype1, ctype2 = ctype_pair(header)
    wcs = WCS(naxis=2)
    if projection in SURFACE_MAP_PROJECTIONS:
        wcs.wcs.ctype = [ctype1, ctype2]
        wcs.wcs.cunit = [default_angular_cunit(header, 1) or "deg", default_angular_cunit(header, 2) or "deg"]
    else:
        wcs.wcs.ctype = [f"RA---{projection}", f"DEC--{projection}"]
    wcs.wcs.crval = [crval1_deg, crval2_deg]
    return wcs


def build_jhv_meta(header) -> JHVMeta:
    pixel_width = int(header.get("ZNAXIS1", header.get("NAXIS1")))
    pixel_height = int(header.get("ZNAXIS2", header.get("NAXIS2")))
    projection = projection_suffix(header)

    arcsec_x = unit_scale_from_cunit(default_angular_cunit(header, 1))
    arcsec_y = unit_scale_from_cunit(default_angular_cunit(header, 2))
    arcsec_per_pixel_x = float(header["CDELT1"]) * arcsec_x
    arcsec_per_pixel_y = float(header["CDELT2"]) * arcsec_y

    dsun_obs = header.get("DSUN_OBS")
    observer_distance = dsun_obs / SUN_RADIUS_METER if dsun_obs is not None else earth_distance_solar_radii(header)

    if projection in SURFACE_MAP_PROJECTIONS:
        unit_per_arcsec = math.pi / (180.0 * 3600.0)
        plane_units_per_rad = 1.0
        cd11, cd12, cd21, cd22 = cea_effective_cd(header) if projection == "CEA" else car_effective_cd_rad(header)
        unit_per_pixel_x = math.hypot(cd11, cd21)
        unit_per_pixel_y = math.hypot(cd12, cd22)
        arcsec_per_pixel_x = math.degrees(unit_per_pixel_x) * 3600.0
        arcsec_per_pixel_y = math.degrees(unit_per_pixel_y) * 3600.0 if projection == "CAR" else unit_per_pixel_y
        crval_internal_x = math.radians(angular_header_value_to_deg(header.get("CRVAL1", 0.0), default_angular_cunit(header, 1)))
        if projection == "CEA":
            lam = float(header.get("PV2_1", 1.0))
            crval_lat = math.radians(angular_header_value_to_deg(header.get("CRVAL2", 0.0), default_angular_cunit(header, 2)))
            crval_internal_y = math.sin(crval_lat) / lam
        else:
            crval_internal_y = math.radians(angular_header_value_to_deg(header.get("CRVAL2", 0.0), default_angular_cunit(header, 2)))
    else:
        radius_sun_in_arcsec = math.degrees(math.atan2(1.0, observer_distance)) * 3600.0
        unit_per_arcsec = 1.0 / radius_sun_in_arcsec
        plane_units_per_rad = unit_per_arcsec * ARCSEC_PER_RAD
        unit_per_pixel_x = abs(arcsec_per_pixel_x * unit_per_arcsec)
        unit_per_pixel_y = abs(arcsec_per_pixel_y * unit_per_arcsec)
        crval_internal_x = float(header.get("CRVAL1", 0.0)) * arcsec_x * unit_per_arcsec
        crval_internal_y = float(header.get("CRVAL2", 0.0)) * arcsec_y * unit_per_arcsec

    crpix1_gl = float(header.get("CRPIX1", (pixel_width + 1) / 2.0)) - 0.5
    crpix2_gl = float(header.get("CRPIX2", (pixel_height + 1) / 2.0)) - 0.5

    try:
        pc2_1 = float(header["PC2_1"])
        pc1_1 = float(header["PC1_1"])
        if projection in SURFACE_MAP_PROJECTIONS:
            cd11, _, cd21, _ = cea_effective_cd(header) if projection == "CEA" else car_effective_cd_rad(header)
            crota_rad = math.atan2(cd21, cd11)
        else:
            crota_rad = math.atan2(pc2_1 / (arcsec_per_pixel_x / arcsec_per_pixel_y), pc1_1)
    except Exception:
        crota_deg = (
            header.get("CROTA")
            or header.get("CROTA1")
            or header.get("CROTA2")
            or 0.0
        )
        crota_rad = math.radians(float(crota_deg))

    pv2 = [float(header.get(f"PV2_{i}", 0.0)) for i in range(6)]
    if projection == "CEA":
        pv2[1] = float(header.get("PV2_1", 1.0))

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
        projection=projection,
        pv2=tuple(pv2),
    )


def ensure_supported_projection(header) -> None:
    ctype1, ctype2 = ctype_pair(header)
    if ctype1[-3:] != ctype2[-3:]:
        raise ValueError(f"Mismatched projection types: {ctype1!r} / {ctype2!r}")
    if not (ctype1.endswith("TAN") or ctype1.endswith("AZP") or ctype1.endswith("ZPN") or ctype1.endswith("CAR") or ctype1.endswith("CEA")):
        raise ValueError(f"Only TAN, AZP, ZPN, CAR, and CEA FITS files are supported right now, got {ctype1!r} / {ctype2!r}")


# Shared geometric and projection math.

def wrap_delta_lon_rad(lon: float, lon0: float) -> float:
    return (lon - lon0 + math.pi) % (2.0 * math.pi) - math.pi


def world2helioprojective(point_xyz: tuple[float, float, float], observer_distance: float) -> tuple[float, float]:
    x, y, z = point_xyz
    zeta = observer_distance - z
    return (
        math.atan2(x, zeta),
        math.atan2(y, math.sqrt(x * x + zeta * zeta)),
    )


def quat_rotate_vector(quat: tuple[float, float, float, float], vec: tuple[float, float, float]) -> tuple[float, float, float]:
    qx, qy, qz, qw = quat
    vx, vy, vz = vec
    tx = qy * vz - qz * vy + qw * vx
    ty = qz * vx - qx * vz + qw * vy
    tz = qx * vy - qy * vx + qw * vz
    return (
        vx + 2.0 * (qy * tz - qz * ty),
        vy + 2.0 * (qz * tx - qx * tz),
        vz + 2.0 * (qx * ty - qy * tx),
    )


def quat_rotate_vector_inverse(quat: tuple[float, float, float, float], vec: tuple[float, float, float]) -> tuple[float, float, float]:
    qx, qy, qz, qw = quat
    vx, vy, vz = vec
    tx = -qy * vz + qz * vy + qw * vx
    ty = -qz * vx + qx * vz + qw * vy
    tz = -qx * vy + qy * vx + qw * vz
    return (
        vx + 2.0 * (-qy * tz + qz * ty),
        vy + 2.0 * (-qz * tx + qx * tz),
        vz + 2.0 * (-qx * ty + qy * tx),
    )


# CPU mirror of the shared GLSL helpers in solarCommon.frag.
def wrapDeltaLongitude(lon: float, lon0: float) -> float:
    return wrap_delta_lon_rad(lon, lon0)


def worldToHelioprojective(world_xyz: tuple[float, float, float], observer_distance: float) -> tuple[float, float]:
    return world2helioprojective(world_xyz, observer_distance)


def rotate_vector(quat: tuple[float, float, float, float], vec: tuple[float, float, float]) -> tuple[float, float, float]:
    return quat_rotate_vector(quat, vec)


def rotate_vector_inverse(quat: tuple[float, float, float, float], vec: tuple[float, float, float]) -> tuple[float, float, float]:
    return quat_rotate_vector_inverse(quat, vec)


def apply_center(v: tuple[float, float, float], shift: tuple[float, float], crota_quat: tuple[float, float, float, float]) -> tuple[float, float, float]:
    shifted = (v[0] - shift[0], v[1] - shift[1], v[2])
    return rotate_vector_inverse(crota_quat, shifted)


def nativeZenithalCoordinates(helioprojective: tuple[float, float], meta: JHVMeta) -> tuple[float, float, float]:
    phi, theta = helioprojective
    phi0 = meta.crval_internal_x / meta.plane_units_per_rad
    theta0 = meta.crval_internal_y / meta.plane_units_per_rad

    sin_lat = math.sin(theta)
    cos_lat = math.cos(theta)
    sin_lat0 = math.sin(theta0)
    cos_lat0 = math.cos(theta0)
    delta_lon = phi - phi0
    sin_delta_lon = math.sin(delta_lon)
    cos_delta_lon = math.cos(delta_lon)

    native_x = cos_lat * sin_delta_lon
    native_y = cos_lat0 * sin_lat - sin_lat0 * cos_lat * cos_delta_lon
    cos_native_distance = sin_lat0 * sin_lat + cos_lat0 * cos_lat * cos_delta_lon
    return native_x, native_y, cos_native_distance


def projectTanToWcsPlane(helioprojective: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    native_x, native_y, cos_native_distance = nativeZenithalCoordinates(helioprojective, meta)
    if cos_native_distance <= 0.0:
        raise ValueError("Point is outside the visible TAN hemisphere")
    return (
        meta.plane_units_per_rad * (native_x / cos_native_distance),
        meta.plane_units_per_rad * (native_y / cos_native_distance),
    )


def projectCarToWcsPlane(world_xyz: tuple[float, float, float], meta: JHVMeta) -> tuple[float, float]:
    norm = math.sqrt(world_xyz[0] * world_xyz[0] + world_xyz[1] * world_xyz[1] + world_xyz[2] * world_xyz[2])
    if norm == 0.0:
        return (math.nan, math.nan)
    lon = math.atan2(world_xyz[0], world_xyz[2])
    lat = math.asin(max(-1.0, min(1.0, world_xyz[1] / norm)))
    lon0 = meta.crval_internal_x
    lat0 = meta.crval_internal_y
    return (
        meta.plane_units_per_rad * wrapDeltaLongitude(lon, lon0),
        meta.plane_units_per_rad * (lat - lat0),
    )


def projectCeaToWcsPlane(world_xyz: tuple[float, float, float], meta: JHVMeta) -> tuple[float, float]:
    norm = math.sqrt(world_xyz[0] * world_xyz[0] + world_xyz[1] * world_xyz[1] + world_xyz[2] * world_xyz[2])
    if norm == 0.0:
        return (math.nan, math.nan)
    lon = math.atan2(world_xyz[0], world_xyz[2])
    lat = math.asin(max(-1.0, min(1.0, world_xyz[1] / norm)))
    lon0 = meta.crval_internal_x
    y0 = meta.crval_internal_y
    lam = max(abs(meta.pv2[1]), 1e-12)
    return (
        meta.plane_units_per_rad * wrapDeltaLongitude(lon, lon0),
        meta.plane_units_per_rad * (math.sin(lat) / lam - y0),
    )


def projectAzpToWcsPlane(helioprojective: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    native_x, native_y, cos_native_distance = nativeZenithalCoordinates(helioprojective, meta)
    mu = meta.pv2[1]
    gamma = math.radians(meta.pv2[2])
    native_radius = math.hypot(native_x, native_y)
    if native_radius == 0.0:
        return (0.0, 0.0)
    denom = mu + cos_native_distance - native_y * math.tan(gamma)
    if denom == 0.0:
        raise ValueError("Point is on the AZP singularity")
    radial = (mu + 1.0) * native_radius / denom
    return (
        meta.plane_units_per_rad * radial * native_x / native_radius,
        meta.plane_units_per_rad * radial * native_y / (native_radius * math.cos(gamma)),
    )


def zpnRadialAndDerivative(eta_rad: float, meta: JHVMeta) -> tuple[float, float]:
    return zpn_radial(meta, eta_rad), zpn_radial_derivative(meta, eta_rad)


def projectZpnToWcsPlane(helioprojective: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    native_x, native_y, cos_native_distance = nativeZenithalCoordinates(helioprojective, meta)
    native_radius = math.hypot(native_x, native_y)
    if native_radius == 0.0:
        return (0.0, 0.0)
    eta = math.acos(max(-1.0, min(1.0, cos_native_distance)))
    radial, derivative = zpnRadialAndDerivative(eta, meta)
    if radial < 0.0 or derivative <= 0.0:
        raise ValueError("Point is outside the primary forward ZPN branch")
    return (
        meta.plane_units_per_rad * radial * native_x / native_radius,
        meta.plane_units_per_rad * radial * native_y / native_radius,
    )


def tan_world_to_plane_internal(world_rad: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    return projectTanToWcsPlane(world_rad, meta)


def azp_world_to_plane_internal(world_rad: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    return projectAzpToWcsPlane(world_rad, meta)


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
    return projectZpnToWcsPlane(world_rad, meta)


def azp_plane_internal_to_world(plane_internal: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    gamma = math.radians(meta.pv2[2])
    x = plane_internal[0] / meta.plane_units_per_rad
    y = plane_internal[1] / meta.plane_units_per_rad
    r = math.hypot(x, y)

    lon0 = meta.crval_internal_x / meta.plane_units_per_rad
    lat0 = meta.crval_internal_y / meta.plane_units_per_rad
    mu = meta.pv2[1]
    sin_gamma = math.sin(gamma)
    cos_gamma = math.cos(gamma)

    if r == 0.0:
        return (lon0, lat0)

    mu_plus_1 = mu + 1.0
    a = 1.0 + y * sin_gamma / mu_plus_1
    k = (x * x + y * y * cos_gamma * cos_gamma) / (mu_plus_1 * mu_plus_1 * a * a)
    discriminant = 1.0 + k * (1.0 - mu * mu)
    if discriminant < 0.0:
        raise ValueError("Plane point is outside the real AZP inverse branch")
    cos_native_distance = (-k * mu + math.sqrt(discriminant)) / (1.0 + k)
    denom = (mu + cos_native_distance) / a
    native_x = x * denom / mu_plus_1
    native_y = y * cos_gamma * denom / mu_plus_1

    sin_lat0 = math.sin(lat0)
    cos_lat0 = math.cos(lat0)
    lat = math.asin(cos_native_distance * sin_lat0 + native_y * cos_lat0)
    lon = lon0 + math.atan2(native_x, cos_native_distance * cos_lat0 - native_y * sin_lat0)
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
    if meta.projection == "CAR":
        world_xyz = (
            math.cos(world_rad[1]) * math.sin(world_rad[0]),
            math.sin(world_rad[1]),
            math.cos(world_rad[1]) * math.cos(world_rad[0]),
        )
        return projectCarToWcsPlane(world_xyz, meta)
    if meta.projection == "CEA":
        world_xyz = (
            math.cos(world_rad[1]) * math.sin(world_rad[0]),
            math.sin(world_rad[1]),
            math.cos(world_rad[1]) * math.cos(world_rad[0]),
        )
        return projectCeaToWcsPlane(world_xyz, meta)
    if meta.projection == "TAN":
        return tan_world_to_plane_internal(world_rad, meta)
    if meta.projection == "AZP":
        return azp_world_to_plane_internal(world_rad, meta)
    if meta.projection == "ZPN":
        return zpn_world_to_plane_internal(world_rad, meta)
    raise ValueError(f"Unsupported projection {meta.projection!r}")


def project_plane_internal_to_world(plane_internal: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    if meta.projection == "CAR":
        lon0 = meta.crval_internal_x
        lat0 = meta.crval_internal_y
        return (
            lon0 + plane_internal[0] / meta.plane_units_per_rad,
            lat0 + plane_internal[1] / meta.plane_units_per_rad,
        )
    if meta.projection == "CEA":
        lon0 = meta.crval_internal_x
        y0 = meta.crval_internal_y
        lam = max(abs(meta.pv2[1]), 1e-12)
        return (
            lon0 + plane_internal[0] / meta.plane_units_per_rad,
            math.asin(max(-1.0, min(1.0, lam * (plane_internal[1] / meta.plane_units_per_rad + y0)))),
        )
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

    if meta.projection == "CAR":
        lon0 = meta.crval_internal_x
        lat0 = meta.crval_internal_y
        delta_lon = (lon - lon0 + math.pi) % (2.0 * math.pi) - math.pi
        x = meta.plane_units_per_rad * delta_lon
        y = meta.plane_units_per_rad * (lat - lat0)
        return np.column_stack((x, y))
    if meta.projection == "CEA":
        lon0 = meta.crval_internal_x
        y0 = meta.crval_internal_y
        lam = max(abs(meta.pv2[1]), 1e-12)
        delta_lon = (lon - lon0 + math.pi) % (2.0 * math.pi) - math.pi
        x = meta.plane_units_per_rad * delta_lon
        y = meta.plane_units_per_rad * (np.sin(lat) / lam - y0)
        return np.column_stack((x, y))

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


def rotate_z(vec_xy: tuple[float, float], angle_rad: float) -> tuple[float, float]:
    x, y = vec_xy
    c = math.cos(angle_rad)
    s = math.sin(angle_rad)
    return (
        c * x - s * y,
        s * x + c * y,
    )


def surface_map_wraps_x(meta: JHVMeta) -> bool:
    if meta.projection not in SURFACE_MAP_PROJECTIONS:
        return False
    width_internal = meta.pixel_width * abs(meta.unit_per_pixel_x)
    return abs(width_internal - 2.0 * math.pi) <= 2.0 * abs(meta.unit_per_pixel_x)


def wrap_source_x_pixel(x_px: float, meta: JHVMeta) -> float:
    if surface_map_wraps_x(meta):
        return x_px % meta.pixel_width
    return x_px


def pixel_center_error_px(jhv_px: tuple[float, float], astro_px: tuple[float, float], meta: JHVMeta) -> float:
    dx = abs(jhv_px[0] - astro_px[0])
    if surface_map_wraps_x(meta):
        dx = min(dx, abs((jhv_px[0] + meta.pixel_width) - astro_px[0]), abs(jhv_px[0] - (astro_px[0] + meta.pixel_width)))
    dy = abs(jhv_px[1] - astro_px[1])
    return max(dx, dy)


def plane_internal_to_pixel_center(plane_internal: tuple[float, float], meta: JHVMeta, wrap_x: bool = False) -> tuple[float, float]:
    rotated_internal = rotate_inverse_z(plane_internal, meta.crota_rad)
    px = rotated_internal[0] / meta.unit_per_pixel_x + meta.crpix1_gl
    py = -rotated_internal[1] / meta.unit_per_pixel_y + meta.crpix2_gl
    return (wrap_source_x_pixel(px, meta) if wrap_x else px, py)


def plane_internal_array_to_pixel_center(plane_internal: np.ndarray, meta: JHVMeta, wrap_x: bool = False) -> np.ndarray:
    c = math.cos(meta.crota_rad)
    s = math.sin(meta.crota_rad)
    rotated_x = c * plane_internal[:, 0] + s * plane_internal[:, 1]
    rotated_y = -s * plane_internal[:, 0] + c * plane_internal[:, 1]
    px = rotated_x / meta.unit_per_pixel_x + meta.crpix1_gl
    if wrap_x and surface_map_wraps_x(meta):
        px = np.mod(px, meta.pixel_width)
    py = -rotated_y / meta.unit_per_pixel_y + meta.crpix2_gl
    return np.column_stack((px, py))


def pixel_center_to_plane_internal(pixel_center: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    rotated_internal = (
        (pixel_center[0] - meta.crpix1_gl) * meta.unit_per_pixel_x,
        -(pixel_center[1] - meta.crpix2_gl) * meta.unit_per_pixel_y,
    )
    return rotate_z(rotated_internal, meta.crota_rad)


def pixel_center_to_world_deg(pixel_center: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    world_rad = project_plane_internal_to_world(pixel_center_to_plane_internal(pixel_center, meta), meta)
    return (math.degrees(world_rad[0]), math.degrees(world_rad[1]))


def mirrored_world_to_plane_internal(world_rad: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    if meta.projection == "CAR":
        lon, lat = world_rad
        world_xyz = (
            math.cos(lat) * math.sin(lon),
            math.sin(lat),
            math.cos(lat) * math.cos(lon),
        )
        return projectCarToWcsPlane(world_xyz, meta)
    if meta.projection == "CEA":
        lon, lat = world_rad
        world_xyz = (
            math.cos(lat) * math.sin(lon),
            math.sin(lat),
            math.cos(lat) * math.cos(lon),
        )
        return projectCeaToWcsPlane(world_xyz, meta)
    return projectHelioprojectiveToWcsPlane(world_rad, meta)


def mirrored_world_to_pixel_center(world_rad: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    plane_internal = mirrored_world_to_plane_internal(world_rad, meta)
    return plane_internal_to_pixel_center(plane_internal, meta, wrap_x=surface_map_wraps_x(meta))


def mirrored_world_array_to_pixel_center(world_deg: np.ndarray, meta: JHVMeta) -> np.ndarray:
    plane_internal = project_world_to_plane_internal_array(world_deg, meta)
    return plane_internal_array_to_pixel_center(plane_internal, meta, wrap_x=surface_map_wraps_x(meta))


def simple_tan_world_array_to_pixel_center(world_xyz: np.ndarray, meta: JHVMeta) -> np.ndarray:
    dx = world_xyz[:, 0] - meta.crval_internal_x
    dy = world_xyz[:, 1] - meta.crval_internal_y
    return plane_internal_array_to_pixel_center(np.column_stack((dx, dy)), meta)


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
    crval1_deg = angular_header_value_to_deg(header.get("CRVAL1", 0.0), default_angular_cunit(header, 1))
    crval2_deg = angular_header_value_to_deg(header.get("CRVAL2", 0.0), default_angular_cunit(header, 2))
    projection = projection_suffix(header)
    wcs = build_astropy_wcs_base(header, projection, crval1_deg, crval2_deg)
    wcs.wcs.crpix = [1.0, 1.0]
    wcs.wcs.cdelt = [1.0, 1.0]
    wcs.wcs.pc = [[1.0, 0.0], [0.0, 1.0]]
    if projection in PV2_PROJECTIONS:
        wcs.wcs.set_pv([(2, i, float(header.get(f"PV2_{i}", 0.0))) for i in range(6) if f"PV2_{i}" in header])
    return wcs


def build_jhv_equivalent_astropy_wcs(header, meta: JHVMeta) -> WCS:
    crval1_deg = angular_header_value_to_deg(header.get("CRVAL1", 0.0), default_angular_cunit(header, 1))
    crval2_deg = angular_header_value_to_deg(header.get("CRVAL2", 0.0), default_angular_cunit(header, 2))
    projection = projection_suffix(header)

    sx = abs(meta.arcsec_per_pixel_x) / 3600.0
    sy = abs(meta.arcsec_per_pixel_y) / 3600.0 if projection != "CEA" else abs(meta.arcsec_per_pixel_y) * 180.0 / math.pi
    c = math.cos(meta.crota_rad)
    s = math.sin(meta.crota_rad)

    wcs = build_astropy_wcs_base(header, projection, crval1_deg, crval2_deg)
    wcs.wcs.crpix = [meta.crpix1_gl + 0.5, meta.crpix2_gl + 0.5]
    wcs.wcs.cd = np.array([
        [c * sx, s * sy],
        [s * sx, -c * sy],
    ])
    if projection in PV2_PROJECTIONS:
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


def pixel_center_to_texcoord(px: float, py: float, image2d: np.ndarray) -> tuple[float, float]:
    return (px / image2d.shape[1], py / image2d.shape[0])


def texture_texel(image2d: np.ndarray, texel_x: int, texel_y: int) -> float:
    return float(image2d[image2d.shape[0] - 1 - texel_y, texel_x])


def wcsPlaneToPixelCenter(plane_internal: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    return plane_internal_to_pixel_center(plane_internal, meta)


def wcsPlaneToTexcoord(plane_internal: tuple[float, float], meta: JHVMeta, image2d: np.ndarray) -> tuple[float, float]:
    texcoord = pixel_center_to_texcoord(*wcsPlaneToPixelCenter(plane_internal, meta), image2d)
    return texcoord if clamp_coord(texcoord) else (math.nan, math.nan)


def wcsPlaneToWrappedXTexcoord(plane_internal: tuple[float, float], meta: JHVMeta, image2d: np.ndarray) -> tuple[float, float]:
    texcoord = pixel_center_to_texcoord(*plane_internal_to_pixel_center(plane_internal, meta, wrap_x=True), image2d)
    texcoord = (texcoord[0] % 1.0, texcoord[1])
    return texcoord if clamp_coord(texcoord) else (math.nan, math.nan)


def sample_texture_linear(image2d: np.ndarray, texcoord: tuple[float, float], wrap_x: bool = False) -> float:
    u, v = texcoord
    if not math.isfinite(u) or not math.isfinite(v):
        return math.nan

    if wrap_x:
        u = u % 1.0
    elif u < 0.0 or u > 1.0:
        return math.nan

    if v < 0.0 or v > 1.0:
        return math.nan

    fx = u * image2d.shape[1] - 0.5
    fy = v * image2d.shape[0] - 0.5
    x0 = int(math.floor(fx))
    y0 = int(math.floor(fy))
    tx = fx - x0
    ty = fy - y0
    x1 = x0 + 1
    y1 = y0 + 1

    y0 = min(max(y0, 0), image2d.shape[0] - 1)
    y1 = min(max(y1, 0), image2d.shape[0] - 1)

    if wrap_x:
        x0 = x0 % image2d.shape[1]
        x1 = x1 % image2d.shape[1]
    else:
        x0 = min(max(x0, 0), image2d.shape[1] - 1)
        x1 = min(max(x1, 0), image2d.shape[1] - 1)

    v00 = texture_texel(image2d, x0, y0)
    v10 = texture_texel(image2d, x1, y0)
    v01 = texture_texel(image2d, x0, y1)
    v11 = texture_texel(image2d, x1, y1)
    return (
        (1.0 - tx) * (1.0 - ty) * v00 +
        tx * (1.0 - ty) * v10 +
        (1.0 - tx) * ty * v01 +
        tx * ty * v11
    )


def sample_observer_image_linear(image2d: np.ndarray, px: float, py: float) -> float:
    return sample_texture_linear(image2d, pixel_center_to_texcoord(px, py, image2d))


def sample_surface_map_linear(image2d: np.ndarray, px: float, py: float, meta: JHVMeta) -> float:
    return sample_texture_linear(image2d, pixel_center_to_texcoord(px, py, image2d), wrap_x=surface_map_wraps_x(meta))


def sample_source_linear(image2d: np.ndarray, px: float, py: float, meta: JHVMeta) -> float:
    if meta.projection in SURFACE_MAP_PROJECTIONS:
        return sample_surface_map_linear(image2d, px, py, meta)
    return sample_observer_image_linear(image2d, px, py)


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


# CPU mirror of solarHpc.frag.
def screenToHelioprojective(scrpos: tuple[float, float], bounds_deg: tuple[float, float, float, float]) -> tuple[float, float]:
    return hpc_screen_to_world_rad(scrpos, bounds_deg)


def helioprojectiveToWorld(helioprojective: tuple[float, float], observer_distance: float) -> tuple[bool, tuple[float, float, float]]:
    ray = helioprojective_to_observer_ray(helioprojective)
    b = observer_distance * ray[2]
    c = observer_distance * observer_distance - 1.0
    discriminant = b * b - c
    if discriminant < 0.0:
        return False, (0.0, 0.0, 0.0)

    root = math.sqrt(discriminant)
    t_near = -b - root
    t_far = -b + root
    t = t_near if t_near > 0.0 else t_far
    if t <= 0.0:
        return False, (0.0, 0.0, 0.0)

    observer = (0.0, 0.0, observer_distance)
    world = (
        observer[0] + t * ray[0],
        observer[1] + t * ray[1],
        observer[2] + t * ray[2],
    )
    return True, world


def helioprojectiveToHpcXY(helioprojective: tuple[float, float], observer_distance: float) -> tuple[float, float]:
    ray = helioprojective_to_observer_ray(helioprojective)
    scale = -observer_distance / ray[2]
    return float(scale * ray[0]), float(scale * ray[1])


def hpcEnhancementFactor(hpc_xy: tuple[float, float]) -> float:
    return max(1.0, math.hypot(hpc_xy[0], hpc_xy[1]))


def passes_sector(xy: tuple[float, float], sector: tuple[float, float, float] = DISPLAY_SECTOR) -> bool:
    if sector[2] == 0.0:
        return True
    theta = math.atan2(xy[1], xy[0])
    return sector[0] <= theta <= sector[1]


def passes_radii(radial2: float, radii: tuple[float, float] = DISPLAY_RADII) -> bool:
    return radii[0] * radii[0] <= radial2 <= radii[1] * radii[1]


def passes_cutoff(xy: tuple[float, float], cutoff: tuple[float, float, float] = DISPLAY_CUTOFF) -> bool:
    if cutoff[2] < 0.0:
        return True
    geometry_flat_dist = abs(xy[0] * cutoff[0] + xy[1] * cutoff[1])
    cutoff_alt = (-cutoff[1], cutoff[0])
    geometry_flat_dist_alt = abs(xy[0] * cutoff_alt[0] + xy[1] * cutoff_alt[1])
    return geometry_flat_dist <= cutoff[2] and geometry_flat_dist_alt <= cutoff[2]


def clamp_coord(coord: tuple[float, float], slit: tuple[float, float] = DISPLAY_SLIT) -> bool:
    return slit[0] <= coord[0] <= slit[1] and 0.0 <= coord[1] <= 1.0


def clamp_value(value: float, low: float, high: float) -> bool:
    return low <= value <= high


def getScrPos(scrpos: tuple[float, float], slit: tuple[float, float] = DISPLAY_SLIT) -> tuple[float, float]:
    return scrpos if clamp_coord(scrpos, slit) else (math.nan, math.nan)


def clipHpcGeometry(hpc_xy: tuple[float, float]) -> bool:
    radial2 = hpc_xy[0] * hpc_xy[0] + hpc_xy[1] * hpc_xy[1]
    return passes_sector(hpc_xy) and passes_radii(radial2) and passes_cutoff(hpc_xy)


def projectHelioprojectiveToWcsPlane(helioprojective: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    if meta.projection == "TAN":
        return projectTanToWcsPlane(helioprojective, meta)
    if meta.projection == "AZP":
        return projectAzpToWcsPlane(helioprojective, meta)
    if meta.projection == "ZPN":
        return projectZpnToWcsPlane(helioprojective, meta)
    raise ValueError(f"HPC path does not support projection {meta.projection!r}")


def sampleHpcTexcoord(helioprojective: tuple[float, float], hpc_xy: tuple[float, float], meta: JHVMeta, image2d: np.ndarray) -> tuple[tuple[float, float], float]:
    enhancement_factor = 1.0
    hp = helioprojective
    hit, world = helioprojectiveToWorld(hp, meta.observer_distance)
    if hit:
        hp = worldToHelioprojective(world, meta.observer_distance)
    else:
        enhancement_factor = hpcEnhancementFactor(hpc_xy)

    try:
        plane = projectHelioprojectiveToWcsPlane(hp, meta)
    except ValueError:
        return (math.nan, math.nan), enhancement_factor
    return wcsPlaneToTexcoord(plane, meta, image2d), enhancement_factor


# solarHpc.frag mirror.

def renderHpcTexcoords(
    scrpos: tuple[float, float],
    bounds_deg: tuple[float, float, float, float],
    meta: JHVMeta,
    image2d: np.ndarray,
    diff_meta: JHVMeta | None = None,
    diff_image2d: np.ndarray | None = None,
) -> tuple[tuple[float, float], tuple[float, float], float, float, tuple[float, float], tuple[float, float]]:
    clamped_scrpos = getScrPos(scrpos)
    if not math.isfinite(clamped_scrpos[0]) or not math.isfinite(clamped_scrpos[1]):
        nan2 = (math.nan, math.nan)
        return nan2, nan2, math.nan, math.nan, nan2, nan2

    helioprojective = screenToHelioprojective(clamped_scrpos, bounds_deg)
    hpc_xy = helioprojectiveToHpcXY(helioprojective, meta.observer_distance)
    if not clipHpcGeometry(hpc_xy):
        nan2 = (math.nan, math.nan)
        return nan2, nan2, math.nan, math.nan, helioprojective, nan2

    texcoord, enhancement_factor = sampleHpcTexcoord(helioprojective, hpc_xy, meta, image2d)
    if diff_meta is None:
        return texcoord, texcoord, enhancement_factor, enhancement_factor, helioprojective, hpc_xy

    diff_hpc_xy = helioprojectiveToHpcXY(helioprojective, diff_meta.observer_distance)
    if not clipHpcGeometry(diff_hpc_xy):
        nan2 = (math.nan, math.nan)
        return texcoord, nan2, enhancement_factor, math.nan, helioprojective, diff_hpc_xy

    diff_texcoord, diff_enhancement_factor = sampleHpcTexcoord(
        helioprojective,
        diff_hpc_xy,
        diff_meta,
        diff_image2d if diff_image2d is not None else image2d,
    )
    return texcoord, diff_texcoord, enhancement_factor, diff_enhancement_factor, helioprojective, diff_hpc_xy


# solarLati.frag mirror.

def crota_quaternion(meta: JHVMeta) -> tuple[float, float, float, float]:
    half = 0.5 * meta.crota_rad
    return (0.0, 0.0, math.sin(half), math.cos(half))


def displayLatitudinalWorld(
    scrpos: tuple[float, float],
    bounds_deg: tuple[float, float, float, float],
    display_map_quat: tuple[float, float, float, float] = IDENTITY_QUAT,
) -> tuple[float, float, float]:
    longitude = math.radians(bounds_deg[0] + scrpos[0] * (bounds_deg[1] - bounds_deg[0]))
    latitude = math.radians(bounds_deg[2] + scrpos[1] * (bounds_deg[3] - bounds_deg[2]))
    if not clamp_value(latitude, -0.5 * math.pi, 0.5 * math.pi):
        return (math.nan, math.nan, math.nan)
    cos_latitude = math.cos(latitude)
    display_surface = (
        cos_latitude * math.sin(longitude),
        math.sin(latitude),
        cos_latitude * math.cos(longitude),
    )
    return rotate_vector_inverse(display_map_quat, display_surface)


def sampleLatiCarTexcoord(
    scrpos: tuple[float, float],
    bounds_deg: tuple[float, float, float, float],
    meta: JHVMeta,
    image2d: np.ndarray,
    display_map_quat: tuple[float, float, float, float] = IDENTITY_QUAT,
) -> tuple[float, float]:
    world = displayLatitudinalWorld(scrpos, bounds_deg, display_map_quat)
    plane = projectCarToWcsPlane(world, meta)
    return wcsPlaneToWrappedXTexcoord(plane, meta, image2d)


def sampleLatiCeaTexcoord(
    scrpos: tuple[float, float],
    bounds_deg: tuple[float, float, float, float],
    meta: JHVMeta,
    image2d: np.ndarray,
    display_map_quat: tuple[float, float, float, float] = IDENTITY_QUAT,
) -> tuple[float, float]:
    world = displayLatitudinalWorld(scrpos, bounds_deg, display_map_quat)
    plane = projectCeaToWcsPlane(world, meta)
    return wcsPlaneToWrappedXTexcoord(plane, meta, image2d)


def sampleLatiZenithalTexcoord(
    scrpos: tuple[float, float],
    bounds_deg: tuple[float, float, float, float],
    meta: JHVMeta,
    image2d: np.ndarray,
    grid: tuple[float, float, float],
    delta_t: float = 0.0,
) -> tuple[float, float]:
    longitude = grid[0] + scrpos[0] * (2.0 * math.pi)
    latitude = grid[1] + (scrpos[1] - 0.5) * math.pi
    if latitude < -0.5 * math.pi or latitude > 0.5 * math.pi:
        return (math.nan, math.nan)

    if delta_t != 0.0:
        longitude -= differentialRotation(delta_t, latitude)

    cos_latitude = math.cos(latitude)
    spherical = (
        cos_latitude * math.cos(longitude),
        cos_latitude * math.sin(longitude),
        math.sin(latitude),
    )

    sin_grid_latitude = -math.sin(grid[2])
    cos_grid_latitude = math.cos(grid[2])
    rotated_spherical = (
        cos_grid_latitude * spherical[0] + sin_grid_latitude * spherical[2],
        spherical[1],
        -sin_grid_latitude * spherical[0] + cos_grid_latitude * spherical[2],
    )
    if rotated_spherical[0] < 0.0:
        return (math.nan, math.nan)

    centered = apply_center(
        (rotated_spherical[1], rotated_spherical[2], 0.0),
        (meta.crval_internal_x, meta.crval_internal_y),
        crota_quaternion(meta),
    )
    texcoord = (
        (centered[0] - (-meta.crpix1_gl * meta.unit_per_pixel_x)) / (meta.pixel_width * meta.unit_per_pixel_x),
        (-centered[1] - (-meta.crpix2_gl * meta.unit_per_pixel_y)) / (meta.pixel_height * meta.unit_per_pixel_y),
    )
    return texcoord if texcoord_in_bounds(texcoord) else (math.nan, math.nan)


def sampleLatiTexcoord(
    scrpos: tuple[float, float],
    bounds_deg: tuple[float, float, float, float],
    meta: JHVMeta,
    image2d: np.ndarray,
    display_map_quat: tuple[float, float, float, float] = IDENTITY_QUAT,
    grid: tuple[float, float, float] | None = None,
    delta_t: float = 0.0,
) -> tuple[float, float]:
    if meta.projection == "CAR":
        return sampleLatiCarTexcoord(scrpos, bounds_deg, meta, image2d, display_map_quat)
    if meta.projection == "CEA":
        return sampleLatiCeaTexcoord(scrpos, bounds_deg, meta, image2d, display_map_quat)
    if grid is not None:
        return sampleLatiZenithalTexcoord(scrpos, bounds_deg, meta, image2d, grid, delta_t)
    raise ValueError("Latitudinal render mode currently mirrors CAR/CEA surface maps or requires an explicit zenithal latiGrid")


def renderLatitudinalTexcoords(
    scrpos: tuple[float, float],
    bounds_deg: tuple[float, float, float, float],
    meta: JHVMeta,
    image2d: np.ndarray,
    diff_meta: JHVMeta | None = None,
    diff_image2d: np.ndarray | None = None,
    display_map_quat: tuple[float, float, float, float] = IDENTITY_QUAT,
    diff_display_map_quat: tuple[float, float, float, float] = IDENTITY_QUAT,
    grid: tuple[float, float, float] | None = None,
    diff_grid: tuple[float, float, float] | None = None,
    delta_t: float = 0.0,
    diff_delta_t: float = 0.0,
) -> tuple[tuple[float, float], tuple[float, float]]:
    if DISPLAY_RADII[0] > 1.0:
        return (math.nan, math.nan), (math.nan, math.nan)
    clamped_scrpos = getScrPos(scrpos)
    if not math.isfinite(clamped_scrpos[0]) or not math.isfinite(clamped_scrpos[1]):
        return (math.nan, math.nan), (math.nan, math.nan)
    texcoord = sampleLatiTexcoord(clamped_scrpos, bounds_deg, meta, image2d, display_map_quat, grid, delta_t)
    if diff_meta is None:
        return texcoord, texcoord
    diff_texcoord = sampleLatiTexcoord(
        clamped_scrpos,
        bounds_deg,
        diff_meta,
        diff_image2d if diff_image2d is not None else image2d,
        diff_display_map_quat,
        diff_grid,
        diff_delta_t,
    )
    return texcoord, diff_texcoord


def renderLatitudinalPixel(
    scrpos: tuple[float, float],
    bounds_deg: tuple[float, float, float, float],
    meta: JHVMeta,
    image2d: np.ndarray,
    display_map_quat: tuple[float, float, float, float] = IDENTITY_QUAT,
    grid: tuple[float, float, float] | None = None,
    delta_t: float = 0.0,
) -> tuple[float, float]:
    texcoord, _ = renderLatitudinalTexcoords(
        scrpos,
        bounds_deg,
        meta,
        image2d,
        display_map_quat=display_map_quat,
        grid=grid,
        delta_t=delta_t,
    )
    return texcoord


# solarOrtho.frag mirror.

def ortho_screen_to_world(screen_xy: tuple[float, float]) -> tuple[float, float, float]:
    x, y = screen_xy
    radius2 = x * x + y * y
    return (x, y, math.sqrt(max(0.0, 1.0 - radius2)))


def texcoord_to_pixel_center(texcoord: tuple[float, float], width: int, height: int) -> tuple[float, float]:
    return (texcoord[0] * width, texcoord[1] * height)


# CPU mirror of the on-disk source sampling logic in solarOrtho.frag.
def sampleOrthoTexcoord(
    world_xyz: tuple[float, float, float],
    meta: JHVMeta,
    image2d: np.ndarray,
    simple_tan: bool = False,
) -> tuple[float, float]:
    if meta.projection == "CAR":
        plane = projectCarToWcsPlane(world_xyz, meta)
        return wcsPlaneToWrappedXTexcoord(plane, meta, image2d)
    if meta.projection == "CEA":
        plane = projectCeaToWcsPlane(world_xyz, meta)
        return wcsPlaneToWrappedXTexcoord(plane, meta, image2d)
    if simple_tan and meta.projection == "TAN":
        return wcsPlaneToTexcoord((world_xyz[0] - meta.crval_internal_x, world_xyz[1] - meta.crval_internal_y), meta, image2d)
    helioprojective = worldToHelioprojective(world_xyz, meta.observer_distance)
    plane = projectHelioprojectiveToWcsPlane(helioprojective, meta)
    return wcsPlaneToTexcoord(plane, meta, image2d)


def rotateOnDiskPoint(
    hit_point: tuple[float, float, float],
    meta: JHVMeta,
    camera_diff_quat: tuple[float, float, float, float] = IDENTITY_QUAT,
    delta_t: float = 0.0,
) -> tuple[float, float, float]:
    rotated = rotate_vector_inverse(camera_diff_quat, hit_point)
    if delta_t != 0.0:
        rotated = differential(delta_t, rotated)
    return rotated


def intersectPlane(camera_diff_quat: tuple[float, float, float, float], vecin: tuple[float, float, float], discard_back_facing: bool) -> float:
    altnormal = rotate_vector(camera_diff_quat, (0.0, 0.0, 1.0))
    if discard_back_facing and altnormal[2] <= 0.0:
        return math.nan
    if abs(altnormal[2]) < PLANE_Z_EPS:
        return math.nan
    return -(altnormal[0] * vecin[0] + altnormal[1] * vecin[1]) / altnormal[2]


def clipOrthoGeometry(sample_point: tuple[float, float, float]) -> bool:
    xy = (sample_point[0], sample_point[1])
    if not passes_sector(xy):
        return False
    radial2 = sample_point[0] * sample_point[0] + sample_point[1] * sample_point[1]
    return passes_radii(radial2) and passes_cutoff(xy)


def orthographic_vs_hpc_screen_pixel_centers(screen_xy: tuple[float, float], meta: JHVMeta, image2d: np.ndarray) -> tuple[tuple[float, float], tuple[float, float]]:
    ortho_texcoord, _ = renderOrthographicPixel(screen_xy, meta, image2d)
    ortho_px = texcoord_to_pixel_center(ortho_texcoord, meta.pixel_width, meta.pixel_height)

    solar_limb_angle_deg = math.degrees(math.atan2(1.0, meta.observer_distance))
    hpc_bounds_deg = (
        -solar_limb_angle_deg,
        solar_limb_angle_deg,
        -solar_limb_angle_deg,
        solar_limb_angle_deg,
    )
    hpc_scrpos = (
        0.5 * (screen_xy[0] + 1.0),
        0.5 * (screen_xy[1] + 1.0),
    )
    hpc_texcoord, _, _, _, _, _ = renderHpcTexcoords(hpc_scrpos, hpc_bounds_deg, meta, image2d)
    hpc_px = texcoord_to_pixel_center(hpc_texcoord, meta.pixel_width, meta.pixel_height)
    return ortho_px, hpc_px


def renderOrthographicTexcoords(
    screen_xy: tuple[float, float],
    meta: JHVMeta,
    image2d: np.ndarray,
    diff_meta: JHVMeta | None = None,
    diff_image2d: np.ndarray | None = None,
    simple_tan: bool = False,
    camera_diff_quat: tuple[float, float, float, float] = IDENTITY_QUAT,
    diff_camera_diff_quat: tuple[float, float, float, float] = IDENTITY_QUAT,
    source_view_quat: tuple[float, float, float, float] = IDENTITY_QUAT,
    diff_source_view_quat: tuple[float, float, float, float] = IDENTITY_QUAT,
) -> tuple[tuple[float, float], tuple[float, float], tuple[float, float, float], tuple[float, float, float]]:
    radius2 = screen_xy[0] * screen_xy[0] + screen_xy[1] * screen_xy[1]
    on_disk = radius2 <= 1.0
    surface_map_mode = meta.projection in SURFACE_MAP_PROJECTIONS
    diff_surface_map_mode = diff_meta.projection in SURFACE_MAP_PROJECTIONS if diff_meta is not None else False
    if surface_map_mode and not on_disk:
        return (math.nan, math.nan), (math.nan, math.nan), (math.nan, math.nan, math.nan), (math.nan, math.nan, math.nan)
    if diff_meta is not None and diff_surface_map_mode and not on_disk:
        return (math.nan, math.nan), (math.nan, math.nan), (math.nan, math.nan, math.nan), (math.nan, math.nan, math.nan)

    if on_disk:
        hit_point = ortho_screen_to_world(screen_xy)
        world_xyz = (
            rotate_vector_inverse(source_view_quat, hit_point)
            if surface_map_mode
            else rotateOnDiskPoint(hit_point, meta, camera_diff_quat)
        )
    else:
        hit_point = (math.nan, math.nan, math.nan)
        world_xyz = (0.0, 0.0, 0.0)

    if not surface_map_mode and world_xyz[2] <= 0.0:
        hit_point = (screen_xy[0], screen_xy[1], intersectPlane(camera_diff_quat, (screen_xy[0], screen_xy[1], 0.0), on_disk))
        world_xyz = rotate_vector_inverse(camera_diff_quat, hit_point)
        if on_disk and hit_point[2] < 0.0:
            return (math.nan, math.nan), (math.nan, math.nan), world_xyz, world_xyz
        if world_xyz[0] * world_xyz[0] + world_xyz[1] * world_xyz[1] + world_xyz[2] * world_xyz[2] <= 1.0:
            return (math.nan, math.nan), (math.nan, math.nan), world_xyz, world_xyz

    if not clipOrthoGeometry(world_xyz):
        return (math.nan, math.nan), (math.nan, math.nan), world_xyz, world_xyz

    texcoord = sampleOrthoTexcoord(world_xyz, meta, image2d, simple_tan=simple_tan)
    if diff_meta is None:
        return texcoord, texcoord, world_xyz, world_xyz

    if on_disk:
        diff_hit_point = ortho_screen_to_world(screen_xy)
        diff_world_xyz = (
            rotate_vector_inverse(diff_source_view_quat, diff_hit_point)
            if diff_surface_map_mode
            else rotateOnDiskPoint(diff_hit_point, diff_meta, diff_camera_diff_quat)
        )
    else:
        diff_hit_point = (math.nan, math.nan, math.nan)
        diff_world_xyz = (0.0, 0.0, 0.0)

    if not diff_surface_map_mode and diff_world_xyz[2] <= 0.0:
        diff_hit_point = (screen_xy[0], screen_xy[1], intersectPlane(diff_camera_diff_quat, (screen_xy[0], screen_xy[1], 0.0), on_disk))
        diff_world_xyz = rotate_vector_inverse(diff_camera_diff_quat, diff_hit_point)
        if on_disk and diff_hit_point[2] < 0.0:
            return (math.nan, math.nan), (math.nan, math.nan), world_xyz, diff_world_xyz
        if (
            diff_world_xyz[0] * diff_world_xyz[0] +
            diff_world_xyz[1] * diff_world_xyz[1] +
            diff_world_xyz[2] * diff_world_xyz[2]
        ) <= 1.0:
            return (math.nan, math.nan), (math.nan, math.nan), world_xyz, diff_world_xyz

    if not clipOrthoGeometry(diff_world_xyz):
        return (math.nan, math.nan), (math.nan, math.nan), world_xyz, diff_world_xyz

    diff_texcoord = sampleOrthoTexcoord(
        diff_world_xyz,
        diff_meta,
        diff_image2d if diff_image2d is not None else image2d,
        simple_tan=simple_tan,
    )
    return texcoord, diff_texcoord, world_xyz, diff_world_xyz


def renderOrthographicPixel(
    screen_xy: tuple[float, float],
    meta: JHVMeta,
    image2d: np.ndarray,
    simple_tan: bool = False,
    camera_diff_quat: tuple[float, float, float, float] = IDENTITY_QUAT,
    source_view_quat: tuple[float, float, float, float] = IDENTITY_QUAT,
) -> tuple[tuple[float, float], tuple[float, float, float]]:
    texcoord, _, world_xyz, _ = renderOrthographicTexcoords(
        screen_xy,
        meta,
        image2d,
        simple_tan=simple_tan,
        camera_diff_quat=camera_diff_quat,
        source_view_quat=source_view_quat,
    )
    return texcoord, world_xyz


# solarPolar.frag / solarLogPolar.frag mirror.

def texcoord_in_bounds(texcoord: tuple[float, float]) -> bool:
    return math.isfinite(texcoord[0]) and math.isfinite(texcoord[1]) and 0.0 <= texcoord[0] <= 1.0 and 0.0 <= texcoord[1] <= 1.0


def samplePolarTexcoord(
    scrpos: tuple[float, float],
    radial_coordinate: float,
    meta: JHVMeta,
    _image2d: np.ndarray,
) -> tuple[float, float]:
    if radial_coordinate > DISPLAY_RADII[1] or radial_coordinate < DISPLAY_RADII[0]:
        return (math.nan, math.nan)

    theta = -(scrpos[0] * 2.0 * math.pi + 0.5 * math.pi)
    polar_xy = (
        math.cos(theta) * radial_coordinate,
        math.sin(theta) * radial_coordinate,
    )
    if DISPLAY_CUTOFF[2] >= 0.0:
        display_xy = (polar_xy[1], polar_xy[0])
        cutoff_alt = (-DISPLAY_CUTOFF[1], DISPLAY_CUTOFF[0])
        geometry_flat_dist = abs(display_xy[0] * DISPLAY_CUTOFF[0] + display_xy[1] * DISPLAY_CUTOFF[1])
        geometry_flat_dist_alt = abs(display_xy[0] * cutoff_alt[0] + display_xy[1] * cutoff_alt[1])
        if geometry_flat_dist > DISPLAY_CUTOFF[2] or geometry_flat_dist_alt > DISPLAY_CUTOFF[2]:
            return (math.nan, math.nan)

    centered = apply_center(
        (polar_xy[0], -polar_xy[1], 0.0),
        (meta.crval_internal_x, meta.crval_internal_y),
        crota_quaternion(meta),
    )
    texcoord = (
        (centered[0] - (-meta.crpix1_gl * meta.unit_per_pixel_x)) / (meta.pixel_width * meta.unit_per_pixel_x),
        (-centered[1] - (-meta.crpix2_gl * meta.unit_per_pixel_y)) / (meta.pixel_height * meta.unit_per_pixel_y),
    )
    return texcoord if texcoord_in_bounds(texcoord) else (math.nan, math.nan)


def sampleLogPolarTexcoord(
    scrpos: tuple[float, float],
    radial_coordinate: float,
    meta: JHVMeta,
    image2d: np.ndarray,
) -> tuple[float, float]:
    return samplePolarTexcoord(scrpos, radial_coordinate, meta, image2d)


def renderPolarTexcoords(
    scrpos: tuple[float, float],
    radial_coordinate: float,
    meta: JHVMeta,
    image2d: np.ndarray,
    diff_meta: JHVMeta | None = None,
    diff_image2d: np.ndarray | None = None,
    logpolar: bool = False,
) -> tuple[tuple[float, float], tuple[float, float]]:
    clamped_scrpos = getScrPos(scrpos)
    if not math.isfinite(clamped_scrpos[0]) or not math.isfinite(clamped_scrpos[1]):
        return (math.nan, math.nan), (math.nan, math.nan)

    sample_texcoord = sampleLogPolarTexcoord if logpolar else samplePolarTexcoord
    texcoord = sample_texcoord(clamped_scrpos, radial_coordinate, meta, image2d)
    if diff_meta is None:
        return texcoord, texcoord

    diff_source = diff_image2d if diff_image2d is not None else image2d
    diff_texcoord = sample_texcoord(clamped_scrpos, radial_coordinate, diff_meta, diff_source)
    return texcoord, diff_texcoord


# Shared render utilities.

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


def save_png(path: Path, image: np.ndarray) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(normalize_image_for_png(image), mode="L").save(path)


def require_2d_image(image_data: np.ndarray, mode_name: str) -> None:
    if image_data.ndim != 2:
        raise ValueError(f"{mode_name} expects 2D image data, got shape {image_data.shape!r}")


def render_square_image(
    size: int,
    texcoord_at_pixel,
    sample_value,
) -> np.ndarray:
    image = np.full((size, size), np.nan, dtype=np.float64)
    for iy in range(size):
        sy = 1.0 - (iy / (size - 1) if size > 1 else 0.5)
        for ix in range(size):
            sx = ix / (size - 1) if size > 1 else 0.5
            image[iy, ix] = sample_value(texcoord_at_pixel(sx, sy))
    return image


def render_square_signed_image(
    size: int,
    texcoord_at_pixel,
    sample_value,
) -> np.ndarray:
    image = np.full((size, size), np.nan, dtype=np.float64)
    for iy in range(size):
        sy = 1.0 - 2.0 * (iy / (size - 1) if size > 1 else 0.5)
        for ix in range(size):
            sx = -1.0 + 2.0 * (ix / (size - 1) if size > 1 else 0.5)
            image[iy, ix] = sample_value(texcoord_at_pixel(sx, sy))
    return image


def evaluate_diff_selfcheck(
    size: int,
    screen_positions,
    texcoords_at_pixel,
    sample_value,
) -> tuple[np.ndarray, np.ndarray, float, float]:
    base_img = np.full((size, size), np.nan, dtype=np.float64)
    diff_img = np.full((size, size), np.nan, dtype=np.float64)
    max_texcoord_err = 0.0
    max_sample_err = 0.0

    for iy, ix, screen_pos in screen_positions(size):
        texcoord, diff_texcoord = texcoords_at_pixel(screen_pos)
        base_sample = sample_value(texcoord)
        diff_sample = sample_value(diff_texcoord)
        base_img[iy, ix] = base_sample
        err = abs(base_sample - diff_sample) if math.isfinite(base_sample) and math.isfinite(diff_sample) else math.nan
        diff_img[iy, ix] = err
        if math.isfinite(texcoord[0]) and math.isfinite(texcoord[1]) and math.isfinite(diff_texcoord[0]) and math.isfinite(diff_texcoord[1]):
            max_texcoord_err = max(
                max_texcoord_err,
                max(abs(texcoord[0] - diff_texcoord[0]), abs(texcoord[1] - diff_texcoord[1])),
            )
        if math.isfinite(err):
            max_sample_err = max(max_sample_err, err)

    return base_img, diff_img, max_texcoord_err, max_sample_err


def square_screen_positions(size: int):
    for iy in range(size):
        sy = 1.0 - (iy / (size - 1) if size > 1 else 0.5)
        for ix in range(size):
            sx = ix / (size - 1) if size > 1 else 0.5
            yield iy, ix, (sx, sy)


def signed_square_screen_positions(size: int):
    for iy in range(size):
        sy = 1.0 - 2.0 * (iy / (size - 1) if size > 1 else 0.5)
        for ix in range(size):
            sx = -1.0 + 2.0 * (ix / (size - 1) if size > 1 else 0.5)
            yield iy, ix, (sx, sy)


def is_surface_map_projection(meta: JHVMeta) -> bool:
    return meta.projection in SURFACE_MAP_PROJECTIONS


def render_surface_latitudinal_image(size: int, meta: JHVMeta, image2d: np.ndarray) -> np.ndarray:
    return render_square_image(
        size,
        lambda sx, sy: renderLatitudinalPixel((sx, sy), LATI_SURFACE_BOUNDS_DEG, meta, image2d),
        lambda texcoord: sample_texture_linear(image2d, texcoord, wrap_x=True),
    )


def render_zenithal_latitudinal_image(
    size: int,
    meta: JHVMeta,
    image2d: np.ndarray,
    grid: tuple[float, float, float] = (0.0, 0.0, 0.0),
    delta_t: float = 0.0,
) -> np.ndarray:
    return render_square_image(
        size,
        lambda sx, sy: renderLatitudinalPixel((sx, sy), LATI_ZENITHAL_BOUNDS_DEG, meta, image2d, grid=grid, delta_t=delta_t),
        lambda texcoord: sample_texture_linear(image2d, texcoord),
    )


def render_polar_mode_image(size: int, meta: JHVMeta, image2d: np.ndarray, logpolar: bool) -> np.ndarray:
    if logpolar:
        radial_start = math.log(LOGPOLAR_MIN_RADIUS)
        radial_stop = math.log(max(LOGPOLAR_MIN_RADIUS, 1.0))
    else:
        radial_start = 0.0
        radial_stop = 1.0

    def polar_texcoord(sx: float, sy: float) -> tuple[float, float]:
        radial_coordinate = math.exp(radial_start + sy * (radial_stop - radial_start)) if logpolar else radial_start + sy * (radial_stop - radial_start)
        if radial_coordinate > 1.0 or radial_coordinate < 0.0:
            return (math.nan, math.nan)
        return renderPolarTexcoords((sx, sy), radial_coordinate, meta, image2d, logpolar=logpolar)[0]

    return render_square_image(
        size,
        polar_texcoord,
        lambda texcoord: sample_texture_linear(image2d, texcoord),
    )


def render_orthographic_image(size: int, meta: JHVMeta, image2d: np.ndarray) -> np.ndarray:
    return render_square_signed_image(
        size,
        lambda sx, sy: renderOrthographicPixel((sx, sy), meta, image2d)[0],
        lambda texcoord: sample_texture_linear(image2d, texcoord, wrap_x=is_surface_map_projection(meta)),
    )


# Validator mode runners.

def run_hpc_bounds_compare(fits_file: Path, meta: JHVMeta) -> int:
    raw_bounds_deg = raw_hpc_footprint_bounds_degrees(meta)
    centered_bounds_deg = hpc_bounds_degrees(meta, 1.0)
    print(f"file={fits_file}")
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


def run_latitudinal_render(fits_file: Path, output_dir: Path, render_size: int, meta: JHVMeta, image_data: np.ndarray) -> int:
    require_2d_image(image_data, "Latitudinal render")
    if not is_surface_map_projection(meta):
        raise ValueError("Latitudinal render mode currently supports CAR/CEA surface maps only")

    jhv_img = render_surface_latitudinal_image(render_size, meta, image_data)
    jhv_path = output_dir / f"{fits_file.stem}_lati_jhv.png"
    save_png(jhv_path, jhv_img)

    print(f"file={fits_file}")
    print(f"mode=latitudinal_render size={render_size}")
    print(f"bounds_deg=({LATI_SURFACE_BOUNDS_DEG[0]:.12f}, {LATI_SURFACE_BOUNDS_DEG[1]:.12f}, {LATI_SURFACE_BOUNDS_DEG[2]:.12f}, {LATI_SURFACE_BOUNDS_DEG[3]:.12f})")
    print(f"jhv_png={jhv_path}")
    return 0


def run_latitudinal_zenithal_render(fits_file: Path, output_dir: Path, render_size: int, meta: JHVMeta, image_data: np.ndarray) -> int:
    require_2d_image(image_data, "Latitudinal zenithal render")
    if is_surface_map_projection(meta):
        raise ValueError("Latitudinal zenithal render is for the legacy zenithal path, not CAR/CEA surface maps")

    grid = (0.0, 0.0, 0.0)
    jhv_img = render_zenithal_latitudinal_image(render_size, meta, image_data, grid=grid)
    jhv_path = output_dir / f"{fits_file.stem}_lati_zenithal_jhv.png"
    save_png(jhv_path, jhv_img)

    print(f"file={fits_file}")
    print(f"mode=latitudinal_zenithal_render size={render_size}")
    print(f"default_lati_grid=({grid[0]:.12f}, {grid[1]:.12f}, {grid[2]:.12f})")
    print(f"jhv_png={jhv_path}")
    return 0


def run_polar_render(fits_file: Path, output_dir: Path, render_size: int, meta: JHVMeta, image_data: np.ndarray, logpolar: bool) -> int:
    require_2d_image(image_data, "Polar render")

    mode_name = "logpolar_render" if logpolar else "polar_render"
    suffix = "logpolar_jhv" if logpolar else "polar_jhv"
    jhv_img = render_polar_mode_image(render_size, meta, image_data, logpolar=logpolar)
    jhv_path = output_dir / f"{fits_file.stem}_{suffix}.png"
    save_png(jhv_path, jhv_img)

    print(f"file={fits_file}")
    print(f"mode={mode_name} size={render_size}")
    print(f"jhv_png={jhv_path}")
    return 0


def run_orthographic_render(fits_file: Path, output_dir: Path, render_size: int, meta: JHVMeta, image_data: np.ndarray) -> int:
    require_2d_image(image_data, "Orthographic render")

    jhv_img = render_orthographic_image(render_size, meta, image_data)
    jhv_path = output_dir / f"{fits_file.stem}_ortho_jhv.png"
    save_png(jhv_path, jhv_img)

    print(f"file={fits_file}")
    print(f"mode=orthographic_render size={render_size}")
    print(f"jhv_png={jhv_path}")
    return 0


def run_hpc_diff_selfcheck(
    fits_file: Path,
    output_dir: Path,
    render_size: int,
    meta: JHVMeta,
    image_data: np.ndarray,
) -> int:
    require_2d_image(image_data, "HPC diff selfcheck")

    bounds_deg = hpc_bounds_degrees(meta, 1.0)
    base_img, diff_img, max_texcoord_err, max_sample_err = evaluate_diff_selfcheck(
        render_size,
        square_screen_positions,
        lambda scrpos: renderHpcTexcoords(
                scrpos,
                bounds_deg,
                meta,
                image_data,
                diff_meta=meta,
                diff_image2d=image_data,
            )[:2],
        lambda texcoord: sample_texture_linear(image_data, texcoord),
    )

    base_path = output_dir / f"{fits_file.stem}_hpc_diff_self_base.png"
    diff_path = output_dir / f"{fits_file.stem}_hpc_diff_self_diff.png"
    save_png(base_path, base_img)
    save_png(diff_path, diff_img)

    print(f"file={fits_file}")
    print(f"mode=hpc_diff_selfcheck size={render_size}")
    print(f"texcoord_max_abs_error={max_texcoord_err:.6e}")
    print(f"sample_max_abs_error={max_sample_err:.6e}")
    print(f"base_png={base_path}")
    print(f"diff_png={diff_path}")
    return 0


def run_latitudinal_diff_selfcheck(
    fits_file: Path,
    output_dir: Path,
    render_size: int,
    meta: JHVMeta,
    image_data: np.ndarray,
) -> int:
    require_2d_image(image_data, "Latitudinal diff selfcheck")
    if not is_surface_map_projection(meta):
        raise ValueError("Latitudinal diff selfcheck currently supports CAR/CEA surface maps only")

    base_img, diff_img, max_texcoord_err, max_sample_err = evaluate_diff_selfcheck(
        render_size,
        square_screen_positions,
        lambda scrpos: renderLatitudinalTexcoords(
                scrpos,
                LATI_SURFACE_BOUNDS_DEG,
                meta,
                image_data,
                diff_meta=meta,
                diff_image2d=image_data,
            ),
        lambda texcoord: sample_texture_linear(image_data, texcoord, wrap_x=True),
    )

    base_path = output_dir / f"{fits_file.stem}_lati_diff_self_base.png"
    diff_path = output_dir / f"{fits_file.stem}_lati_diff_self_diff.png"
    save_png(base_path, base_img)
    save_png(diff_path, diff_img)

    print(f"file={fits_file}")
    print(f"mode=latitudinal_diff_selfcheck size={render_size}")
    print(f"texcoord_max_abs_error={max_texcoord_err:.6e}")
    print(f"sample_max_abs_error={max_sample_err:.6e}")
    print(f"base_png={base_path}")
    print(f"diff_png={diff_path}")
    return 0


def run_orthographic_diff_selfcheck(
    fits_file: Path,
    output_dir: Path,
    render_size: int,
    meta: JHVMeta,
    image_data: np.ndarray,
) -> int:
    require_2d_image(image_data, "Orthographic diff selfcheck")

    base_img, diff_img, max_texcoord_err, max_sample_err = evaluate_diff_selfcheck(
        render_size,
        signed_square_screen_positions,
        lambda screen_xy: renderOrthographicTexcoords(
                screen_xy,
                meta,
                image_data,
                diff_meta=meta,
                diff_image2d=image_data,
            )[:2],
        lambda texcoord: sample_texture_linear(image_data, texcoord, wrap_x=is_surface_map_projection(meta)),
    )

    base_path = output_dir / f"{fits_file.stem}_ortho_diff_self_base.png"
    diff_path = output_dir / f"{fits_file.stem}_ortho_diff_self_diff.png"
    save_png(base_path, base_img)
    save_png(diff_path, diff_img)

    print(f"file={fits_file}")
    print(f"mode=orthographic_diff_selfcheck size={render_size}")
    print(f"texcoord_max_abs_error={max_texcoord_err:.6e}")
    print(f"sample_max_abs_error={max_sample_err:.6e}")
    print(f"base_png={base_path}")
    print(f"diff_png={diff_path}")
    return 0


def run_polar_diff_selfcheck(
    fits_file: Path,
    output_dir: Path,
    render_size: int,
    meta: JHVMeta,
    image_data: np.ndarray,
    logpolar: bool,
) -> int:
    require_2d_image(image_data, "Polar diff selfcheck")

    mode_name = "logpolar_diff_selfcheck" if logpolar else "polar_diff_selfcheck"
    prefix = "logpolar" if logpolar else "polar"
    if logpolar:
        radial_start = math.log(LOGPOLAR_MIN_RADIUS)
        radial_stop = math.log(max(LOGPOLAR_MIN_RADIUS, 1.0))
    else:
        radial_start = 0.0
        radial_stop = 1.0

    def polar_diff_texcoords(scrpos: tuple[float, float]) -> tuple[tuple[float, float], tuple[float, float]]:
        radial_coordinate = math.exp(radial_start + scrpos[1] * (radial_stop - radial_start)) if logpolar else radial_start + scrpos[1] * (radial_stop - radial_start)
        if radial_coordinate > 1.0 or radial_coordinate < 0.0:
            nan2 = (math.nan, math.nan)
            return nan2, nan2
        return renderPolarTexcoords(
            scrpos,
            radial_coordinate,
            meta,
            image_data,
            diff_meta=meta,
            diff_image2d=image_data,
            logpolar=logpolar,
        )

    base_img, diff_img, max_texcoord_err, max_sample_err = evaluate_diff_selfcheck(
        render_size,
        square_screen_positions,
        polar_diff_texcoords,
        lambda texcoord: sample_texture_linear(image_data, texcoord),
    )

    base_path = output_dir / f"{fits_file.stem}_{prefix}_diff_self_base.png"
    diff_path = output_dir / f"{fits_file.stem}_{prefix}_diff_self_diff.png"
    save_png(base_path, base_img)
    save_png(diff_path, diff_img)

    print(f"file={fits_file}")
    print(f"mode={mode_name} size={render_size}")
    print(f"texcoord_max_abs_error={max_texcoord_err:.6e}")
    print(f"sample_max_abs_error={max_sample_err:.6e}")
    print(f"base_png={base_path}")
    print(f"diff_png={diff_path}")
    return 0


def run_hpc_render_compare(
    fits_file: Path,
    output_dir: Path,
    render_size: int,
    meta: JHVMeta,
    image_data: np.ndarray,
    pixel_wcs: WCS,
) -> int:
    require_2d_image(image_data, "HPC render compare")

    raw_bounds_deg = raw_hpc_footprint_bounds_degrees(meta)
    bounds_deg = hpc_bounds_degrees(meta, 1.0)
    jhv_img = np.full((render_size, render_size), np.nan, dtype=np.float64)
    astro_img = np.full((render_size, render_size), np.nan, dtype=np.float64)
    diff_px = np.full((render_size, render_size), np.nan, dtype=np.float64)
    max_px_err = 0.0
    sum_px_err2 = 0.0
    count = 0

    for iy in range(render_size):
        sy = 1.0 - (iy / (render_size - 1) if render_size > 1 else 0.5)
        for ix in range(render_size):
            sx = ix / (render_size - 1) if render_size > 1 else 0.5
            texcoord, _, _, _, helioprojective, _ = renderHpcTexcoords((sx, sy), bounds_deg, meta, image_data)
            if not math.isfinite(texcoord[0]) or not math.isfinite(texcoord[1]):
                diff_px[iy, ix] = math.nan
                jhv_img[iy, ix] = math.nan
                astro_img[iy, ix] = math.nan
                continue

            world_deg = [math.degrees(helioprojective[0]), math.degrees(helioprojective[1])]
            try:
                jhv_px = texcoord_to_pixel_center(texcoord, meta.pixel_width, meta.pixel_height)
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

            jhv_img[iy, ix] = sample_texture_linear(image_data, texcoord)
            astro_img[iy, ix] = sample_source_linear(image_data, astro_px[0], astro_px[1], meta)

    intensity_diff = np.abs(jhv_img - astro_img)
    jhv_path = output_dir / f"{fits_file.stem}_hpc_jhv.png"
    astro_path = output_dir / f"{fits_file.stem}_hpc_astropy.png"
    diff_path = output_dir / f"{fits_file.stem}_hpc_diff.png"
    save_png(jhv_path, jhv_img)
    save_png(astro_path, astro_img)
    save_png(diff_path, intensity_diff)

    print(f"file={fits_file}")
    print(f"mode=hpc_render_compare size={render_size}")
    print(f"raw_bounds_deg=({raw_bounds_deg[0]:.12f}, {raw_bounds_deg[1]:.12f}, {raw_bounds_deg[2]:.12f}, {raw_bounds_deg[3]:.12f})")
    print(f"bounds_deg=({bounds_deg[0]:.12f}, {bounds_deg[1]:.12f}, {bounds_deg[2]:.12f}, {bounds_deg[3]:.12f})")
    print(f"pixel_center_max_error_px={max_px_err:.6e}")
    print(f"pixel_center_rms_error_px={math.sqrt(sum_px_err2 / count):.6e}" if count > 0 else "pixel_center_rms_error_px=nan")
    print(f"jhv_png={jhv_path}")
    print(f"astropy_png={astro_path}")
    print(f"diff_png={diff_path}")
    return 0


def run_ortho_vs_hpc_screen_compare(
    fits_file: Path,
    output_dir: Path,
    render_size: int,
    meta: JHVMeta,
    image_data: np.ndarray,
) -> int:
    require_2d_image(image_data, "Ortho/HPC screen compare")

    ortho_img = np.full((render_size, render_size), np.nan, dtype=np.float64)
    hpc_img = np.full((render_size, render_size), np.nan, dtype=np.float64)
    diff_px = np.full((render_size, render_size), np.nan, dtype=np.float64)
    max_px_err = 0.0
    sum_px_err2 = 0.0
    count = 0

    for iy in range(render_size):
        sy = 1.0 - 2.0 * (iy / (render_size - 1) if render_size > 1 else 0.5)
        for ix in range(render_size):
            sx = -1.0 + 2.0 * (ix / (render_size - 1) if render_size > 1 else 0.5)
            ortho_px, hpc_px = orthographic_vs_hpc_screen_pixel_centers((sx, sy), meta, image_data)
            if not (
                math.isfinite(ortho_px[0]) and math.isfinite(ortho_px[1]) and
                math.isfinite(hpc_px[0]) and math.isfinite(hpc_px[1])
            ):
                diff_px[iy, ix] = math.nan
                ortho_img[iy, ix] = math.nan
                hpc_img[iy, ix] = math.nan
                continue
            err = max(abs(ortho_px[0] - hpc_px[0]), abs(ortho_px[1] - hpc_px[1]))
            diff_px[iy, ix] = err
            max_px_err = max(max_px_err, err)
            sum_px_err2 += err * err
            count += 1

            ortho_img[iy, ix] = sample_source_linear(image_data, ortho_px[0], ortho_px[1], meta)
            hpc_img[iy, ix] = sample_source_linear(image_data, hpc_px[0], hpc_px[1], meta)

    intensity_diff = np.abs(ortho_img - hpc_img)
    ortho_path = output_dir / f"{fits_file.stem}_ortho_screen.png"
    hpc_path = output_dir / f"{fits_file.stem}_hpc_screen.png"
    diff_path = output_dir / f"{fits_file.stem}_ortho_vs_hpc_diff.png"
    save_png(ortho_path, ortho_img)
    save_png(hpc_path, hpc_img)
    save_png(diff_path, intensity_diff)

    print(f"file={fits_file}")
    print(f"mode=ortho_vs_hpc_screen_compare size={render_size}")
    print(f"observer_distance={meta.observer_distance:.12f}")
    print(f"solar_limb_angle_deg={math.degrees(math.atan2(1.0, meta.observer_distance)):.12f}")
    print(f"pixel_center_max_error_px={max_px_err:.6e}")
    print(f"pixel_center_rms_error_px={math.sqrt(sum_px_err2 / count):.6e}" if count > 0 else "pixel_center_rms_error_px=nan")
    print(f"ortho_png={ortho_path}")
    print(f"hpc_png={hpc_path}")
    print(f"diff_png={diff_path}")
    return 0


def run_compare_initial_tan_image_frame(
    fits_file: Path,
    output_dir: Path,
    meta: JHVMeta,
    image_data: np.ndarray,
    pixel_wcs: WCS,
) -> int:
    require_2d_image(image_data, "TAN implementation comparison")
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
        screen_xy = np.column_stack((world_xyz[:, 0], world_xyz[:, 1]))
        new_px = np.array([
            texcoord_to_pixel_center(
                renderOrthographicTexcoords((float(sx), float(sy)), meta, image_data, simple_tan=False)[0],
                meta.pixel_width,
                meta.pixel_height,
            )
            for sx, sy in screen_xy
        ], dtype=np.float64)
        old_px = np.array([
            texcoord_to_pixel_center(
                renderOrthographicTexcoords((float(sx), float(sy)), meta, image_data, simple_tan=True)[0],
                meta.pixel_width,
                meta.pixel_height,
            )
            for sx, sy in screen_xy
        ], dtype=np.float64)

        finite_mask = (
            np.isfinite(old_px[:, 0]) & np.isfinite(old_px[:, 1]) &
            np.isfinite(new_px[:, 0]) & np.isfinite(new_px[:, 1])
        )
        if not np.any(finite_mask):
            continue

        astro_px = astro_px[finite_mask]
        old_px = old_px[finite_mask]
        new_px = new_px[finite_mask]
        row_indices = np.nonzero(finite_mask)[0]

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

        old_samples = np.array([sample_source_linear(image_data, px, py, meta) for px, py in old_px], dtype=np.float64)
        new_samples = np.array([sample_source_linear(image_data, px, py, meta) for px, py in new_px], dtype=np.float64)
        diff_samples = np.abs(old_samples - new_samples)
        old_img[iy, row_indices] = old_samples
        new_img[iy, row_indices] = new_samples
        diff_img[iy, row_indices] = diff_samples

    suffix = "_image_frame"
    old_path = output_dir / f"{fits_file.stem}_initial_tan{suffix}.png"
    new_path = output_dir / f"{fits_file.stem}_formal_tan{suffix}.png"
    diff_path = output_dir / f"{fits_file.stem}_initial_vs_formal_tan{suffix}_diff.png"
    save_png(old_path, old_img)
    save_png(new_path, new_img)
    save_png(diff_path, diff_img)

    print(f"file={fits_file}")
    print(f"mode=compare_initial_tan_image_frame size={size}")
    print("domain=image_frame")
    print(f"observer_distance={meta.observer_distance:.12f}")
    print(f"samples={count}")
    print(f"old_max_px_vs_astropy={max_old:.6e}")
    print(f"old_rms_px_vs_astropy={math.sqrt(sum_old2 / count):.6e}" if count > 0 else "old_rms_px_vs_astropy=nan")
    print(f"new_max_px_vs_astropy={max_new:.6e}")
    print(f"new_rms_px_vs_astropy={math.sqrt(sum_new2 / count):.6e}" if count > 0 else "new_rms_px_vs_astropy=nan")
    print(f"old_new_max_px={max_old_new:.6e}")
    print(f"old_new_rms_px={math.sqrt(sum_old_new2 / count):.6e}" if count > 0 else "old_new_rms_px=nan")
    print(f"initial_tan_png={old_path}")
    print(f"formal_tan_png={new_path}")
    print(f"initial_vs_formal_diff_png={diff_path}")
    return 0


def run_compare_initial_tan_vs_hpc(
    fits_file: Path,
    output_dir: Path,
    meta: JHVMeta,
    image_data: np.ndarray,
) -> int:
    require_2d_image(image_data, "--compare-initial-tan-vs-hpc")
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
        old_px = simple_tan_world_array_to_pixel_center(world_xyz, meta)
        hpc_world_deg = np.rad2deg(np.column_stack((
            xs * solar_limb_angle,
            np.full(xs.shape, y * solar_limb_angle, dtype=np.float64),
        )))
        hpc_px = mirrored_world_array_to_pixel_center(hpc_world_deg, meta)

        finite_mask = (
            np.isfinite(old_px[:, 0]) & np.isfinite(old_px[:, 1]) &
            np.isfinite(hpc_px[:, 0]) & np.isfinite(hpc_px[:, 1])
        )
        if not np.any(finite_mask):
            continue

        valid_indices = valid_indices[finite_mask]
        old_px = old_px[finite_mask]
        hpc_px = hpc_px[finite_mask]

        px_err = np.maximum(np.abs(old_px[:, 0] - hpc_px[:, 0]), np.abs(old_px[:, 1] - hpc_px[:, 1]))
        max_px_err = max(max_px_err, float(np.max(px_err)))
        sum_px_err2 += float(np.sum(px_err * px_err))
        count += int(px_err.size)

        old_samples = np.array([sample_source_linear(image_data, px, py, meta) for px, py in old_px], dtype=np.float64)
        hpc_samples = np.array([sample_source_linear(image_data, px, py, meta) for px, py in hpc_px], dtype=np.float64)
        diff_samples = np.abs(old_samples - hpc_samples)
        old_img[iy, valid_indices] = old_samples
        hpc_img[iy, valid_indices] = hpc_samples
        diff_img[iy, valid_indices] = diff_samples

    old_path = output_dir / f"{fits_file.stem}_initial_tan_screen.png"
    hpc_path = output_dir / f"{fits_file.stem}_hpc_screen_from_initial_tan_compare.png"
    diff_path = output_dir / f"{fits_file.stem}_initial_tan_vs_hpc_diff.png"
    save_png(old_path, old_img)
    save_png(hpc_path, hpc_img)
    save_png(diff_path, diff_img)

    print(f"file={fits_file}")
    print(f"mode=compare_initial_tan_vs_hpc size={size}")
    print(f"observer_distance={meta.observer_distance:.12f}")
    print(f"samples={count}")
    print(f"initial_tan_vs_hpc_max_px={max_px_err:.6e}")
    print(f"initial_tan_vs_hpc_rms_px={math.sqrt(sum_px_err2 / count):.6e}" if count > 0 else "initial_tan_vs_hpc_rms_px=nan")
    print(f"initial_tan_screen_png={old_path}")
    print(f"hpc_screen_png={hpc_path}")
    print(f"initial_tan_vs_hpc_diff_png={diff_path}")
    return 0


def run_inverse_validation(
    fits_file: Path,
    projection_wcs: WCS,
    pixel_wcs: WCS,
    meta: JHVMeta,
    samples: int,
    seed: int,
    report_worst: int,
    inverse_tan: bool,
    inverse_azp: bool,
    inverse_zpn: bool,
    inverse_car: bool,
    inverse_cea: bool,
) -> int:
    inverse_mode_count = sum(1 for enabled in (inverse_tan, inverse_azp, inverse_zpn, inverse_car, inverse_cea) if enabled)
    if inverse_mode_count > 1:
        raise ValueError("Choose at most one of --inverse-tan, --inverse-azp, --inverse-zpn, --inverse-car, or --inverse-cea")

    if inverse_tan:
        expected_projection = "TAN"
        mode_name = "inverse_tan"
    elif inverse_azp:
        expected_projection = "AZP"
        mode_name = "inverse_azp"
    elif inverse_zpn:
        expected_projection = "ZPN"
        mode_name = "inverse_zpn"
    elif inverse_car:
        expected_projection = "CAR"
        mode_name = "inverse_car"
    else:
        expected_projection = "CEA"
        mode_name = "inverse_cea"

    if meta.projection != expected_projection:
        raise ValueError(f"--{mode_name.replace('_', '-')} requires a {expected_projection} FITS file")

    inverse_err_max_deg = 0.0
    roundtrip_err_max_internal = 0.0
    valid_inverse_samples = 0
    skipped_inverse_samples = 0
    worst_inverse: list[tuple[float, tuple[float, float], tuple[float, float], tuple[float, float]]] = []
    if expected_projection in PIXEL_SAMPLED_FORWARD_PROJECTIONS:
        inverse_samples = sample_worlds_from_pixels(pixel_wcs, meta, samples)
    else:
        inverse_samples = [world2helioprojective(point_xyz, meta.observer_distance) for point_xyz in sample_points(samples, seed)]
    actual_sample_count = len(inverse_samples)

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
    print(f"file={fits_file}")
    print(f"mode={mode_name} samples={actual_sample_count} requested_samples={samples} seed={seed}")
    print(f"valid_samples={valid_inverse_samples}")
    print(f"skipped_samples={skipped_inverse_samples}")
    print(f"inverse_world_max_error_deg={inverse_err_max_deg:.6e}")
    print(f"roundtrip_plane_max_error_internal={roundtrip_err_max_internal:.6e}")
    print("worst_inverse_samples:")
    for inverse_err_deg, plane_internal, inverse_world_deg, astro_world_deg in worst_inverse[:report_worst]:
        print(
            f"  err={inverse_err_deg:.6e} plane_internal={plane_internal!r} "
            f"inverse={inverse_world_deg!r} astropy={astro_world_deg!r}"
        )
    return 0


def run_forward_validation(
    fits_file: Path,
    projection_wcs: WCS,
    pixel_wcs: WCS,
    meta: JHVMeta,
    samples: int,
    seed: int,
    report_worst: int,
    all_pixels: bool,
) -> int:
    proj_err_max = 0.0
    pixel_err_max = 0.0
    worst: list[tuple[float, str, tuple[float, float], tuple[float, float]]] = []

    if all_pixels:
        worst_pixels: list[tuple[float, tuple[int, int], tuple[float, float], tuple[float, float]]] = []
        for y in range(meta.pixel_height):
            fits_y = np.full(meta.pixel_width, y + 1.0, dtype=np.float64)
            fits_x = np.arange(meta.pixel_width, dtype=np.float64) + 1.0
            world = pixel_wcs.wcs_pix2world(np.column_stack((fits_x, fits_y)), 1)
            if not np.all(np.isfinite(world)):
                world = np.array([
                    pixel_center_to_world_deg((float(ax - 0.5), float(ay - 0.5)), meta)
                    if not np.all(np.isfinite(w))
                    else (float(w[0]), float(w[1]))
                    for w, ax, ay in zip(world, fits_x, fits_y, strict=True)
                ], dtype=np.float64)
            jhv_pixel_center = mirrored_world_array_to_pixel_center(world, meta)
            astro_pixel_center = np.column_stack((fits_x - 0.5, fits_y - 0.5))
            errors = np.array([
                pixel_center_error_px((float(jx), float(jy)), (float(ax), float(ay)), meta)
                for (jx, jy), (ax, ay) in zip(jhv_pixel_center, astro_pixel_center, strict=True)
            ], dtype=np.float64)
            row_max = float(np.max(errors))
            pixel_err_max = max(pixel_err_max, row_max)
            if report_worst > 0:
                idx = int(np.argmax(errors))
                worst_pixels.append((
                    float(errors[idx]),
                    (int(idx), y),
                    (float(jhv_pixel_center[idx, 0]), float(jhv_pixel_center[idx, 1])),
                    (float(astro_pixel_center[idx, 0]), float(astro_pixel_center[idx, 1])),
                ))

        worst_pixels.sort(key=lambda item: item[0], reverse=True)
        print(f"file={fits_file}")
        print("mode=all_pixels")
        print(f"pixel_center_max_error_px={pixel_err_max:.6e}")
        print("worst_pixels:")
        for pixel_err, pixel_xy, jhv_pixel_center, astro_pixel_center in worst_pixels[:report_worst]:
            print(
                f"  err={pixel_err:.6e} pixel={pixel_xy!r} "
                f"jhv={jhv_pixel_center!r} astropy={astro_pixel_center!r}"
            )
        return 0

    valid_samples = 0
    skipped_samples = 0
    if meta.projection in PIXEL_SAMPLED_FORWARD_PROJECTIONS:
        world_samples = sample_worlds_from_pixels(pixel_wcs, meta, samples)
    else:
        world_samples = [world2helioprojective(point_xyz, meta.observer_distance) for point_xyz in sample_points(samples, seed)]
    actual_sample_count = len(world_samples)

    for world_rad in world_samples:
        world_deg = [math.degrees(world_rad[0]), math.degrees(world_rad[1])]

        try:
            jhv_plane_internal = mirrored_world_to_plane_internal(world_rad, meta)
        except ValueError:
            skipped_samples += 1
            continue

        jhv_pixel_center = mirrored_world_to_pixel_center(world_rad, meta)

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
        pixel_err = pixel_center_error_px(jhv_pixel_center, astro_pixel_center, meta)
        pixel_err_max = max(pixel_err_max, pixel_err)

        worst.append((
            pixel_err,
            f"world_deg=({world_deg[0]:.12f}, {world_deg[1]:.12f})",
            jhv_pixel_center,
            astro_pixel_center,
        ))
        valid_samples += 1

    worst.sort(key=lambda item: item[0], reverse=True)

    print(f"file={fits_file}")
    print(f"samples={actual_sample_count} requested_samples={samples} seed={seed}")
    print(f"valid_samples={valid_samples}")
    print(f"skipped_samples={skipped_samples}")
    print(f"projection_max_error_internal={proj_err_max:.6e}")
    print(f"pixel_center_max_error_px={pixel_err_max:.6e}")
    print("worst_samples:")
    for pixel_err, sample_desc, jhv_pixel_center, astro_pixel_center in worst[:report_worst]:
        print(
            f"  err={pixel_err:.6e} {sample_desc} "
            f"jhv={jhv_pixel_center!r} astropy={astro_pixel_center!r}"
        )

    return 0


# CLI/bootstrap helpers.

def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Validate the current JHV image/WCS projection code paths against astropy.wcs. "
            "The script covers the formal TAN/AZP/ZPN/CAR/CEA image path, the inverse TAN/AZP/ZPN/CAR/CEA branches "
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
    parser.add_argument("--inverse-azp", action="store_true", help="Validate the AZP inverse plane->world mapping")
    parser.add_argument("--inverse-zpn", action="store_true", help="Validate the primary-branch ZPN inverse plane->world mapping")
    parser.add_argument("--inverse-car", action="store_true", help="Validate the CAR inverse plane->world mapping")
    parser.add_argument("--inverse-cea", action="store_true", help="Validate the CEA inverse plane->world mapping")
    parser.add_argument("--hpc-render-compare", action="store_true", help="Render a bounded HPC screen through JHV and Astropy mappings and write diagnostic PNGs")
    parser.add_argument("--hpc-bounds-compare", action="store_true", help="Report the raw and centered HPC bounds used by the current JHV display logic")
    parser.add_argument("--latitudinal-render", action="store_true", help="Render the CAR/CEA latitudinal surface-map path mirrored from solarLati.frag")
    parser.add_argument("--latitudinal-zenithal-render", action="store_true", help="Render the legacy zenithal latitudinal path mirrored from solarLati.frag")
    parser.add_argument("--polar-render", action="store_true", help="Render the polar path mirrored from solarPolar.frag")
    parser.add_argument("--logpolar-render", action="store_true", help="Render the log-polar path mirrored from solarLogPolar.frag")
    parser.add_argument("--orthographic-render", action="store_true", help="Render the orthographic path mirrored from solarOrtho.frag")
    parser.add_argument("--hpc-diff-selfcheck", action="store_true", help="Exercise the mirrored HPC diff branch with identical source/meta on both sides")
    parser.add_argument("--latitudinal-diff-selfcheck", action="store_true", help="Exercise the mirrored Latitudinal diff branch with identical source/meta on both sides")
    parser.add_argument("--orthographic-diff-selfcheck", action="store_true", help="Exercise the mirrored Orthographic diff branch with identical source/meta on both sides")
    parser.add_argument("--polar-diff-selfcheck", action="store_true", help="Exercise the mirrored Polar diff branch with identical source/meta on both sides")
    parser.add_argument("--logpolar-diff-selfcheck", action="store_true", help="Exercise the mirrored LogPolar diff branch with identical source/meta on both sides")
    parser.add_argument("--ortho-vs-hpc-screen-compare", action="store_true", help="Compare formal-TAN in Orthographic mode against JHV HPC over the full rendered comparison frame")
    parser.add_argument("--compare-initial-tan-image-frame", action="store_true", help="Compare simple-TAN against formal-TAN over the full image frame")
    parser.add_argument("--compare-initial-tan-vs-hpc", action="store_true", help="Compare simple-TAN against the JHV HPC display sampling over the full rendered comparison frame")
    parser.add_argument("--render-size", type=int, default=512, help="Square output size for HPC diagnostic renderings")
    parser.add_argument("--output-dir", type=Path, default=Path("extra/test/out"), help="Directory for diagnostic PNGs")
    return parser


def load_validation_context(
    fits_file: Path,
    hdu_index: int | None,
) -> tuple[np.ndarray, JHVMeta, WCS, WCS]:
    with fits.open(fits_file) as hdul:
        hdu = find_image_hdu(hdul, hdu_index)
        header = hdu.header
        image_data = np.squeeze(np.asarray(hdu.data, dtype=np.float64))

    ensure_supported_projection(header)
    meta = build_jhv_meta(header)
    projection_wcs = build_projection_only_wcs(header)
    pixel_wcs = build_jhv_equivalent_astropy_wcs(header, meta)
    return image_data, meta, projection_wcs, pixel_wcs


# Main CLI entry point.

def main() -> int:
    args = build_arg_parser().parse_args()

    image_data, meta, projection_wcs, pixel_wcs = load_validation_context(args.fits_file, args.hdu)

    if args.hpc_bounds_compare:
        return run_hpc_bounds_compare(args.fits_file, meta)

    if args.latitudinal_render:
        return run_latitudinal_render(args.fits_file, args.output_dir, args.render_size, meta, image_data)

    if args.latitudinal_zenithal_render:
        return run_latitudinal_zenithal_render(args.fits_file, args.output_dir, args.render_size, meta, image_data)

    if args.polar_render or args.logpolar_render:
        return run_polar_render(args.fits_file, args.output_dir, args.render_size, meta, image_data, logpolar=args.logpolar_render)

    if args.orthographic_render:
        return run_orthographic_render(args.fits_file, args.output_dir, args.render_size, meta, image_data)

    if args.hpc_diff_selfcheck:
        return run_hpc_diff_selfcheck(args.fits_file, args.output_dir, args.render_size, meta, image_data)

    if args.latitudinal_diff_selfcheck:
        return run_latitudinal_diff_selfcheck(args.fits_file, args.output_dir, args.render_size, meta, image_data)

    if args.orthographic_diff_selfcheck:
        return run_orthographic_diff_selfcheck(args.fits_file, args.output_dir, args.render_size, meta, image_data)

    if args.polar_diff_selfcheck or args.logpolar_diff_selfcheck:
        return run_polar_diff_selfcheck(
            args.fits_file,
            args.output_dir,
            args.render_size,
            meta,
            image_data,
            logpolar=args.logpolar_diff_selfcheck,
        )

    if args.hpc_render_compare:
        return run_hpc_render_compare(
            args.fits_file,
            args.output_dir,
            args.render_size,
            meta,
            image_data,
            pixel_wcs,
        )

    if args.ortho_vs_hpc_screen_compare:
        return run_ortho_vs_hpc_screen_compare(
            args.fits_file,
            args.output_dir,
            args.render_size,
            meta,
            image_data,
        )

    if args.compare_initial_tan_image_frame:
        return run_compare_initial_tan_image_frame(
            args.fits_file,
            args.output_dir,
            meta,
            image_data,
            pixel_wcs,
        )

    if args.compare_initial_tan_vs_hpc:
        return run_compare_initial_tan_vs_hpc(
            args.fits_file,
            args.output_dir,
            meta,
            image_data,
        )

    if args.inverse_tan or args.inverse_azp or args.inverse_zpn or args.inverse_car or args.inverse_cea:
        return run_inverse_validation(
            args.fits_file,
            projection_wcs,
            pixel_wcs,
            meta,
            args.samples,
            args.seed,
            args.report_worst,
            args.inverse_tan,
            args.inverse_azp,
            args.inverse_zpn,
            args.inverse_car,
            args.inverse_cea,
        )

    return run_forward_validation(
        args.fits_file,
        projection_wcs,
        pixel_wcs,
        meta,
        args.samples,
        args.seed,
        args.report_worst,
        args.all_pixels,
    )


if __name__ == "__main__":
    sys.exit(main())
