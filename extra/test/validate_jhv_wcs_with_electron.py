#!/usr/bin/env python3

from __future__ import annotations

import argparse
import json
import math
import os
import subprocess
import sys
from pathlib import Path
from tempfile import TemporaryDirectory

import numpy as np

from validate_jhv_wcs_against_astropy import (
    LATI_ZENITHAL_BOUNDS_DEG,
    LATI_SURFACE_BOUNDS_DEG,
    clipHpcGeometry,
    displayLatitudinalWorld,
    hpc_bounds_degrees,
    is_surface_map_projection,
    load_validation_context,
    pixel_center_error_px,
    renderHpcTexcoordsFloat32,
    renderHpcTexcoords,
    renderLatitudinalTexcoords,
    renderOrthographicTexcoords,
    sampleHpcTexcoord,
    sample_texture_linear,
    texcoord_to_pixel_center,
    worldToHelioprojective,
    wcsRect,
    zpn_primary_branch_upper_eta,
)


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent.parent
RUNNER_DIR = SCRIPT_DIR / "electron_webgl_runner"
DEFAULT_ELECTRON = Path(os.environ.get(
    "JHV_ELECTRON",
    str(Path.home() / "electron-v42.1.0-darwin-arm64/Electron.app/Contents/MacOS/Electron"),
))
ALL_MODES = ("hpc", "ortho", "lati_zenithal", "radial_warp", "rect_warp")
WARP_MODES = ("radial_warp", "rect_warp")
WARP_OUTER_RADIUS = 4.0
WARP_LAMBDAS = (-1.0, -0.5, 0.0, 0.5, 1.0)
DEFAULT_WARP_LAMBDA = 0.0
HPC_PROJECTION_CASES = (
    ("arc_punch", "PUNCH_L3_CAM_20260425001600_v0k.fits", 1, 0.3),
    ("azp_hi1", "20250622_000831_s4h1A.fts", None, 0.2),
    ("azp_hi2", "20250622_000851_s4h2A.fts", None, 0.2),
    ("zpn_wispr_1211", "psp_L3_wispr_20231227T150508_V1_1211.fits", None, 0.2),
    ("zpn_wispr_2222", "psp_L3_wispr_20231227T150704_V1_2222.fits", None, 0.2),
)

PROJECTION_CODES = {
    "TAN": 0.0,
    "ARC": 1.0,
    "AZP": 2.0,
    "ZPN": 3.0,
    "CAR": 4.0,
    "CEA": 5.0,
}

HPC_RENDER_CASES = (
    ("tan_cor2", "20241224_194245_d4c2A.fts", None, 0.3),
    *HPC_PROJECTION_CASES,
)

SURFACE_MAP_CASES = (
    ("car_sunerf", "sunerf_map.fits"),
    ("car_hmi", "syn_HMI_hmi.m_720s_2026-02-25T00-00-00_a_V1.fits"),
    ("car_aia", "syn_AIA_171_2026-01-12T00-00-00_f_V3.fits"),
    ("cea", "mrzqs260301t2314c2308_169.fits"),
)

TAN_SCREEN_CASES = (
    ("sample171", "sample.171.fits", 1),
    ("solo_eui", "solo_L2_eui-fsi174-image_20251002T150055171_V00.fits", None),
    ("cor2", "20241224_194245_d4c2A.fts", None),
)


def crota_quat(crota_rad: float) -> list[float]:
    half = 0.5 * crota_rad
    return [0.0, 0.0, math.sin(half), math.cos(half)]


def run_electron(electron: Path, job_path: Path, backend: str) -> dict:
    env = os.environ.copy()
    env["JHV_ELECTRON_GL_BACKEND"] = backend
    completed = subprocess.run(
        [str(electron), str(RUNNER_DIR), str(job_path)],
        cwd=REPO_ROOT,
        env=env,
        capture_output=True,
        text=True,
    )

    result = None
    for line in completed.stdout.splitlines():
        line = line.strip()
        if not line.startswith("{"):
            continue
        try:
            parsed = json.loads(line)
        except json.JSONDecodeError:
            continue
        if "ok" in parsed:
            result = parsed

    if completed.returncode != 0 or result is None:
        raise RuntimeError(
            "Electron WebGL runner failed\n"
            f"returncode={completed.returncode}\n"
            f"stdout={completed.stdout}\n"
            f"stderr={completed.stderr}"
        )
    if not result.get("ok"):
        raise RuntimeError(
            "Electron WebGL runner reported failure\n"
            f"result={json.dumps(result, indent=2)}\n"
            f"stderr={completed.stderr}"
        )
    return result


def run_electron_jobs(electron: Path, jobs: list[dict], backend: str) -> list[dict]:
    with TemporaryDirectory() as temp_dir:
        job_path = Path(temp_dir) / "electron-jobs.json"
        job_path.write_text(json.dumps({"jobs": jobs}))
        result = run_electron(electron, job_path, backend)
    return result["results"]


def common_job(
    mode: str,
    fits_file: Path,
    render_size: int,
    output_dir: Path,
    meta,
    bounds: tuple[float, float, float, float],
    sample_texture: bool,
    backend: str,
    name: str | None = None,
    diff_selfcheck: bool = False,
    color_smoke: bool = False,
    color_diff_smoke: bool = False,
) -> dict:
    output_kind = "color_diff" if color_diff_smoke else "color" if color_smoke else "sample" if sample_texture else "texcoord"
    output_name = name if name is not None else mode
    output_path = output_dir / f"{fits_file.stem}_{backend}_{output_name}_{output_kind}.rgba32f"
    return {
        "mode": mode,
        "name": output_name,
        "repoRoot": str(REPO_ROOT),
        "width": render_size,
        "height": render_size,
        "outputPath": str(output_path),
        "boundsDeg": list(bounds),
        "warpLambda": 1.0,
        "rect": list(wcsRect(meta)),
        "crota": crota_quat(meta.crota_rad),
        "crval": [meta.crval_internal_x, meta.crval_internal_y],
        "zpnUpperEta": zpn_primary_branch_upper_eta(meta) if meta.projection == "ZPN" else 0.0,
        "projectionCode": PROJECTION_CODES[meta.projection],
        "planeUnitsPerRadian": meta.plane_units_per_rad,
        "observerDistance": meta.observer_distance,
        "pv2": list(meta.pv2),
        "sampleTexture": sample_texture,
        "diffSelfcheck": diff_selfcheck,
        "colorSmoke": color_smoke,
        "colorDiffSmoke": color_diff_smoke,
        "textureWidth": min(meta.pixel_width, 512),
        "textureHeight": min(meta.pixel_height, 512),
    }


def run_shader_job(electron: Path, backend: str, job: dict) -> tuple[dict, np.ndarray]:
    with TemporaryDirectory() as temp_dir:
        job_path = Path(temp_dir) / "electron-job.json"
        job_path.write_text(json.dumps(job))
        result = run_electron(electron, job_path, backend)

    pixels = np.fromfile(job["outputPath"], dtype=np.float32)
    expected = job["width"] * job["height"] * 4
    if pixels.size != expected:
        raise RuntimeError(f"Expected {expected} float values from Electron WebGL, got {pixels.size}")
    return result, pixels.reshape((job["height"], job["width"], 4))


def read_job_pixels(job: dict) -> np.ndarray:
    pixels = np.fromfile(job["outputPath"], dtype=np.float32)
    expected = job["width"] * job["height"] * 4
    if pixels.size != expected:
        raise RuntimeError(f"Expected {expected} float values from Electron WebGL, got {pixels.size}")
    return pixels.reshape((job["height"], job["width"], 4))


def pixel_error(texcoord_a: tuple[float, float], texcoord_b: tuple[float, float], width: int, height: int) -> float:
    px_a = texcoord_to_pixel_center(texcoord_a, width, height)
    px_b = texcoord_to_pixel_center(texcoord_b, width, height)
    return max(abs(px_a[0] - px_b[0]), abs(px_a[1] - px_b[1]))


def is_finite_texcoord(texcoord: tuple[float, float]) -> bool:
    return math.isfinite(texcoord[0]) and math.isfinite(texcoord[1])


def synthetic_texture(width: int, height: int) -> np.ndarray:
    y, x = np.indices((height, width), dtype=np.float64)
    return (x + 2.0 * (height - 1 - y)) / (width + 2.0 * height)


def print_gl_setup_errors(result: dict) -> None:
    errors = {key: value for key, value in result.get("errors", {}).items() if value}
    if errors:
        print(f"gl_setup_errors={errors}")


def compare_hpc_shader_to_astropy(
    fits_file: Path,
    hdu: int | None,
    render_size: int,
    electron: Path,
    output_dir: Path,
    max_error_px: float,
    max_sample_error: float,
    sample_texture: bool,
    backend: str,
) -> int:
    image_data, meta, _projection_wcs, pixel_wcs = load_validation_context(fits_file, hdu)
    if meta.projection not in PROJECTION_CODES:
        raise ValueError(f"Electron WebGL HPC validation does not support projection {meta.projection!r}")

    output_dir.mkdir(parents=True, exist_ok=True)
    bounds_deg = hpc_bounds_degrees(meta, 1.0)
    job = common_job("hpc", fits_file, render_size, output_dir, meta, bounds_deg, sample_texture, backend)
    result, pixels = run_shader_job(electron, backend, job)
    return evaluate_hpc_shader_to_astropy(
        fits_file,
        render_size,
        max_error_px,
        max_sample_error,
        sample_texture,
        image_data,
        meta,
        pixel_wcs,
        bounds_deg,
        job,
        result,
        pixels,
    )


def evaluate_hpc_shader_to_astropy(
    fits_file: Path,
    render_size: int,
    max_error_px: float,
    max_sample_error: float,
    sample_texture: bool,
    image_data: np.ndarray,
    meta,
    pixel_wcs,
    bounds_deg: tuple[float, float, float, float],
    job: dict,
    result: dict,
    pixels: np.ndarray,
) -> int:
    texture_data = synthetic_texture(job["textureWidth"], job["textureHeight"]) if sample_texture else None

    max_px_err = 0.0
    sum_px_err2 = 0.0
    max_sample_err = 0.0
    sum_sample_err2 = 0.0
    max_shader_cpu_px_err = 0.0
    sum_shader_cpu_px_err2 = 0.0
    max_cpu_astropy_px_err = 0.0
    sum_cpu_astropy_px_err2 = 0.0
    max_shader_float32_px_err = 0.0
    sum_shader_float32_px_err2 = 0.0
    max_float32_astropy_px_err = 0.0
    sum_float32_astropy_px_err2 = 0.0
    count = 0
    cpu_compare_count = 0
    float32_compare_count = 0
    skipped = 0

    for iy in range(render_size):
        sy = (iy + 0.5) / render_size
        row_world_deg = np.empty((render_size, 2), dtype=np.float64)
        for ix in range(render_size):
            sx = (ix + 0.5) / render_size
            row_world_deg[ix, 0] = bounds_deg[0] + sx * (bounds_deg[1] - bounds_deg[0])
            row_world_deg[ix, 1] = bounds_deg[2] + sy * (bounds_deg[3] - bounds_deg[2])

        astro_px_raw = pixel_wcs.wcs_world2pix(row_world_deg, 1)
        astro_px = np.column_stack((astro_px_raw[:, 0] - 0.5, astro_px_raw[:, 1] - 0.5))

        for ix in range(render_size):
            sx = (ix + 0.5) / render_size
            texcoord = (float(pixels[iy, ix, 0]), float(pixels[iy, ix, 1]))
            valid = float(pixels[iy, ix, 3]) > 0.5
            if not valid or not math.isfinite(texcoord[0]) or not math.isfinite(texcoord[1]):
                skipped += 1
                continue

            try:
                shader_px = texcoord_to_pixel_center(texcoord, meta.pixel_width, meta.pixel_height)
            except ValueError:
                skipped += 1
                continue

            if not np.all(np.isfinite(astro_px[ix])):
                skipped += 1
                continue

            cpu_texcoord, _, _, _, _, _ = renderHpcTexcoords((sx, sy), bounds_deg, meta, image_data)
            float32_texcoord, _, _, _ = renderHpcTexcoordsFloat32((sx, sy), bounds_deg, meta, image_data)

            err = max(abs(shader_px[0] - astro_px[ix, 0]), abs(shader_px[1] - astro_px[ix, 1]))
            max_px_err = max(max_px_err, float(err))
            sum_px_err2 += float(err * err)
            if sample_texture and texture_data is not None:
                shader_sample = float(pixels[iy, ix, 2])
                cpu_sample = sample_texture_linear(texture_data, texcoord)
                sample_err = abs(shader_sample - cpu_sample)
                max_sample_err = max(max_sample_err, float(sample_err))
                sum_sample_err2 += float(sample_err * sample_err)
            count += 1

            if math.isfinite(cpu_texcoord[0]) and math.isfinite(cpu_texcoord[1]):
                cpu_px = texcoord_to_pixel_center(cpu_texcoord, meta.pixel_width, meta.pixel_height)
                shader_cpu_err = max(abs(shader_px[0] - cpu_px[0]), abs(shader_px[1] - cpu_px[1]))
                cpu_astropy_err = max(abs(cpu_px[0] - astro_px[ix, 0]), abs(cpu_px[1] - astro_px[ix, 1]))
                max_shader_cpu_px_err = max(max_shader_cpu_px_err, float(shader_cpu_err))
                max_cpu_astropy_px_err = max(max_cpu_astropy_px_err, float(cpu_astropy_err))
                sum_shader_cpu_px_err2 += float(shader_cpu_err * shader_cpu_err)
                sum_cpu_astropy_px_err2 += float(cpu_astropy_err * cpu_astropy_err)
                cpu_compare_count += 1

            if math.isfinite(float32_texcoord[0]) and math.isfinite(float32_texcoord[1]):
                float32_px = texcoord_to_pixel_center(float32_texcoord, meta.pixel_width, meta.pixel_height)
                shader_float32_err = max(abs(shader_px[0] - float32_px[0]), abs(shader_px[1] - float32_px[1]))
                float32_astropy_err = max(abs(float32_px[0] - astro_px[ix, 0]), abs(float32_px[1] - astro_px[ix, 1]))
                max_shader_float32_px_err = max(max_shader_float32_px_err, float(shader_float32_err))
                max_float32_astropy_px_err = max(max_float32_astropy_px_err, float(float32_astropy_err))
                sum_shader_float32_px_err2 += float(shader_float32_err * shader_float32_err)
                sum_float32_astropy_px_err2 += float(float32_astropy_err * float32_astropy_err)
                float32_compare_count += 1

    print(f"file={fits_file}")
    print(f"mode=electron_hpc_{'sample' if sample_texture else 'render'}_compare size={render_size}")
    print(f"renderer={result['renderer']}")
    print(f"gl_errors=(clear={result.get('clearError')}, draw={result.get('drawError')}, read={result.get('readError')})")
    print_gl_setup_errors(result)
    print(f"bounds_deg=({bounds_deg[0]:.12f}, {bounds_deg[1]:.12f}, {bounds_deg[2]:.12f}, {bounds_deg[3]:.12f})")
    print(f"valid_samples={count}")
    print(f"skipped_samples={skipped}")
    print(f"pixel_center_max_error_px={max_px_err:.6e}")
    print(f"pixel_center_rms_error_px={math.sqrt(sum_px_err2 / count):.6e}" if count > 0 else "pixel_center_rms_error_px=nan")
    if sample_texture:
        print(f"sample_max_error={max_sample_err:.6e}")
        print(f"sample_rms_error={math.sqrt(sum_sample_err2 / count):.6e}" if count > 0 else "sample_rms_error=nan")
    print(f"cpu_comparable_samples={cpu_compare_count}")
    print(f"shader_vs_cpu_max_error_px={max_shader_cpu_px_err:.6e}")
    print(f"shader_vs_cpu_rms_error_px={math.sqrt(sum_shader_cpu_px_err2 / cpu_compare_count):.6e}" if cpu_compare_count > 0 else "shader_vs_cpu_rms_error_px=nan")
    print(f"cpu_vs_astropy_max_error_px={max_cpu_astropy_px_err:.6e}")
    print(f"cpu_vs_astropy_rms_error_px={math.sqrt(sum_cpu_astropy_px_err2 / cpu_compare_count):.6e}" if cpu_compare_count > 0 else "cpu_vs_astropy_rms_error_px=nan")
    print(f"float32_cpu_comparable_samples={float32_compare_count}")
    print(f"shader_vs_float32_cpu_max_error_px={max_shader_float32_px_err:.6e}")
    print(f"shader_vs_float32_cpu_rms_error_px={math.sqrt(sum_shader_float32_px_err2 / float32_compare_count):.6e}" if float32_compare_count > 0 else "shader_vs_float32_cpu_rms_error_px=nan")
    print(f"float32_cpu_vs_astropy_max_error_px={max_float32_astropy_px_err:.6e}")
    print(f"float32_cpu_vs_astropy_rms_error_px={math.sqrt(sum_float32_astropy_px_err2 / float32_compare_count):.6e}" if float32_compare_count > 0 else "float32_cpu_vs_astropy_rms_error_px=nan")
    print(f"electron_rgba32f={job['outputPath']}")
    if count == 0:
        print("FAILED: no valid Electron WebGL samples")
        return 1
    if max_px_err > max_error_px:
        print(f"FAILED: pixel_center_max_error_px exceeds {max_error_px:.6e}")
        return 1
    if sample_texture and max_sample_err > max_sample_error:
        print(f"FAILED: sample_max_error exceeds {max_sample_error:.6e}")
        return 1
    return 0


def cpu_texcoord_for_mode(
    mode: str,
    sx: float,
    sy: float,
    meta,
    image_data: np.ndarray,
    outer_radius: float,
    warp_lambda: float,
) -> tuple[float, float]:
    if mode == "ortho":
        screen_xy = (2.0 * sx - 1.0, 2.0 * sy - 1.0)
        if screen_xy[0] * screen_xy[0] + screen_xy[1] * screen_xy[1] > 1.0:
            return (math.nan, math.nan)
        texcoord, _, _, _ = renderOrthographicTexcoords(screen_xy, meta, image_data, simple_tan=True)
        return texcoord
    if mode == "lati_zenithal":
        texcoord, _ = renderLatitudinalTexcoords(
            (sx, sy),
            LATI_ZENITHAL_BOUNDS_DEG,
            meta,
            image_data,
            grid=(0.0, 0.0, 0.0),
        )
        return texcoord
    if mode in WARP_MODES:
        hpc_xy = warp_hpc_xy(mode, sx, sy, outer_radius, warp_lambda)
        if not is_finite_texcoord(hpc_xy) or not clipHpcGeometry(hpc_xy):
            return (math.nan, math.nan)
        helioprojective = worldToHelioprojective((hpc_xy[0], hpc_xy[1], 0.0), meta.observer_distance)
        texcoord, _ = sampleHpcTexcoord(helioprojective, hpc_xy, meta, image_data)
        return texcoord
    raise ValueError(f"Unsupported Electron WebGL mode: {mode}")


def unwarp_radius(normalized_radius: float, outer_radius: float, warp_lambda: float) -> float:
    limb_position = 1.0 / outer_radius
    if outer_radius <= 1.0 or normalized_radius <= limb_position:
        return normalized_radius / limb_position

    u = (normalized_radius - limb_position) / (1.0 - limb_position)
    return (
        math.pow(outer_radius, u)
        if warp_lambda == 0.0
        else math.pow(1.0 + u * (math.pow(outer_radius, warp_lambda) - 1.0), 1.0 / warp_lambda)
    )


def warp_hpc_xy(mode: str, sx: float, sy: float, outer_radius: float, warp_lambda: float) -> tuple[float, float]:
    if mode == "radial_warp":
        x = sx - 0.5
        y = sy - 0.5
        radius = math.hypot(x, y)
        t = 2.0 * radius
        if t > 1.0 or t == 0.0:
            return (math.nan, math.nan)
        radial_coordinate = unwarp_radius(t, outer_radius, warp_lambda)
        scale = radial_coordinate / radius
        return (scale * x, scale * y)
    if mode == "rect_warp":
        angle = sx * 2.0 * math.pi
        radial_coordinate = unwarp_radius(sy, outer_radius, warp_lambda)
        return (-radial_coordinate * math.sin(angle), radial_coordinate * math.cos(angle))
    raise ValueError(f"Unsupported warp mode: {mode}")


def expected_plane_internal_for_mode(
    mode: str,
    sx: float,
    sy: float,
    meta,
) -> tuple[float, float]:
    if mode == "ortho":
        x = 2.0 * sx - 1.0
        y = 2.0 * sy - 1.0
        radius2 = x * x + y * y
        if radius2 > 1.0:
            return (math.nan, math.nan)
        return (x - meta.crval_internal_x, y - meta.crval_internal_y)

    if mode == "lati_zenithal":
        longitude = sx * (2.0 * math.pi)
        latitude = (sy - 0.5) * math.pi
        if latitude < -0.5 * math.pi or latitude > 0.5 * math.pi:
            return (math.nan, math.nan)
        cos_latitude = math.cos(latitude)
        spherical = (
            cos_latitude * math.cos(longitude),
            cos_latitude * math.sin(longitude),
            math.sin(latitude),
        )
        if spherical[0] < 0.0:
            return (math.nan, math.nan)
        return (spherical[1] - meta.crval_internal_x, spherical[2] - meta.crval_internal_y)

    return (math.nan, math.nan)


def bounds_for_mode(mode: str) -> tuple[float, float, float, float]:
    if mode == "lati_zenithal":
        return LATI_ZENITHAL_BOUNDS_DEG
    if mode in WARP_MODES:
        return (0.0, 360.0, 0.0, WARP_OUTER_RADIUS)
    return (0.0, 1.0, 0.0, 1.0)


def make_mode_job(
    mode: str,
    fits_file: Path,
    render_size: int,
    output_dir: Path,
    sample_texture: bool,
    meta,
    backend: str,
    diff_selfcheck: bool = False,
    color_smoke: bool = False,
    color_diff_smoke: bool = False,
    warp_lambda: float = DEFAULT_WARP_LAMBDA,
    name: str | None = None,
) -> dict:
    job = common_job(
        mode,
        fits_file,
        render_size,
        output_dir,
        meta,
        bounds_for_mode(mode),
        sample_texture,
        backend,
        name if name is not None else f"{mode}_diff_selfcheck" if diff_selfcheck else None,
        diff_selfcheck=diff_selfcheck,
        color_smoke=color_smoke,
        color_diff_smoke=color_diff_smoke,
    )
    if mode == "lati_zenithal":
        job["latiGrid"] = [0.0, 0.0, 0.0]
    if mode in WARP_MODES:
        job["warpLambda"] = warp_lambda
    return job


def compare_shader_to_cpu(
    mode: str,
    fits_file: Path,
    hdu: int | None,
    render_size: int,
    electron: Path,
    output_dir: Path,
    max_error_px: float,
    max_sample_error: float,
    sample_texture: bool,
    backend: str,
) -> int:
    image_data, meta, projection_wcs, pixel_wcs = load_validation_context(fits_file, hdu)
    if mode == "ortho" and meta.projection not in PROJECTION_CODES:
        raise ValueError(f"Electron WebGL Ortho validation does not support projection {meta.projection!r}")

    output_dir.mkdir(parents=True, exist_ok=True)
    job = make_mode_job(mode, fits_file, render_size, output_dir, sample_texture, meta, backend)

    result, pixels = run_shader_job(electron, backend, job)
    return evaluate_shader_to_cpu(
        mode,
        fits_file,
        render_size,
        max_error_px,
        max_sample_error,
        sample_texture,
        image_data,
        meta,
        projection_wcs,
        pixel_wcs,
        job,
        result,
        pixels,
    )


def evaluate_shader_to_cpu(
    mode: str,
    fits_file: Path,
    render_size: int,
    max_error_px: float,
    max_sample_error: float,
    sample_texture: bool,
    image_data: np.ndarray,
    meta,
    projection_wcs,
    pixel_wcs,
    job: dict,
    result: dict,
    pixels: np.ndarray,
) -> int:
    texture_data = synthetic_texture(job["textureWidth"], job["textureHeight"]) if sample_texture else None

    max_px_err = 0.0
    sum_px_err2 = 0.0
    max_sample_err = 0.0
    sum_sample_err2 = 0.0
    count = 0
    skipped = 0
    shader_only = 0
    cpu_only = 0
    astropy_count = 0
    max_shader_astropy_px_err = 0.0
    sum_shader_astropy_px_err2 = 0.0

    for iy in range(render_size):
        sy = (iy + 0.5) / render_size
        for ix in range(render_size):
            sx = (ix + 0.5) / render_size
            shader_texcoord = (float(pixels[iy, ix, 0]), float(pixels[iy, ix, 1]))
            shader_valid = float(pixels[iy, ix, 3]) > 0.5 and is_finite_texcoord(shader_texcoord)
            cpu_texcoord = cpu_texcoord_for_mode(
                mode,
                sx,
                sy,
                meta,
                image_data,
                job["boundsDeg"][3],
                job.get("warpLambda", DEFAULT_WARP_LAMBDA),
            )
            cpu_valid = is_finite_texcoord(cpu_texcoord)

            if not shader_valid and not cpu_valid:
                skipped += 1
                continue
            if shader_valid != cpu_valid:
                if shader_valid:
                    shader_only += 1
                else:
                    cpu_only += 1
                skipped += 1
                continue

            err = pixel_center_error_px(
                texcoord_to_pixel_center(shader_texcoord, meta.pixel_width, meta.pixel_height),
                texcoord_to_pixel_center(cpu_texcoord, meta.pixel_width, meta.pixel_height),
                meta,
            )
            max_px_err = max(max_px_err, float(err))
            sum_px_err2 += float(err * err)
            if sample_texture and texture_data is not None:
                shader_sample = float(pixels[iy, ix, 2])
                cpu_sample = sample_texture_linear(texture_data, shader_texcoord)
                sample_err = abs(shader_sample - cpu_sample)
                max_sample_err = max(max_sample_err, float(sample_err))
                sum_sample_err2 += float(sample_err * sample_err)
            if projection_wcs is not None and pixel_wcs is not None:
                expected_plane_internal = expected_plane_internal_for_mode(mode, sx, sy, meta)
                if math.isfinite(expected_plane_internal[0]) and math.isfinite(expected_plane_internal[1]):
                    expected_plane_deg = (
                        math.degrees(expected_plane_internal[0] / meta.plane_units_per_rad),
                        math.degrees(expected_plane_internal[1] / meta.plane_units_per_rad),
                    )
                    expected_world_deg = projection_wcs.wcs_pix2world([expected_plane_deg], 0)[0]
                    astro_px = pixel_wcs.wcs_world2pix([expected_world_deg], 1)[0]
                    if np.all(np.isfinite(astro_px)):
                        shader_px = texcoord_to_pixel_center(shader_texcoord, meta.pixel_width, meta.pixel_height)
                        astro_pixel_center = (float(astro_px[0] - 0.5), float(astro_px[1] - 0.5))
                        astropy_err = pixel_center_error_px(shader_px, astro_pixel_center, meta)
                        max_shader_astropy_px_err = max(max_shader_astropy_px_err, float(astropy_err))
                        sum_shader_astropy_px_err2 += float(astropy_err * astropy_err)
                        astropy_count += 1
            count += 1

    print(f"file={fits_file}")
    print(f"mode=electron_{mode}_{'sample' if sample_texture else 'texcoord'}_compare size={render_size}")
    print(f"renderer={result['renderer']}")
    print(f"gl_errors=(clear={result.get('clearError')}, draw={result.get('drawError')}, read={result.get('readError')})")
    print_gl_setup_errors(result)
    print(f"valid_samples={count}")
    print(f"skipped_samples={skipped}")
    print(f"shader_only_samples={shader_only}")
    print(f"cpu_only_samples={cpu_only}")
    print(f"shader_vs_cpu_max_error_px={max_px_err:.6e}")
    print(f"shader_vs_cpu_rms_error_px={math.sqrt(sum_px_err2 / count):.6e}" if count > 0 else "shader_vs_cpu_rms_error_px=nan")
    print(f"shader_vs_astropy_samples={astropy_count}")
    print(f"shader_vs_astropy_max_error_px={max_shader_astropy_px_err:.6e}")
    print(f"shader_vs_astropy_rms_error_px={math.sqrt(sum_shader_astropy_px_err2 / astropy_count):.6e}" if astropy_count > 0 else "shader_vs_astropy_rms_error_px=nan")
    if sample_texture:
        print(f"sample_max_error={max_sample_err:.6e}")
        print(f"sample_rms_error={math.sqrt(sum_sample_err2 / count):.6e}" if count > 0 else "sample_rms_error=nan")
    print(f"electron_rgba32f={job['outputPath']}")
    if count == 0:
        print("FAILED: no comparable Electron WebGL samples")
        return 1
    if shader_only or cpu_only:
        print("FAILED: shader/CPU validity masks differ")
        return 1
    if max_px_err > max_error_px:
        print(f"FAILED: shader_vs_cpu_max_error_px exceeds {max_error_px:.6e}")
        return 1
    if astropy_count > 0 and max_shader_astropy_px_err > max_error_px:
        print(f"FAILED: shader_vs_astropy_max_error_px exceeds {max_error_px:.6e}")
        return 1
    if sample_texture and max_sample_err > max_sample_error:
        print(f"FAILED: sample_max_error exceeds {max_sample_error:.6e}")
        return 1
    return 0


def compare_batch(
    fits_file: Path,
    hdu: int | None,
    render_size: int,
    electron: Path,
    output_dir: Path,
    max_error_px: float,
    max_sample_error: float,
    sample_texture: bool,
    backend: str,
) -> int:
    image_data, meta, projection_wcs, pixel_wcs = load_validation_context(fits_file, hdu)
    output_dir.mkdir(parents=True, exist_ok=True)

    jobs: list[dict] = []
    metadata: list[dict] = []
    for mode in ALL_MODES:
        if mode == "hpc":
            if meta.projection not in PROJECTION_CODES:
                continue
            bounds = hpc_bounds_degrees(meta, 1.0)
            job = common_job("hpc", fits_file, render_size, output_dir, meta, bounds, sample_texture, backend)
            jobs.append(job)
            metadata.append({"mode": mode, "pixel_wcs": pixel_wcs, "bounds": bounds})
        elif mode in WARP_MODES:
            for warp_lambda in WARP_LAMBDAS:
                lambda_name = f"{warp_lambda:g}".replace("-", "minus").replace(".", "_")
                job = make_mode_job(
                    mode,
                    fits_file,
                    render_size,
                    output_dir,
                    sample_texture,
                    meta,
                    backend,
                    warp_lambda=warp_lambda,
                    name=f"{mode}_lambda_{lambda_name}",
                )
                jobs.append(job)
                metadata.append({"mode": mode, "projection_wcs": projection_wcs, "pixel_wcs": pixel_wcs})
        else:
            if mode == "ortho" and meta.projection not in PROJECTION_CODES:
                continue
            job = make_mode_job(mode, fits_file, render_size, output_dir, sample_texture, meta, backend)
            jobs.append(job)
            metadata.append({"mode": mode, "projection_wcs": projection_wcs, "pixel_wcs": pixel_wcs})

    results = run_electron_jobs(electron, jobs, backend)
    failed = False
    for job, info, result in zip(jobs, metadata, results, strict=True):
        pixels = read_job_pixels(job)
        mode = info["mode"]
        if mode == "hpc":
            code = evaluate_hpc_shader_to_astropy(
                fits_file,
                render_size,
                max_error_px,
                max_sample_error,
                sample_texture,
                image_data,
                meta,
                info["pixel_wcs"],
                info["bounds"],
                job,
                result,
                pixels,
            )
        else:
            mode_max_error = 1.0 if mode == "lati_zenithal" else max_error_px
            code = evaluate_shader_to_cpu(
                mode,
                fits_file,
                render_size,
                mode_max_error,
                max_sample_error,
                sample_texture,
                image_data,
                meta,
                info["projection_wcs"],
                info["pixel_wcs"],
                job,
                result,
                pixels,
            )
        failed = failed or code != 0

    return 1 if failed else 0


def compare_tan_all_modes_case_batch(
    electron: Path,
    output_dir: Path,
    render_size: int,
    max_error_px: float,
    max_sample_error: float,
    sample_texture: bool,
    backend: str,
) -> int:
    output_dir.mkdir(parents=True, exist_ok=True)

    jobs: list[dict] = []
    metadata: list[dict] = []
    for name, filename, hdu in TAN_SCREEN_CASES:
        fits_file = SCRIPT_DIR / "data" / filename
        image_data, meta, projection_wcs, pixel_wcs = load_validation_context(fits_file, hdu)
        if meta.projection not in PROJECTION_CODES:
            raise ValueError(f"Electron WebGL TAN all-modes validation does not support projection {meta.projection!r} for {fits_file}")

        for mode in ALL_MODES:
            if mode == "hpc":
                bounds = hpc_bounds_degrees(meta, 1.0)
                job = common_job("hpc", fits_file, render_size, output_dir, meta, bounds, sample_texture, backend, f"{name}_hpc")
                jobs.append(job)
                metadata.append({
                    "mode": mode,
                    "fits_file": fits_file,
                    "image_data": image_data,
                    "meta": meta,
                    "pixel_wcs": pixel_wcs,
                    "bounds": bounds,
                })
            else:
                job = make_mode_job(mode, fits_file, render_size, output_dir, sample_texture, meta, backend)
                job["name"] = f"{name}_{mode}"
                kind = "sample" if sample_texture else "texcoord"
                job["outputPath"] = str(output_dir / f"{fits_file.stem}_{backend}_{name}_{mode}_{kind}.rgba32f")
                jobs.append(job)
                metadata.append({
                    "mode": mode,
                    "fits_file": fits_file,
                    "image_data": image_data,
                    "meta": meta,
                    "projection_wcs": projection_wcs,
                    "pixel_wcs": pixel_wcs,
                })

    results = run_electron_jobs(electron, jobs, backend)
    failed = False
    for job, info, result in zip(jobs, metadata, results, strict=True):
        pixels = read_job_pixels(job)
        mode = info["mode"]
        if mode == "hpc":
            code = evaluate_hpc_shader_to_astropy(
                info["fits_file"],
                render_size,
                max_error_px,
                max_sample_error,
                sample_texture,
                info["image_data"],
                info["meta"],
                info["pixel_wcs"],
                info["bounds"],
                job,
                result,
                pixels,
            )
        else:
            mode_max_error = 1.0 if mode == "lati_zenithal" else max_error_px
            code = evaluate_shader_to_cpu(
                mode,
                info["fits_file"],
                render_size,
                mode_max_error,
                max_sample_error,
                sample_texture,
                info["image_data"],
                info["meta"],
                info["projection_wcs"],
                info["pixel_wcs"],
                job,
                result,
                pixels,
            )
        failed = failed or code != 0

    return 1 if failed else 0


def compare_tan_all_modes_color_smoke_batch(
    electron: Path,
    output_dir: Path,
    render_size: int,
    backend: str,
    diff_mode: bool = False,
) -> int:
    output_dir.mkdir(parents=True, exist_ok=True)

    jobs: list[dict] = []
    metadata: list[dict] = []
    suffix = "color_diff_smoke" if diff_mode else "color_smoke"
    for name, filename, hdu in TAN_SCREEN_CASES:
        fits_file = SCRIPT_DIR / "data" / filename
        _image_data, meta, _projection_wcs, _pixel_wcs = load_validation_context(fits_file, hdu)
        if meta.projection not in PROJECTION_CODES:
            raise ValueError(f"Electron WebGL TAN all-modes color smoke does not support projection {meta.projection!r} for {fits_file}")

        for mode in ALL_MODES:
            if mode == "hpc":
                job = common_job(
                    "hpc",
                    fits_file,
                    render_size,
                    output_dir,
                    meta,
                    hpc_bounds_degrees(meta, 1.0),
                    True,
                    backend,
                    f"{name}_hpc_{suffix}",
                    color_smoke=not diff_mode,
                    color_diff_smoke=diff_mode,
                )
            else:
                job = make_mode_job(
                    mode,
                    fits_file,
                    render_size,
                    output_dir,
                    True,
                    meta,
                    backend,
                    color_smoke=not diff_mode,
                    color_diff_smoke=diff_mode,
                )
                job["name"] = f"{name}_{mode}_{suffix}"
                kind = "color_diff" if diff_mode else "color"
                job["outputPath"] = str(output_dir / f"{fits_file.stem}_{backend}_{name}_{mode}_{suffix}_{kind}.rgba32f")
            jobs.append(job)
            metadata.append({"fits_file": fits_file})

    results = run_electron_jobs(electron, jobs, backend)
    failed = False
    for job, info, result in zip(jobs, metadata, results, strict=True):
        pixels = read_job_pixels(job)
        failed = failed or evaluate_color_smoke(info["fits_file"], render_size, job, result, pixels) != 0

    return 1 if failed else 0


def compare_tan_screen_case_batch(
    electron: Path,
    output_dir: Path,
    render_size: int,
    max_error_px: float,
    max_sample_error: float,
    sample_texture: bool,
    backend: str,
) -> int:
    output_dir.mkdir(parents=True, exist_ok=True)

    jobs: list[dict] = []
    metadata: list[dict] = []
    for name, filename, hdu in TAN_SCREEN_CASES:
        fits_file = SCRIPT_DIR / "data" / filename
        image_data, meta, projection_wcs, pixel_wcs = load_validation_context(fits_file, hdu)
        if meta.projection not in PROJECTION_CODES:
            raise ValueError(f"Electron WebGL TAN screen validation does not support projection {meta.projection!r} for {fits_file}")

        bounds = hpc_bounds_degrees(meta, 1.0)
        hpc_job = common_job("hpc", fits_file, render_size, output_dir, meta, bounds, sample_texture, backend, f"{name}_hpc")
        jobs.append(hpc_job)
        metadata.append({
            "mode": "hpc",
            "fits_file": fits_file,
            "image_data": image_data,
            "meta": meta,
            "pixel_wcs": pixel_wcs,
            "bounds": bounds,
        })

        ortho_job = make_mode_job("ortho", fits_file, render_size, output_dir, sample_texture, meta, backend)
        ortho_job["name"] = f"{name}_ortho"
        ortho_job["outputPath"] = str(output_dir / f"{fits_file.stem}_{backend}_{name}_ortho_{'sample' if sample_texture else 'texcoord'}.rgba32f")
        jobs.append(ortho_job)
        metadata.append({
            "mode": "ortho",
            "fits_file": fits_file,
            "image_data": image_data,
            "meta": meta,
            "projection_wcs": projection_wcs,
            "pixel_wcs": pixel_wcs,
        })

    results = run_electron_jobs(electron, jobs, backend)
    failed = False
    for job, info, result in zip(jobs, metadata, results, strict=True):
        pixels = read_job_pixels(job)
        if info["mode"] == "hpc":
            code = evaluate_hpc_shader_to_astropy(
                info["fits_file"],
                render_size,
                max_error_px,
                max_sample_error,
                sample_texture,
                info["image_data"],
                info["meta"],
                info["pixel_wcs"],
                info["bounds"],
                job,
                result,
                pixels,
            )
        else:
            code = evaluate_shader_to_cpu(
                info["mode"],
                info["fits_file"],
                render_size,
                max_error_px,
                max_sample_error,
                sample_texture,
                info["image_data"],
                info["meta"],
                info["projection_wcs"],
                info["pixel_wcs"],
                job,
                result,
                pixels,
            )
        failed = failed or code != 0

    return 1 if failed else 0


def compare_tan_screen_color_smoke_batch(
    electron: Path,
    output_dir: Path,
    render_size: int,
    backend: str,
    diff_mode: bool = False,
) -> int:
    output_dir.mkdir(parents=True, exist_ok=True)

    jobs: list[dict] = []
    metadata: list[dict] = []
    suffix = "color_diff_smoke" if diff_mode else "color_smoke"
    for name, filename, hdu in TAN_SCREEN_CASES:
        fits_file = SCRIPT_DIR / "data" / filename
        _image_data, meta, _projection_wcs, _pixel_wcs = load_validation_context(fits_file, hdu)
        if meta.projection not in PROJECTION_CODES:
            raise ValueError(f"Electron WebGL TAN color smoke does not support projection {meta.projection!r} for {fits_file}")

        hpc_job = common_job(
            "hpc",
            fits_file,
            render_size,
            output_dir,
            meta,
            hpc_bounds_degrees(meta, 1.0),
            True,
            backend,
            f"{name}_hpc_{suffix}",
            color_smoke=not diff_mode,
            color_diff_smoke=diff_mode,
        )
        jobs.append(hpc_job)
        metadata.append({"fits_file": fits_file})

        ortho_job = make_mode_job(
            "ortho",
            fits_file,
            render_size,
            output_dir,
            True,
            meta,
            backend,
            color_smoke=not diff_mode,
            color_diff_smoke=diff_mode,
        )
        ortho_job["name"] = f"{name}_ortho_{suffix}"
        kind = "color_diff" if diff_mode else "color"
        ortho_job["outputPath"] = str(output_dir / f"{fits_file.stem}_{backend}_{name}_ortho_{suffix}_{kind}.rgba32f")
        jobs.append(ortho_job)
        metadata.append({"fits_file": fits_file})

    results = run_electron_jobs(electron, jobs, backend)
    failed = False
    for job, info, result in zip(jobs, metadata, results, strict=True):
        pixels = read_job_pixels(job)
        failed = failed or evaluate_color_smoke(info["fits_file"], render_size, job, result, pixels) != 0

    return 1 if failed else 0


def compare_hpc_projection_batch(
    electron: Path,
    output_dir: Path,
    render_size: int,
    max_error_px: float,
    max_sample_error: float,
    sample_texture: bool,
    backend: str,
) -> int:
    output_dir.mkdir(parents=True, exist_ok=True)

    jobs: list[dict] = []
    metadata: list[dict] = []
    for name, filename, hdu, case_max_error_px in HPC_PROJECTION_CASES:
        fits_file = SCRIPT_DIR / "data" / filename
        image_data, meta, _projection_wcs, pixel_wcs = load_validation_context(fits_file, hdu)
        if meta.projection not in PROJECTION_CODES:
            raise ValueError(f"Electron WebGL HPC validation does not support projection {meta.projection!r} for {fits_file}")
        bounds = hpc_bounds_degrees(meta, 1.0)
        job = common_job("hpc", fits_file, render_size, output_dir, meta, bounds, sample_texture, backend, name)
        jobs.append(job)
        metadata.append({
            "fits_file": fits_file,
            "image_data": image_data,
            "meta": meta,
            "pixel_wcs": pixel_wcs,
            "bounds": bounds,
            "max_error_px": case_max_error_px,
        })

    results = run_electron_jobs(electron, jobs, backend)
    failed = False
    for job, info, result in zip(jobs, metadata, results, strict=True):
        pixels = read_job_pixels(job)
        code = evaluate_hpc_shader_to_astropy(
            info["fits_file"],
            render_size,
            max(max_error_px, info["max_error_px"]),
            max_sample_error,
            sample_texture,
            info["image_data"],
            info["meta"],
            info["pixel_wcs"],
            info["bounds"],
            job,
            result,
            pixels,
        )
        failed = failed or code != 0

    return 1 if failed else 0


def compare_hpc_render_case_batch(
    electron: Path,
    output_dir: Path,
    render_size: int,
    max_error_px: float,
    max_sample_error: float,
    sample_texture: bool,
    backend: str,
) -> int:
    output_dir.mkdir(parents=True, exist_ok=True)

    jobs: list[dict] = []
    metadata: list[dict] = []
    for name, filename, hdu, case_max_error_px in HPC_RENDER_CASES:
        fits_file = SCRIPT_DIR / "data" / filename
        image_data, meta, _projection_wcs, pixel_wcs = load_validation_context(fits_file, hdu)
        if meta.projection not in PROJECTION_CODES:
            raise ValueError(f"Electron WebGL HPC validation does not support projection {meta.projection!r} for {fits_file}")
        bounds = hpc_bounds_degrees(meta, 1.0)
        job = common_job("hpc", fits_file, render_size, output_dir, meta, bounds, sample_texture, backend, name)
        jobs.append(job)
        metadata.append({
            "fits_file": fits_file,
            "image_data": image_data,
            "meta": meta,
            "pixel_wcs": pixel_wcs,
            "bounds": bounds,
            "max_error_px": case_max_error_px,
        })

    results = run_electron_jobs(electron, jobs, backend)
    failed = False
    for job, info, result in zip(jobs, metadata, results, strict=True):
        pixels = read_job_pixels(job)
        code = evaluate_hpc_shader_to_astropy(
            info["fits_file"],
            render_size,
            max(max_error_px, info["max_error_px"]),
            max_sample_error,
            sample_texture,
            info["image_data"],
            info["meta"],
            info["pixel_wcs"],
            info["bounds"],
            job,
            result,
            pixels,
        )
        failed = failed or code != 0

    return 1 if failed else 0


def evaluate_surface_map_shader_to_cpu(
    fits_file: Path,
    render_size: int,
    max_error_px: float,
    max_sample_error: float,
    sample_texture: bool,
    image_data: np.ndarray,
    meta,
    pixel_wcs,
    job: dict,
    result: dict,
    pixels: np.ndarray,
) -> int:
    texture_data = synthetic_texture(job["textureWidth"], job["textureHeight"]) if sample_texture else None
    max_px_err = 0.0
    sum_px_err2 = 0.0
    max_sample_err = 0.0
    sum_sample_err2 = 0.0
    count = 0
    skipped = 0
    shader_only = 0
    cpu_only = 0
    astropy_count = 0
    max_shader_astropy_px_err = 0.0
    sum_shader_astropy_px_err2 = 0.0

    for iy in range(render_size):
        sy = (iy + 0.5) / render_size
        for ix in range(render_size):
            sx = (ix + 0.5) / render_size
            shader_texcoord = (float(pixels[iy, ix, 0]), float(pixels[iy, ix, 1]))
            shader_valid = float(pixels[iy, ix, 3]) > 0.5 and is_finite_texcoord(shader_texcoord)
            cpu_texcoord, _ = renderLatitudinalTexcoords((sx, sy), LATI_SURFACE_BOUNDS_DEG, meta, image_data)
            cpu_valid = is_finite_texcoord(cpu_texcoord)

            if not shader_valid and not cpu_valid:
                skipped += 1
                continue
            if shader_valid != cpu_valid:
                if shader_valid:
                    shader_only += 1
                else:
                    cpu_only += 1
                skipped += 1
                continue

            err = pixel_center_error_px(
                texcoord_to_pixel_center(shader_texcoord, meta.pixel_width, meta.pixel_height),
                texcoord_to_pixel_center(cpu_texcoord, meta.pixel_width, meta.pixel_height),
                meta,
            )
            max_px_err = max(max_px_err, float(err))
            sum_px_err2 += float(err * err)
            if sample_texture and texture_data is not None:
                sample_err = abs(float(pixels[iy, ix, 2]) - sample_texture_linear(texture_data, shader_texcoord, wrap_x=True))
                max_sample_err = max(max_sample_err, float(sample_err))
                sum_sample_err2 += float(sample_err * sample_err)
            if pixel_wcs is not None:
                world_xyz = displayLatitudinalWorld((sx, sy), LATI_SURFACE_BOUNDS_DEG)
                if math.isfinite(world_xyz[0]) and math.isfinite(world_xyz[1]) and math.isfinite(world_xyz[2]):
                    world_deg = (
                        math.degrees(math.atan2(world_xyz[0], world_xyz[2])),
                        math.degrees(math.asin(max(-1.0, min(1.0, world_xyz[1])))),
                    )
                    astro_px = pixel_wcs.wcs_world2pix([world_deg], 1)[0]
                    if np.all(np.isfinite(astro_px)):
                        shader_px = texcoord_to_pixel_center(shader_texcoord, meta.pixel_width, meta.pixel_height)
                        astro_pixel_center = (float(astro_px[0] - 0.5), float(astro_px[1] - 0.5))
                        astropy_err = pixel_center_error_px(shader_px, astro_pixel_center, meta)
                        max_shader_astropy_px_err = max(max_shader_astropy_px_err, float(astropy_err))
                        sum_shader_astropy_px_err2 += float(astropy_err * astropy_err)
                        astropy_count += 1
            count += 1

    print(f"file={fits_file}")
    print(f"mode=electron_surface_map_{'sample' if sample_texture else 'texcoord'}_compare size={render_size}")
    print(f"renderer={result['renderer']}")
    print(f"gl_errors=(clear={result.get('clearError')}, draw={result.get('drawError')}, read={result.get('readError')})")
    print_gl_setup_errors(result)
    print(f"valid_samples={count}")
    print(f"skipped_samples={skipped}")
    print(f"shader_only_samples={shader_only}")
    print(f"cpu_only_samples={cpu_only}")
    print(f"shader_vs_cpu_max_error_px={max_px_err:.6e}")
    print(f"shader_vs_cpu_rms_error_px={math.sqrt(sum_px_err2 / count):.6e}" if count > 0 else "shader_vs_cpu_rms_error_px=nan")
    print(f"shader_vs_astropy_samples={astropy_count}")
    print(f"shader_vs_astropy_max_error_px={max_shader_astropy_px_err:.6e}")
    print(f"shader_vs_astropy_rms_error_px={math.sqrt(sum_shader_astropy_px_err2 / astropy_count):.6e}" if astropy_count > 0 else "shader_vs_astropy_rms_error_px=nan")
    if sample_texture:
        print(f"sample_max_error={max_sample_err:.6e}")
        print(f"sample_rms_error={math.sqrt(sum_sample_err2 / count):.6e}" if count > 0 else "sample_rms_error=nan")
    print(f"electron_rgba32f={job['outputPath']}")
    if count == 0:
        print("FAILED: no comparable Electron WebGL samples")
        return 1
    if shader_only or cpu_only:
        print("FAILED: shader/CPU validity masks differ")
        return 1
    if max_px_err > max_error_px:
        print(f"FAILED: shader_vs_cpu_max_error_px exceeds {max_error_px:.6e}")
        return 1
    if astropy_count > 0 and max_shader_astropy_px_err > max_error_px:
        print(f"FAILED: shader_vs_astropy_max_error_px exceeds {max_error_px:.6e}")
        return 1
    if sample_texture and max_sample_err > max_sample_error:
        print(f"FAILED: sample_max_error exceeds {max_sample_error:.6e}")
        return 1
    return 0


def compare_surface_map_batch(
    electron: Path,
    output_dir: Path,
    render_size: int,
    max_error_px: float,
    max_sample_error: float,
    sample_texture: bool,
    backend: str,
) -> int:
    output_dir.mkdir(parents=True, exist_ok=True)

    jobs: list[dict] = []
    metadata: list[dict] = []
    for name, filename in SURFACE_MAP_CASES:
        fits_file = SCRIPT_DIR / "data" / filename
        image_data, meta, _projection_wcs, pixel_wcs = load_validation_context(fits_file, None)
        if not is_surface_map_projection(meta):
            raise ValueError(f"Electron WebGL surface-map validation does not support projection {meta.projection!r} for {fits_file}")
        job = common_job("lati_zenithal", fits_file, render_size, output_dir, meta, LATI_SURFACE_BOUNDS_DEG, sample_texture, backend, name)
        jobs.append(job)
        metadata.append({
            "fits_file": fits_file,
            "image_data": image_data,
            "meta": meta,
            "pixel_wcs": pixel_wcs,
        })

    results = run_electron_jobs(electron, jobs, backend)
    failed = False
    for job, info, result in zip(jobs, metadata, results, strict=True):
        pixels = read_job_pixels(job)
        code = evaluate_surface_map_shader_to_cpu(
            info["fits_file"],
            render_size,
            max_error_px,
            max_sample_error,
            sample_texture,
            info["image_data"],
            info["meta"],
            info["pixel_wcs"],
            job,
            result,
            pixels,
        )
        failed = failed or code != 0

    return 1 if failed else 0


def compare_surface_map_color_smoke_batch(
    electron: Path,
    output_dir: Path,
    render_size: int,
    backend: str,
    diff_mode: bool = False,
) -> int:
    output_dir.mkdir(parents=True, exist_ok=True)

    jobs: list[dict] = []
    metadata: list[dict] = []
    suffix = "color_diff_smoke" if diff_mode else "color_smoke"
    for name, filename in SURFACE_MAP_CASES:
        fits_file = SCRIPT_DIR / "data" / filename
        _image_data, meta, _projection_wcs, _pixel_wcs = load_validation_context(fits_file, None)
        if not is_surface_map_projection(meta):
            raise ValueError(f"Electron WebGL surface-map color smoke does not support projection {meta.projection!r} for {fits_file}")
        job = common_job(
            "lati_zenithal",
            fits_file,
            render_size,
            output_dir,
            meta,
            LATI_SURFACE_BOUNDS_DEG,
            True,
            backend,
            f"{name}_{suffix}",
            color_smoke=not diff_mode,
            color_diff_smoke=diff_mode,
        )
        jobs.append(job)
        metadata.append({"fits_file": fits_file})

    results = run_electron_jobs(electron, jobs, backend)
    failed = False
    for job, info, result in zip(jobs, metadata, results, strict=True):
        pixels = read_job_pixels(job)
        failed = failed or evaluate_color_smoke(info["fits_file"], render_size, job, result, pixels) != 0

    return 1 if failed else 0


def compare_surface_map_diff_selfcheck_batch(
    electron: Path,
    output_dir: Path,
    render_size: int,
    max_error_px: float,
    max_sample_error: float,
    backend: str,
    selfcheck_mode: str,
) -> int:
    output_dir.mkdir(parents=True, exist_ok=True)

    jobs: list[dict] = []
    metadata: list[dict] = []
    for name, filename in SURFACE_MAP_CASES:
        fits_file = SCRIPT_DIR / "data" / filename
        _image_data, meta, _projection_wcs, _pixel_wcs = load_validation_context(fits_file, None)
        if not is_surface_map_projection(meta):
            raise ValueError(f"Electron WebGL surface-map diff selfcheck does not support projection {meta.projection!r} for {fits_file}")
        job = common_job(
            "lati_zenithal",
            fits_file,
            render_size,
            output_dir,
            meta,
            LATI_SURFACE_BOUNDS_DEG,
            True,
            backend,
            f"{name}_diff_selfcheck",
            diff_selfcheck=True,
        )
        job["latiDiffSelfcheckMode"] = selfcheck_mode
        jobs.append(job)
        metadata.append({
            "fits_file": fits_file,
            "meta": meta,
        })

    results = run_electron_jobs(electron, jobs, backend)
    failed = False
    for job, info, result in zip(jobs, metadata, results, strict=True):
        pixels = read_job_pixels(job)
        code = evaluate_hpc_diff_selfcheck(
            info["fits_file"],
            render_size,
            max_error_px,
            max_sample_error,
            info["meta"],
            job,
            result,
            pixels,
        )
        failed = failed or code != 0

    return 1 if failed else 0


def evaluate_hpc_diff_selfcheck(
    fits_file: Path,
    render_size: int,
    max_error_px: float,
    max_sample_error: float,
    meta,
    job: dict,
    result: dict,
    pixels: np.ndarray,
) -> int:
    texture_data = synthetic_texture(job["textureWidth"], job["textureHeight"])
    max_px_err = 0.0
    sum_px_err2 = 0.0
    max_sample_err = 0.0
    sum_sample_err2 = 0.0
    count = 0
    skipped = 0

    for iy in range(render_size):
        for ix in range(render_size):
            texcoord = (float(pixels[iy, ix, 0]), float(pixels[iy, ix, 1]))
            diff_texcoord = (float(pixels[iy, ix, 2]), float(pixels[iy, ix, 3]))
            valid = is_finite_texcoord(texcoord) and is_finite_texcoord(diff_texcoord) and any(pixels[iy, ix] != 0.0)
            if not valid:
                skipped += 1
                continue

            px_err = pixel_error(texcoord, diff_texcoord, meta.pixel_width, meta.pixel_height)
            sample_err = abs(sample_texture_linear(texture_data, texcoord) - sample_texture_linear(texture_data, diff_texcoord))
            max_px_err = max(max_px_err, float(px_err))
            max_sample_err = max(max_sample_err, float(sample_err))
            sum_px_err2 += float(px_err * px_err)
            sum_sample_err2 += float(sample_err * sample_err)
            count += 1

    print(f"file={fits_file}")
    print(f"mode=electron_{job['mode']}_diff_selfcheck size={render_size}")
    print(f"renderer={result['renderer']}")
    print(f"gl_errors=(clear={result.get('clearError')}, draw={result.get('drawError')}, read={result.get('readError')})")
    print_gl_setup_errors(result)
    print(f"valid_samples={count}")
    print(f"skipped_samples={skipped}")
    print(f"diff_texcoord_max_error_px={max_px_err:.6e}")
    print(f"diff_texcoord_rms_error_px={math.sqrt(sum_px_err2 / count):.6e}" if count > 0 else "diff_texcoord_rms_error_px=nan")
    print(f"diff_sample_max_error={max_sample_err:.6e}")
    print(f"diff_sample_rms_error={math.sqrt(sum_sample_err2 / count):.6e}" if count > 0 else "diff_sample_rms_error=nan")
    print(f"electron_rgba32f={job['outputPath']}")
    if count == 0:
        print("FAILED: no valid Electron WebGL diff samples")
        return 1
    if max_px_err > max_error_px:
        print(f"FAILED: diff_texcoord_max_error_px exceeds {max_error_px:.6e}")
        return 1
    if max_sample_err > max_sample_error:
        print(f"FAILED: diff_sample_max_error exceeds {max_sample_error:.6e}")
        return 1
    return 0


def compare_hpc_diff_selfcheck(
    fits_file: Path,
    hdu: int | None,
    render_size: int,
    electron: Path,
    output_dir: Path,
    max_error_px: float,
    max_sample_error: float,
    backend: str,
) -> int:
    _image_data, meta, _projection_wcs, _pixel_wcs = load_validation_context(fits_file, hdu)
    if meta.projection not in PROJECTION_CODES:
        raise ValueError(f"Electron WebGL HPC diff selfcheck does not support projection {meta.projection!r}")

    output_dir.mkdir(parents=True, exist_ok=True)
    bounds = hpc_bounds_degrees(meta, 1.0)
    job = common_job("hpc", fits_file, render_size, output_dir, meta, bounds, True, backend, "hpc_diff_selfcheck", True)
    result, pixels = run_shader_job(electron, backend, job)
    return evaluate_hpc_diff_selfcheck(fits_file, render_size, max_error_px, max_sample_error, meta, job, result, pixels)


def compare_hpc_projection_diff_selfcheck_batch(
    electron: Path,
    output_dir: Path,
    render_size: int,
    max_error_px: float,
    max_sample_error: float,
    backend: str,
) -> int:
    output_dir.mkdir(parents=True, exist_ok=True)

    jobs: list[dict] = []
    metadata: list[dict] = []
    for name, filename, hdu, _case_max_error_px in HPC_PROJECTION_CASES:
        fits_file = SCRIPT_DIR / "data" / filename
        _image_data, meta, _projection_wcs, _pixel_wcs = load_validation_context(fits_file, hdu)
        if meta.projection not in PROJECTION_CODES:
            raise ValueError(f"Electron WebGL HPC diff selfcheck does not support projection {meta.projection!r} for {fits_file}")
        bounds = hpc_bounds_degrees(meta, 1.0)
        job = common_job("hpc", fits_file, render_size, output_dir, meta, bounds, True, backend, f"{name}_diff_selfcheck", True)
        jobs.append(job)
        metadata.append({
            "fits_file": fits_file,
            "meta": meta,
        })

    results = run_electron_jobs(electron, jobs, backend)
    failed = False
    for job, info, result in zip(jobs, metadata, results, strict=True):
        pixels = read_job_pixels(job)
        code = evaluate_hpc_diff_selfcheck(
            info["fits_file"],
            render_size,
            max_error_px,
            max_sample_error,
            info["meta"],
            job,
            result,
            pixels,
        )
        failed = failed or code != 0

    return 1 if failed else 0


def compare_hpc_projection_color_smoke_batch(
    electron: Path,
    output_dir: Path,
    render_size: int,
    backend: str,
    diff_mode: bool = False,
) -> int:
    output_dir.mkdir(parents=True, exist_ok=True)

    jobs: list[dict] = []
    metadata: list[dict] = []
    for name, filename, hdu, _case_max_error_px in HPC_PROJECTION_CASES:
        fits_file = SCRIPT_DIR / "data" / filename
        _image_data, meta, _projection_wcs, _pixel_wcs = load_validation_context(fits_file, hdu)
        if meta.projection not in PROJECTION_CODES:
            raise ValueError(f"Electron WebGL HPC color smoke does not support projection {meta.projection!r} for {fits_file}")
        bounds = hpc_bounds_degrees(meta, 1.0)
        suffix = "color_diff_smoke" if diff_mode else "color_smoke"
        job = common_job(
            "hpc",
            fits_file,
            render_size,
            output_dir,
            meta,
            bounds,
            True,
            backend,
            f"{name}_{suffix}",
            color_smoke=not diff_mode,
            color_diff_smoke=diff_mode,
        )
        jobs.append(job)
        metadata.append({"fits_file": fits_file})

    results = run_electron_jobs(electron, jobs, backend)
    failed = False
    for job, info, result in zip(jobs, metadata, results, strict=True):
        pixels = read_job_pixels(job)
        failed = failed or evaluate_color_smoke(info["fits_file"], render_size, job, result, pixels) != 0

    return 1 if failed else 0


def compare_all_modes_diff_selfcheck(
    fits_file: Path,
    hdu: int | None,
    render_size: int,
    electron: Path,
    output_dir: Path,
    max_error_px: float,
    max_sample_error: float,
    backend: str,
) -> int:
    _image_data, meta, _projection_wcs, _pixel_wcs = load_validation_context(fits_file, hdu)
    if meta.projection not in PROJECTION_CODES:
        raise ValueError(f"Electron WebGL all-modes diff selfcheck does not support projection {meta.projection!r}")

    output_dir.mkdir(parents=True, exist_ok=True)
    bounds = hpc_bounds_degrees(meta, 1.0)
    jobs = [
        common_job("hpc", fits_file, render_size, output_dir, meta, bounds, True, backend, "hpc_diff_selfcheck", True),
        *[
            make_mode_job(mode, fits_file, render_size, output_dir, True, meta, backend, True)
            for mode in ALL_MODES
            if mode != "hpc"
        ],
    ]

    results = run_electron_jobs(electron, jobs, backend)
    failed = False
    for job, result in zip(jobs, results, strict=True):
        pixels = read_job_pixels(job)
        code = evaluate_hpc_diff_selfcheck(
            fits_file,
            render_size,
            max_error_px,
            max_sample_error,
            meta,
            job,
            result,
            pixels,
        )
        failed = failed or code != 0

    return 1 if failed else 0


def evaluate_color_smoke(
    fits_file: Path,
    render_size: int,
    job: dict,
    result: dict,
    pixels: np.ndarray,
) -> int:
    alpha = pixels[:, :, 3]
    rendered = alpha > 0.5
    rendered_pixels = pixels[rendered]
    count = int(rendered_pixels.shape[0])
    skipped = int(render_size * render_size - count)
    finite = bool(np.all(np.isfinite(rendered_pixels))) if count else False
    in_range = bool(np.all((rendered_pixels >= -1e-5) & (rendered_pixels <= 1.00001))) if count else False

    print(f"file={fits_file}")
    mode_suffix = "color_diff_smoke" if job.get("colorDiffSmoke") else "color_smoke"
    print(f"mode=electron_{job['mode']}_{mode_suffix} size={render_size}")
    print(f"renderer={result['renderer']}")
    print(f"gl_errors=(clear={result.get('clearError')}, draw={result.get('drawError')}, read={result.get('readError')})")
    print_gl_setup_errors(result)
    print(f"rendered_pixels={count}")
    print(f"skipped_pixels={skipped}")
    print(f"finite_pixels={finite}")
    print(f"in_range_pixels={in_range}")
    print(f"electron_rgba32f={job['outputPath']}")
    if count == 0:
        print("FAILED: no rendered color pixels")
        return 1
    if not finite:
        print("FAILED: non-finite rendered color pixels")
        return 1
    if not in_range:
        print("FAILED: rendered color pixels outside [0, 1]")
        return 1
    return 0


def compare_all_modes_color_smoke(
    fits_file: Path,
    hdu: int | None,
    render_size: int,
    electron: Path,
    output_dir: Path,
    backend: str,
    diff_mode: bool = False,
) -> int:
    _image_data, meta, _projection_wcs, _pixel_wcs = load_validation_context(fits_file, hdu)
    if meta.projection not in PROJECTION_CODES:
        raise ValueError(f"Electron WebGL all-modes color smoke does not support projection {meta.projection!r}")

    output_dir.mkdir(parents=True, exist_ok=True)
    bounds = hpc_bounds_degrees(meta, 1.0)
    suffix = "color_diff_smoke" if diff_mode else "color_smoke"
    jobs = [
        common_job(
            "hpc",
            fits_file,
            render_size,
            output_dir,
            meta,
            bounds,
            True,
            backend,
            f"hpc_{suffix}",
            color_smoke=not diff_mode,
            color_diff_smoke=diff_mode,
        ),
        *[
            make_mode_job(
                mode,
                fits_file,
                render_size,
                output_dir,
                True,
                meta,
                backend,
                color_smoke=not diff_mode,
                color_diff_smoke=diff_mode,
            )
            for mode in ALL_MODES
            if mode != "hpc"
        ],
    ]

    results = run_electron_jobs(electron, jobs, backend)
    failed = False
    for job, result in zip(jobs, results, strict=True):
        pixels = read_job_pixels(job)
        failed = failed or evaluate_color_smoke(fits_file, render_size, job, result, pixels) != 0

    return 1 if failed else 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate JHV GLSL WCS code by running it on Electron/WebGL")
    parser.add_argument("fits_file", type=Path, nargs="?")
    parser.add_argument("--hdu", type=int, default=None)
    parser.add_argument("--render-size", type=int, default=512)
    parser.add_argument("--electron", type=Path, default=DEFAULT_ELECTRON)
    parser.add_argument("--output-dir", type=Path, default=Path("extra/test/out"))
    parser.add_argument("--max-error-px", type=float, default=0.5)
    parser.add_argument("--max-sample-error", type=float, default=1e-3)
    parser.add_argument("--sample-texture", action="store_true")
    parser.add_argument("--all-modes", action="store_true")
    parser.add_argument("--tan-all-modes-cases", action="store_true")
    parser.add_argument("--tan-screen-cases", action="store_true")
    parser.add_argument("--hpc-projection-cases", action="store_true")
    parser.add_argument("--hpc-render-cases", action="store_true")
    parser.add_argument("--surface-map-cases", action="store_true")
    parser.add_argument("--hpc-projection-cases-diff-selfcheck", action="store_true")
    parser.add_argument("--hpc-projection-cases-color-smoke", action="store_true")
    parser.add_argument("--hpc-projection-cases-color-diff-smoke", action="store_true")
    parser.add_argument("--tan-screen-cases-color-smoke", action="store_true")
    parser.add_argument("--tan-screen-cases-color-diff-smoke", action="store_true")
    parser.add_argument("--tan-all-modes-cases-color-smoke", action="store_true")
    parser.add_argument("--tan-all-modes-cases-color-diff-smoke", action="store_true")
    parser.add_argument("--surface-map-cases-color-smoke", action="store_true")
    parser.add_argument("--surface-map-cases-color-diff-smoke", action="store_true")
    parser.add_argument("--surface-map-cases-diff-selfcheck", action="store_true")
    parser.add_argument("--surface-map-diff-selfcheck-mode", choices=("normal", "same-slot", "assign"), default="normal")
    parser.add_argument("--hpc-diff-selfcheck", action="store_true")
    parser.add_argument("--all-modes-diff-selfcheck", action="store_true")
    parser.add_argument("--all-modes-color-smoke", action="store_true")
    parser.add_argument("--all-modes-color-diff-smoke", action="store_true")
    parser.add_argument("--backend", choices=("default", "swiftshader"), default="default")
    parser.add_argument(
        "--mode",
        choices=ALL_MODES,
        default="hpc",
    )
    args = parser.parse_args()

    if args.hpc_projection_cases:
        return compare_hpc_projection_batch(
            args.electron,
            args.output_dir,
            args.render_size,
            args.max_error_px,
            args.max_sample_error,
            args.sample_texture,
            args.backend,
        )

    if args.tan_all_modes_cases:
        return compare_tan_all_modes_case_batch(
            args.electron,
            args.output_dir,
            args.render_size,
            args.max_error_px,
            args.max_sample_error,
            args.sample_texture,
            args.backend,
        )

    if args.tan_screen_cases:
        return compare_tan_screen_case_batch(
            args.electron,
            args.output_dir,
            args.render_size,
            args.max_error_px,
            args.max_sample_error,
            args.sample_texture,
            args.backend,
        )

    if args.hpc_render_cases:
        return compare_hpc_render_case_batch(
            args.electron,
            args.output_dir,
            args.render_size,
            args.max_error_px,
            args.max_sample_error,
            args.sample_texture,
            args.backend,
        )

    if args.surface_map_cases:
        return compare_surface_map_batch(
            args.electron,
            args.output_dir,
            args.render_size,
            args.max_error_px,
            args.max_sample_error,
            args.sample_texture,
            args.backend,
        )

    if args.hpc_projection_cases_diff_selfcheck:
        return compare_hpc_projection_diff_selfcheck_batch(
            args.electron,
            args.output_dir,
            args.render_size,
            args.max_error_px,
            args.max_sample_error,
            args.backend,
        )

    if args.hpc_projection_cases_color_smoke:
        return compare_hpc_projection_color_smoke_batch(
            args.electron,
            args.output_dir,
            args.render_size,
            args.backend,
        )

    if args.hpc_projection_cases_color_diff_smoke:
        return compare_hpc_projection_color_smoke_batch(
            args.electron,
            args.output_dir,
            args.render_size,
            args.backend,
            True,
        )

    if args.tan_screen_cases_color_smoke:
        return compare_tan_screen_color_smoke_batch(
            args.electron,
            args.output_dir,
            args.render_size,
            args.backend,
        )

    if args.tan_screen_cases_color_diff_smoke:
        return compare_tan_screen_color_smoke_batch(
            args.electron,
            args.output_dir,
            args.render_size,
            args.backend,
            True,
        )

    if args.tan_all_modes_cases_color_smoke:
        return compare_tan_all_modes_color_smoke_batch(
            args.electron,
            args.output_dir,
            args.render_size,
            args.backend,
        )

    if args.tan_all_modes_cases_color_diff_smoke:
        return compare_tan_all_modes_color_smoke_batch(
            args.electron,
            args.output_dir,
            args.render_size,
            args.backend,
            True,
        )

    if args.surface_map_cases_color_smoke:
        return compare_surface_map_color_smoke_batch(
            args.electron,
            args.output_dir,
            args.render_size,
            args.backend,
        )

    if args.surface_map_cases_color_diff_smoke:
        return compare_surface_map_color_smoke_batch(
            args.electron,
            args.output_dir,
            args.render_size,
            args.backend,
            True,
        )

    if args.surface_map_cases_diff_selfcheck:
        return compare_surface_map_diff_selfcheck_batch(
            args.electron,
            args.output_dir,
            args.render_size,
            args.max_error_px,
            args.max_sample_error,
            args.backend,
            args.surface_map_diff_selfcheck_mode,
        )

    if args.fits_file is None:
        raise SystemExit("fits_file is required unless a projection-case batch option is used")

    if args.hpc_diff_selfcheck:
        return compare_hpc_diff_selfcheck(
            args.fits_file,
            args.hdu,
            args.render_size,
            args.electron,
            args.output_dir,
            args.max_error_px,
            args.max_sample_error,
            args.backend,
        )

    if args.all_modes_diff_selfcheck:
        return compare_all_modes_diff_selfcheck(
            args.fits_file,
            args.hdu,
            args.render_size,
            args.electron,
            args.output_dir,
            args.max_error_px,
            args.max_sample_error,
            args.backend,
        )

    if args.all_modes_color_smoke:
        return compare_all_modes_color_smoke(
            args.fits_file,
            args.hdu,
            args.render_size,
            args.electron,
            args.output_dir,
            args.backend,
        )

    if args.all_modes_color_diff_smoke:
        return compare_all_modes_color_smoke(
            args.fits_file,
            args.hdu,
            args.render_size,
            args.electron,
            args.output_dir,
            args.backend,
            True,
        )

    if args.all_modes:
        return compare_batch(
            args.fits_file,
            args.hdu,
            args.render_size,
            args.electron,
            args.output_dir,
            args.max_error_px,
            args.max_sample_error,
            args.sample_texture,
            args.backend,
        )

    if args.mode != "hpc":
        return compare_shader_to_cpu(
            args.mode,
            args.fits_file,
            args.hdu,
            args.render_size,
            args.electron,
            args.output_dir,
            args.max_error_px,
            args.max_sample_error,
            args.sample_texture,
            args.backend,
        )

    return compare_hpc_shader_to_astropy(
        args.fits_file,
        args.hdu,
        args.render_size,
        args.electron,
        args.output_dir,
        args.max_error_px,
        args.max_sample_error,
        args.sample_texture,
        args.backend,
    )


if __name__ == "__main__":
    sys.exit(main())
