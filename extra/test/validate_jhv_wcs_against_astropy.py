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
    if not (ctype1.endswith("TAN") or ctype1.endswith("AZP")):
        raise ValueError(f"Only TAN and AZP FITS files are supported right now, got {ctype1!r} / {ctype2!r}")


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


def project_world_to_plane_internal(world_rad: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    if meta.projection == "TAN":
        return tan_world_to_plane_internal(world_rad, meta)
    if meta.projection == "AZP":
        return azp_world_to_plane_internal(world_rad, meta)
    raise ValueError(f"Unsupported projection {meta.projection!r}")


def project_plane_internal_to_world(plane_internal: tuple[float, float], meta: JHVMeta) -> tuple[float, float]:
    if meta.projection == "AZP":
        return azp_plane_internal_to_world(plane_internal, meta)
    raise ValueError(f"Inverse projection is unsupported for {meta.projection!r}")


def helioprojective_to_ray(world_rad: tuple[float, float]) -> np.ndarray:
    lon, lat = world_rad
    ray = np.array([
        math.tan(lon),
        math.tan(lat) / math.cos(lon),
        1.0,
    ], dtype=np.float64)
    return ray / np.linalg.norm(ray)


def observer_plane_point(world_rad: tuple[float, float], center_ray: np.ndarray, plane_distance: float) -> np.ndarray:
    ray = helioprojective_to_ray(world_rad)
    denom = float(np.dot(center_ray, ray))
    if denom <= 0.0:
        raise ValueError("Ray is outside the forward observer-plane hemisphere")
    return (plane_distance / denom) * ray


def observer_shell_point(world_rad: tuple[float, float], shell_radius: float) -> np.ndarray:
    return shell_radius * helioprojective_to_ray(world_rad)


def observer_hpc_reference_point(world_rad: tuple[float, float], observer_distance: float) -> np.ndarray:
    ray = helioprojective_to_ray(world_rad)
    if ray[2] <= 0.0:
        raise ValueError("Ray is outside the forward HPC reference plane")
    return (observer_distance / ray[2]) * ray


def angular_error_rad(point: np.ndarray, world_rad: tuple[float, float]) -> float:
    ray = helioprojective_to_ray(world_rad)
    point_dir = point / np.linalg.norm(point)
    dotp = float(np.clip(np.dot(point_dir, ray), -1.0, 1.0))
    return math.acos(dotp)


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
    if projection == "AZP":
        wcs.wcs.set_pv([(2, 1, float(header.get("PV2_1", 0.0))), (2, 2, float(header.get("PV2_2", 0.0)))])
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
    if projection == "AZP":
        wcs.wcs.set_pv([(2, 1, float(header.get("PV2_1", 0.0))), (2, 2, float(header.get("PV2_2", 0.0)))])
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
    parser = argparse.ArgumentParser(description="Validate the JHV WCS orthographic code path against astropy.wcs.")
    parser.add_argument("fits_file", type=Path)
    parser.add_argument("--hdu", type=int, default=None, help="Explicit FITS HDU index to use")
    parser.add_argument("--samples", type=int, default=1000, help="Number of random 3D samples")
    parser.add_argument("--seed", type=int, default=0, help="Random seed")
    parser.add_argument("--report-worst", type=int, default=5, help="How many worst samples to print")
    parser.add_argument("--all-pixels", action="store_true", help="Validate all pixel centers instead of random 3D samples")
    parser.add_argument("--inverse-azp", action="store_true", help="Validate the non-slanted AZP inverse plane->world mapping")
    parser.add_argument("--observer-plane-prototype", action="store_true", help="Prototype constant-plane observer embedding on the image grid")
    parser.add_argument("--plane-distance-scale", type=float, default=1.0, help="Scale factor k for plane distance L = k * observerDistance in observer-plane prototype mode")
    parser.add_argument("--observer-shell-prototype", action="store_true", help="Prototype constant-radius observer-shell embedding on the image grid")
    parser.add_argument("--shell-radius-scale", type=float, default=1.0, help="Scale factor k for shell radius S = k * observerDistance in observer-shell prototype mode")
    args = parser.parse_args()

    with fits.open(args.fits_file) as hdul:
        hdu = find_image_hdu(hdul, args.hdu)
        header = hdu.header

    ensure_supported_projection(header)
    meta = build_jhv_meta(header)
    projection_wcs = build_projection_only_wcs(header)
    pixel_wcs = build_jhv_equivalent_astropy_wcs(header, meta)

    if args.observer_plane_prototype:
        center_world_rad = (
            meta.crval_internal_x / meta.plane_units_per_rad,
            meta.crval_internal_y / meta.plane_units_per_rad,
        )
        center_ray = helioprojective_to_ray(center_world_rad)
        plane_distance = args.plane_distance_scale * meta.observer_distance

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

        for y in range(meta.pixel_height):
            fits_y = np.full(meta.pixel_width, y + 1.0, dtype=np.float64)
            fits_x = np.arange(meta.pixel_width, dtype=np.float64) + 1.0
            world = pixel_wcs.wcs_pix2world(np.column_stack((fits_x, fits_y)), 1)
            for x in range(meta.pixel_width):
                world_rad = (math.radians(float(world[x, 0])), math.radians(float(world[x, 1])))
                ray = helioprojective_to_ray(world_rad)
                denom = float(np.dot(center_ray, ray))
                if denom <= 0.0:
                    skipped_pixels += 1
                    continue
                point = (plane_distance / denom) * ray
                mins = np.minimum(mins, point)
                maxs = np.maximum(maxs, point)
                denom_min = min(denom_min, denom)
                denom_max = max(denom_max, denom)
                native_angle_err = angular_error_rad(point, world_rad)
                native_angle_err_max = max(native_angle_err_max, native_angle_err)
                native_angle_err_sum2 += native_angle_err * native_angle_err
                native_angle_err_count += 1

            if y in {0, meta.pixel_height // 2, meta.pixel_height - 1}:
                for px, py in sample_pixels:
                    if py != y:
                        continue
                    world_rad = (math.radians(float(world[px, 0])), math.radians(float(world[px, 1])))
                    try:
                        point = observer_plane_point(world_rad, center_ray, plane_distance)
                        embedded_points.append(((px, py), point))
                    except ValueError:
                        pass

        center_point = plane_distance * center_ray
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
        print("sample_points:")
        for (px, py), point in embedded_points:
            print(f"  pixel=({px}, {py}) point=({point[0]:.12e}, {point[1]:.12e}, {point[2]:.12e})")
        return 0

    if args.observer_shell_prototype:
        shell_radius = args.shell_radius_scale * meta.observer_distance
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

        for y in range(meta.pixel_height):
            fits_y = np.full(meta.pixel_width, y + 1.0, dtype=np.float64)
            fits_x = np.arange(meta.pixel_width, dtype=np.float64) + 1.0
            world = pixel_wcs.wcs_pix2world(np.column_stack((fits_x, fits_y)), 1)
            for x in range(meta.pixel_width):
                world_rad = (math.radians(float(world[x, 0])), math.radians(float(world[x, 1])))
                point = observer_shell_point(world_rad, shell_radius)
                mins = np.minimum(mins, point)
                maxs = np.maximum(maxs, point)
                native_angle_err = angular_error_rad(point, world_rad)
                native_angle_err_max = max(native_angle_err_max, native_angle_err)
                native_angle_err_sum2 += native_angle_err * native_angle_err
                native_angle_err_count += 1

            if y in {0, meta.pixel_height // 2, meta.pixel_height - 1}:
                for px, py in sample_pixels:
                    if py != y:
                        continue
                    world_rad = (math.radians(float(world[px, 0])), math.radians(float(world[px, 1])))
                    point = observer_shell_point(world_rad, shell_radius)
                    embedded_points.append(((px, py), point))

        print(f"file={args.fits_file}")
        print(f"mode=observer_shell_prototype shell_radius_scale={args.shell_radius_scale:.6f}")
        print(f"observer_distance={meta.observer_distance:.12f}")
        print(f"shell_radius={shell_radius:.12f}")
        print(f"bbox_min=({mins[0]:.12e}, {mins[1]:.12e}, {mins[2]:.12e})")
        print(f"bbox_max=({maxs[0]:.12e}, {maxs[1]:.12e}, {maxs[2]:.12e})")
        if native_angle_err_count > 0:
            print(f"native_ray_angle_max_error_rad={native_angle_err_max:.12e}")
            print(f"native_ray_angle_rms_error_rad={math.sqrt(native_angle_err_sum2 / native_angle_err_count):.12e}")
        print("sample_points:")
        for (px, py), point in embedded_points:
            print(f"  pixel=({px}, {py}) point=({point[0]:.12e}, {point[1]:.12e}, {point[2]:.12e})")
        return 0

    if args.inverse_azp:
        if meta.projection != "AZP":
            raise ValueError("--inverse-azp requires an AZP FITS file")

        inverse_err_max_deg = 0.0
        roundtrip_err_max_internal = 0.0
        worst_inverse: list[tuple[float, tuple[float, float], tuple[float, float], tuple[float, float]]] = []
        for point_xyz in sample_points(args.samples, args.seed):
            world_rad = world2helioprojective(point_xyz, meta.observer_distance)
            plane_internal = azp_world_to_plane_internal(world_rad, meta)

            inverse_world_rad = azp_plane_internal_to_world(plane_internal, meta)
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

            roundtrip_plane_internal = azp_world_to_plane_internal(inverse_world_rad, meta)
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

        worst_inverse.sort(key=lambda item: item[0], reverse=True)
        print(f"file={args.fits_file}")
        print(f"mode=inverse_azp samples={args.samples} seed={args.seed}")
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
    worst: list[tuple[float, tuple[float, float, float], tuple[float, float], tuple[float, float]]] = []

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

    for point_xyz in sample_points(args.samples, args.seed):
        world_rad = world2helioprojective(point_xyz, meta.observer_distance)
        world_deg = [math.degrees(world_rad[0]), math.degrees(world_rad[1])]

        jhv_plane_internal = project_world_to_plane_internal(world_rad, meta)
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
