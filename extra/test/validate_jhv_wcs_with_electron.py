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
    LOGPOLAR_MIN_RADIUS,
    hpc_bounds_degrees,
    load_validation_context,
    renderHpcTexcoordsFloat32,
    renderHpcTexcoords,
    renderLatitudinalTexcoords,
    renderOrthographicTexcoords,
    renderPolarTexcoords,
    sample_texture_linear,
    texcoord_to_pixel_center,
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
ALL_MODES = ("hpc", "ortho", "lati_zenithal", "polar", "logpolar")
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
}


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
) -> dict:
    output_kind = "sample" if sample_texture else "texcoord"
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
    if mode == "polar":
        texcoord, _ = renderPolarTexcoords((sx, sy), sy, meta, image_data)
        return texcoord
    if mode == "logpolar":
        radial = math.exp(math.log(LOGPOLAR_MIN_RADIUS) + sy * (math.log(1.0) - math.log(LOGPOLAR_MIN_RADIUS)))
        texcoord, _ = renderPolarTexcoords((sx, sy), radial, meta, image_data, logpolar=True)
        return texcoord
    raise ValueError(f"Unsupported Electron WebGL mode: {mode}")


def bounds_for_mode(mode: str) -> tuple[float, float, float, float]:
    if mode == "lati_zenithal":
        return LATI_ZENITHAL_BOUNDS_DEG
    if mode == "polar":
        return (0.0, 1.0, 0.0, 1.0)
    if mode == "logpolar":
        return (0.0, 1.0, math.log(LOGPOLAR_MIN_RADIUS), math.log(1.0))
    return (0.0, 1.0, 0.0, 1.0)


def make_mode_job(
    mode: str,
    fits_file: Path,
    render_size: int,
    output_dir: Path,
    sample_texture: bool,
    meta,
    backend: str,
) -> dict:
    job = common_job(mode, fits_file, render_size, output_dir, meta, bounds_for_mode(mode), sample_texture, backend)
    if mode == "lati_zenithal":
        job["latiGrid"] = [0.0, 0.0, 0.0]
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
    image_data, meta, _projection_wcs, _pixel_wcs = load_validation_context(fits_file, hdu)
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

    for iy in range(render_size):
        sy = (iy + 0.5) / render_size
        for ix in range(render_size):
            sx = (ix + 0.5) / render_size
            shader_texcoord = (float(pixels[iy, ix, 0]), float(pixels[iy, ix, 1]))
            shader_valid = float(pixels[iy, ix, 3]) > 0.5 and is_finite_texcoord(shader_texcoord)
            cpu_texcoord = cpu_texcoord_for_mode(mode, sx, sy, meta, image_data)
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

            err = pixel_error(shader_texcoord, cpu_texcoord, meta.pixel_width, meta.pixel_height)
            max_px_err = max(max_px_err, float(err))
            sum_px_err2 += float(err * err)
            if sample_texture and texture_data is not None:
                shader_sample = float(pixels[iy, ix, 2])
                cpu_sample = sample_texture_linear(texture_data, shader_texcoord)
                sample_err = abs(shader_sample - cpu_sample)
                max_sample_err = max(max_sample_err, float(sample_err))
                sum_sample_err2 += float(sample_err * sample_err)
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
    image_data, meta, _projection_wcs, pixel_wcs = load_validation_context(fits_file, hdu)
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
            metadata.append({"mode": mode, "bounds": bounds})
        else:
            if mode == "ortho" and meta.projection not in PROJECTION_CODES:
                continue
            job = make_mode_job(mode, fits_file, render_size, output_dir, sample_texture, meta, backend)
            jobs.append(job)
            metadata.append({"mode": mode})

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
                pixel_wcs,
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
                job,
                result,
                pixels,
            )
        failed = failed or code != 0

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
    print(f"mode=electron_hpc_diff_selfcheck size={render_size}")
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
    parser.add_argument("--hpc-projection-cases", action="store_true")
    parser.add_argument("--hpc-diff-selfcheck", action="store_true")
    parser.add_argument("--backend", choices=("default", "swiftshader"), default="default")
    parser.add_argument(
        "--mode",
        choices=("hpc", "ortho", "lati_zenithal", "polar", "logpolar"),
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

    if args.fits_file is None:
        raise SystemExit("fits_file is required unless --hpc-projection-cases is used")

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
