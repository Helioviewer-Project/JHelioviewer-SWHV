#!/usr/bin/env python3

from __future__ import annotations

import argparse
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class ValidationRun:
    name: str
    args: tuple[str, ...]


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent.parent
VALIDATOR = SCRIPT_DIR / "validate_jhv_wcs_against_astropy.py"
DATA = SCRIPT_DIR / "data"


# Keep this list in the same order as the validation note. It should cover both
# the documented mode examples and the representative result cases quoted in the
# Results section.
RUNS: tuple[ValidationRun, ...] = (
    ValidationRun(
        "forward_wcs_random_sample",
        (str(DATA / "20241224_194245_d4c2A.fts"),),
    ),
    ValidationRun(
        "full_pixel_center",
        (str(DATA / "20250622_000831_s4h1A.fts"), "--all-pixels"),
    ),
    ValidationRun(
        "inverse_tan",
        (str(DATA / "sample.171.fits"), "--hdu", "1", "--inverse-tan"),
    ),
    ValidationRun(
        "inverse_azp",
        (str(DATA / "20250622_000831_s4h1A.fts"), "--inverse-azp"),
    ),
    ValidationRun(
        "inverse_zpn",
        (str(DATA / "psp_L3_wispr_20231227T150704_V1_2222.fits"), "--inverse-zpn"),
    ),
    ValidationRun(
        "hpc_render_compare",
        (str(DATA / "20241224_194245_d4c2A.fts"), "--hpc-render-compare", "--render-size", "2048"),
    ),
    ValidationRun(
        "hpc_render_compare_hi1",
        (str(DATA / "20250622_000831_s4h1A.fts"), "--hpc-render-compare", "--render-size", "2048"),
    ),
    ValidationRun(
        "hpc_render_compare_hi2",
        (str(DATA / "20250622_000851_s4h2A.fts"), "--hpc-render-compare", "--render-size", "2048"),
    ),
    ValidationRun(
        "hpc_render_compare_wispr_1211",
        (str(DATA / "psp_L3_wispr_20231227T150508_V1_1211.fits"), "--hpc-render-compare", "--render-size", "2048"),
    ),
    ValidationRun(
        "hpc_render_compare_wispr_2222",
        (str(DATA / "psp_L3_wispr_20231227T150704_V1_2222.fits"), "--hpc-render-compare", "--render-size", "2048"),
    ),
    ValidationRun(
        "forward_car_sunerf",
        (str(DATA / "sunerf_map.fits"),),
    ),
    ValidationRun(
        "all_pixels_car_sunerf",
        (str(DATA / "sunerf_map.fits"), "--all-pixels"),
    ),
    ValidationRun(
        "inverse_car_sunerf",
        (str(DATA / "sunerf_map.fits"), "--inverse-car"),
    ),
    ValidationRun(
        "forward_car_hmi",
        (str(DATA / "syn_HMI_hmi.m_720s_2026-02-25T00-00-00_a_V1.fits"),),
    ),
    ValidationRun(
        "all_pixels_car_hmi",
        (str(DATA / "syn_HMI_hmi.m_720s_2026-02-25T00-00-00_a_V1.fits"), "--all-pixels"),
    ),
    ValidationRun(
        "inverse_car_hmi",
        (str(DATA / "syn_HMI_hmi.m_720s_2026-02-25T00-00-00_a_V1.fits"), "--inverse-car"),
    ),
    ValidationRun(
        "forward_cea",
        (str(DATA / "mrzqs260301t2314c2308_169.fits"),),
    ),
    ValidationRun(
        "all_pixels_cea",
        (str(DATA / "mrzqs260301t2314c2308_169.fits"), "--all-pixels"),
    ),
    ValidationRun(
        "inverse_cea",
        (str(DATA / "mrzqs260301t2314c2308_169.fits"), "--inverse-cea"),
    ),
    ValidationRun(
        "ortho_vs_hpc_screen_compare",
        (str(DATA / "sample.171.fits"), "--hdu", "1", "--ortho-vs-hpc-screen-compare", "--render-size", "4096"),
    ),
    ValidationRun(
        "ortho_vs_hpc_screen_compare_solo_eui",
        (str(DATA / "solo_L2_eui-fsi174-image_20251002T150055171_V00.fits"), "--ortho-vs-hpc-screen-compare", "--render-size", "4096"),
    ),
    ValidationRun(
        "ortho_vs_hpc_screen_compare_cor2",
        (str(DATA / "20241224_194245_d4c2A.fts"), "--ortho-vs-hpc-screen-compare", "--render-size", "4096"),
    ),
    ValidationRun(
        "initial_tan_vs_hpc",
        (str(DATA / "sample.171.fits"), "--hdu", "1", "--compare-initial-tan-vs-hpc"),
    ),
    ValidationRun(
        "initial_tan_vs_hpc_solo_eui",
        (str(DATA / "solo_L2_eui-fsi174-image_20251002T150055171_V00.fits"), "--compare-initial-tan-vs-hpc"),
    ),
    ValidationRun(
        "initial_tan_vs_hpc_cor2",
        (str(DATA / "20241224_194245_d4c2A.fts"), "--compare-initial-tan-vs-hpc"),
    ),
    ValidationRun(
        "initial_tan_image_frame",
        (str(DATA / "sample.171.fits"), "--hdu", "1", "--compare-initial-tan-image-frame"),
    ),
    ValidationRun(
        "initial_tan_image_frame_solo_eui",
        (str(DATA / "solo_L2_eui-fsi174-image_20251002T150055171_V00.fits"), "--compare-initial-tan-image-frame"),
    ),
    ValidationRun(
        "initial_tan_image_frame_cor2",
        (str(DATA / "20241224_194245_d4c2A.fts"), "--compare-initial-tan-image-frame"),
    ),
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run the validation suite documented in docs/wcs-validation/jhv_wcs_hpc_validation_note.md"
    )
    parser.add_argument(
        "--list",
        action="store_true",
        help="List the named runs in the suite and exit",
    )
    parser.add_argument(
        "--only",
        nargs="+",
        metavar="RUN",
        help="Run only the named subset",
    )
    parser.add_argument(
        "--keep-going",
        action="store_true",
        help="Continue after failures and report them at the end",
    )
    return parser.parse_args()


def selected_runs(only: list[str] | None) -> list[ValidationRun]:
    if not only:
        return list(RUNS)
    requested = set(only)
    known = {run.name for run in RUNS}
    missing = sorted(requested - known)
    if missing:
        raise SystemExit(f"Unknown run name(s): {', '.join(missing)}")
    return [run for run in RUNS if run.name in requested]


def run_case(run: ValidationRun) -> int:
    cmd = [sys.executable, str(VALIDATOR), *run.args]
    print(f"\n== {run.name} ==")
    print(" ".join(cmd))
    completed = subprocess.run(cmd, cwd=REPO_ROOT)
    return completed.returncode


def main() -> int:
    args = parse_args()
    if args.list:
        for run in RUNS:
            print(run.name)
        return 0

    runs = selected_runs(args.only)
    failures: list[str] = []
    for run in runs:
        code = run_case(run)
        if code == 0:
            continue
        failures.append(run.name)
        if not args.keep_going:
            return code

    if failures:
        print("\nFAILED:")
        for name in failures:
            print(f"- {name}")
        return 1

    print(f"\nAll {len(runs)} validation run(s) passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
