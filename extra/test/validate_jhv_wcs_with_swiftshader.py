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
    hpc_bounds_degrees,
    load_validation_context,
    renderHpcTexcoords,
    texcoord_to_pixel_center,
    wcsRect,
    zpn_primary_branch_upper_eta,
)


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent.parent
RUNNER_DIR = SCRIPT_DIR / "swiftshader_webgl_runner"
DEFAULT_ELECTRON = Path(os.environ.get(
    "JHV_ELECTRON",
    str(Path.home() / "electron-v42.1.0-darwin-arm64/Electron.app/Contents/MacOS/Electron"),
))

PROJECTION_CODES = {
    "TAN": 0.0,
    "ARC": 1.0,
    "AZP": 2.0,
    "ZPN": 3.0,
}


def crota_quat(crota_rad: float) -> list[float]:
    half = 0.5 * crota_rad
    return [0.0, 0.0, math.sin(half), math.cos(half)]


def run_electron(electron: Path, job_path: Path) -> dict:
    completed = subprocess.run(
        [str(electron), str(RUNNER_DIR), str(job_path)],
        cwd=REPO_ROOT,
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
            "Electron SwiftShader runner failed\n"
            f"returncode={completed.returncode}\n"
            f"stdout={completed.stdout}\n"
            f"stderr={completed.stderr}"
        )
    if not result.get("ok"):
        raise RuntimeError(
            "Electron SwiftShader runner reported failure\n"
            f"result={json.dumps(result, indent=2)}\n"
            f"stderr={completed.stderr}"
        )
    return result


def compare_hpc_shader_to_astropy(
    fits_file: Path,
    hdu: int | None,
    render_size: int,
    electron: Path,
    output_dir: Path,
    max_error_px: float,
) -> int:
    image_data, meta, _projection_wcs, pixel_wcs = load_validation_context(fits_file, hdu)
    if meta.projection not in PROJECTION_CODES:
        raise ValueError(f"SwiftShader HPC validation does not support projection {meta.projection!r}")

    output_dir.mkdir(parents=True, exist_ok=True)
    bounds_deg = hpc_bounds_degrees(meta, 1.0)
    output_path = output_dir / f"{fits_file.stem}_swiftshader_hpc_texcoord.rgba32f"

    job = {
        "repoRoot": str(REPO_ROOT),
        "width": render_size,
        "height": render_size,
        "outputPath": str(output_path),
        "boundsDeg": list(bounds_deg),
        "rect": list(wcsRect(meta)),
        "crota": crota_quat(meta.crota_rad),
        "crval": [meta.crval_internal_x, meta.crval_internal_y],
        "zpnUpperEta": zpn_primary_branch_upper_eta(meta) if meta.projection == "ZPN" else 0.0,
        "projectionCode": PROJECTION_CODES[meta.projection],
        "planeUnitsPerRadian": meta.plane_units_per_rad,
        "observerDistance": meta.observer_distance,
        "pv2": list(meta.pv2),
    }

    with TemporaryDirectory() as temp_dir:
        job_path = Path(temp_dir) / "swiftshader-job.json"
        job_path.write_text(json.dumps(job))
        result = run_electron(electron, job_path)

    pixels = np.fromfile(output_path, dtype=np.float32)
    expected = render_size * render_size * 4
    if pixels.size != expected:
        raise RuntimeError(f"Expected {expected} float values from SwiftShader, got {pixels.size}")
    pixels = pixels.reshape((render_size, render_size, 4))

    max_px_err = 0.0
    sum_px_err2 = 0.0
    max_shader_cpu_px_err = 0.0
    sum_shader_cpu_px_err2 = 0.0
    max_cpu_astropy_px_err = 0.0
    sum_cpu_astropy_px_err2 = 0.0
    count = 0
    cpu_compare_count = 0
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

            err = max(abs(shader_px[0] - astro_px[ix, 0]), abs(shader_px[1] - astro_px[ix, 1]))
            max_px_err = max(max_px_err, float(err))
            sum_px_err2 += float(err * err)
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

    print(f"file={fits_file}")
    print(f"mode=swiftshader_hpc_render_compare size={render_size}")
    print(f"renderer={result['renderer']}")
    print(f"gl_errors=(clear={result.get('clearError')}, draw={result.get('drawError')}, read={result.get('readError')})")
    print(f"bounds_deg=({bounds_deg[0]:.12f}, {bounds_deg[1]:.12f}, {bounds_deg[2]:.12f}, {bounds_deg[3]:.12f})")
    print(f"valid_samples={count}")
    print(f"skipped_samples={skipped}")
    print(f"pixel_center_max_error_px={max_px_err:.6e}")
    print(f"pixel_center_rms_error_px={math.sqrt(sum_px_err2 / count):.6e}" if count > 0 else "pixel_center_rms_error_px=nan")
    print(f"cpu_comparable_samples={cpu_compare_count}")
    print(f"shader_vs_cpu_max_error_px={max_shader_cpu_px_err:.6e}")
    print(f"shader_vs_cpu_rms_error_px={math.sqrt(sum_shader_cpu_px_err2 / cpu_compare_count):.6e}" if cpu_compare_count > 0 else "shader_vs_cpu_rms_error_px=nan")
    print(f"cpu_vs_astropy_max_error_px={max_cpu_astropy_px_err:.6e}")
    print(f"cpu_vs_astropy_rms_error_px={math.sqrt(sum_cpu_astropy_px_err2 / cpu_compare_count):.6e}" if cpu_compare_count > 0 else "cpu_vs_astropy_rms_error_px=nan")
    print(f"swiftshader_rgba32f={output_path}")
    if count == 0:
        print("FAILED: no valid SwiftShader samples")
        return 1
    if max_px_err > max_error_px:
        print(f"FAILED: pixel_center_max_error_px exceeds {max_error_px:.6e}")
        return 1
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate JHV GLSL WCS code by running it on Electron/ANGLE SwiftShader")
    parser.add_argument("fits_file", type=Path)
    parser.add_argument("--hdu", type=int, default=None)
    parser.add_argument("--render-size", type=int, default=512)
    parser.add_argument("--electron", type=Path, default=DEFAULT_ELECTRON)
    parser.add_argument("--output-dir", type=Path, default=Path("extra/test/out"))
    parser.add_argument("--max-error-px", type=float, default=0.5)
    args = parser.parse_args()

    return compare_hpc_shader_to_astropy(
        args.fits_file,
        args.hdu,
        args.render_size,
        args.electron,
        args.output_dir,
        args.max_error_px,
    )


if __name__ == "__main__":
    sys.exit(main())
