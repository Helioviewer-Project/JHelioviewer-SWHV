#!/usr/bin/env python3

from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
import tempfile
from concurrent.futures import FIRST_COMPLETED, Future, ThreadPoolExecutor, wait
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class ValidationRun:
    name: str
    args: tuple[str, ...]
    validator: str = "wcs"


@dataclass(frozen=True)
class ValidationResult:
    run: ValidationRun
    returncode: int
    stdout: str
    stderr: str


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent.parent
VALIDATOR = SCRIPT_DIR / "validate_jhv_wcs_against_astropy.py"
GLSL_VALIDATOR = SCRIPT_DIR / "validate_glsl_syntax.py"
ELECTRON_VALIDATOR = SCRIPT_DIR / "validate_jhv_wcs_with_electron.py"
JAVA_METADATA_VALIDATOR = SCRIPT_DIR / "compare_java_metadata_to_validator.py"
DATA = SCRIPT_DIR / "data"


# Keep this list in the same order as the validation note. It should cover both
# the documented mode examples and the representative result cases quoted in the
# Results section.
RUNS: tuple[ValidationRun, ...] = (
    ValidationRun(
        "glsl_syntax",
        (),
        "glsl",
    ),
    ValidationRun(
        "java_metadata",
        (),
        "java_metadata",
    ),
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
        "forward_arc_punch",
        (str(DATA / "PUNCH_L3_CAM_20260425001600_v0k.fits"), "--hdu", "1"),
    ),
    ValidationRun(
        "all_pixels_arc_punch",
        (str(DATA / "PUNCH_L3_CAM_20260425001600_v0k.fits"), "--hdu", "1", "--all-pixels"),
    ),
    ValidationRun(
        "inverse_arc_punch",
        (str(DATA / "PUNCH_L3_CAM_20260425001600_v0k.fits"), "--hdu", "1", "--inverse-arc"),
    ),
    ValidationRun(
        "hpc_render_compare_arc_punch",
        (str(DATA / "PUNCH_L3_CAM_20260425001600_v0k.fits"), "--hdu", "1", "--hpc-render-compare", "--render-size", "2048"),
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
        "all_pixels_zpn_wispr_1211",
        (str(DATA / "psp_L3_wispr_20231227T150508_V1_1211.fits"), "--all-pixels"),
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
        "forward_car_aia",
        (str(DATA / "syn_AIA_171_2026-01-12T00-00-00_f_V3.fits"),),
    ),
    ValidationRun(
        "all_pixels_car_aia",
        (str(DATA / "syn_AIA_171_2026-01-12T00-00-00_f_V3.fits"), "--all-pixels"),
    ),
    ValidationRun(
        "inverse_car_aia",
        (str(DATA / "syn_AIA_171_2026-01-12T00-00-00_f_V3.fits"), "--inverse-car"),
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
        "surface_map_render_compare_car_sunerf",
        (str(DATA / "sunerf_map.fits"), "--surface-map-render-compare", "--surface-map-grid-factor", "4"),
    ),
    ValidationRun(
        "surface_map_render_compare_car_hmi",
        (str(DATA / "syn_HMI_hmi.m_720s_2026-02-25T00-00-00_a_V1.fits"), "--surface-map-render-compare", "--surface-map-grid-factor", "4"),
    ),
    ValidationRun(
        "surface_map_render_compare_car_aia",
        (str(DATA / "syn_AIA_171_2026-01-12T00-00-00_f_V3.fits"), "--surface-map-render-compare", "--surface-map-grid-factor", "4"),
    ),
    ValidationRun(
        "surface_map_render_compare_cea",
        (str(DATA / "mrzqs260301t2314c2308_169.fits"), "--surface-map-render-compare", "--surface-map-grid-factor", "4"),
    ),
    ValidationRun(
        "latitudinal_zenithal_render",
        (str(DATA / "sample.171.fits"), "--hdu", "1", "--latitudinal-zenithal-render", "--render-size", "512"),
    ),
    ValidationRun(
        "hpc_diff_selfcheck",
        (str(DATA / "20241224_194245_d4c2A.fts"), "--hpc-diff-selfcheck", "--render-size", "512"),
    ),
    ValidationRun(
        "latitudinal_diff_selfcheck",
        (str(DATA / "sunerf_map.fits"), "--latitudinal-diff-selfcheck", "--render-size", "512"),
    ),
    ValidationRun(
        "orthographic_diff_selfcheck",
        (str(DATA / "sample.171.fits"), "--hdu", "1", "--orthographic-diff-selfcheck", "--render-size", "512"),
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

ELECTRON_RUNS: tuple[ValidationRun, ...] = (
    ValidationRun(
        "electron_default_all_modes",
        (str(DATA / "sample.171.fits"), "--hdu", "1", "--render-size", "256", "--all-modes", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_all_modes_sample_texture",
        (str(DATA / "sample.171.fits"), "--hdu", "1", "--render-size", "128", "--all-modes", "--sample-texture", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_tan_screen_cases",
        ("--tan-screen-cases", "--render-size", "256", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_tan_screen_cases_sample_texture",
        ("--tan-screen-cases", "--render-size", "128", "--sample-texture", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_tan_all_modes_cases",
        ("--tan-all-modes-cases", "--render-size", "256", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_tan_all_modes_cases_sample_texture",
        ("--tan-all-modes-cases", "--render-size", "128", "--sample-texture", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_tan_all_modes_cases_color_smoke",
        ("--tan-all-modes-cases-color-smoke", "--render-size", "128", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_hpc_projection_cases",
        ("--hpc-projection-cases", "--render-size", "256", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_hpc_projection_cases_sample_texture",
        ("--hpc-projection-cases", "--render-size", "128", "--sample-texture", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_hpc_render_cases",
        ("--hpc-render-cases", "--render-size", "256", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_surface_map_cases",
        ("--surface-map-cases", "--render-size", "256", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_surface_map_cases_sample_texture",
        ("--surface-map-cases", "--render-size", "128", "--sample-texture", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_surface_map_cases_diff_selfcheck",
        ("--surface-map-cases-diff-selfcheck", "--render-size", "128", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_surface_map_cases_color_smoke",
        ("--surface-map-cases-color-smoke", "--render-size", "128", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_hpc_diff_selfcheck",
        (str(DATA / "sample.171.fits"), "--hdu", "1", "--hpc-diff-selfcheck", "--render-size", "256", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_hpc_diff_selfcheck_cor2",
        (str(DATA / "20241224_194245_d4c2A.fts"), "--hpc-diff-selfcheck", "--render-size", "256", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_hpc_projection_cases_diff_selfcheck",
        ("--hpc-projection-cases-diff-selfcheck", "--render-size", "128", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_hpc_projection_cases_color_smoke",
        ("--hpc-projection-cases-color-smoke", "--render-size", "128", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_hpc_projection_cases_color_diff_smoke",
        ("--hpc-projection-cases-color-diff-smoke", "--render-size", "128", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_all_modes_diff_selfcheck",
        (str(DATA / "sample.171.fits"), "--hdu", "1", "--all-modes-diff-selfcheck", "--render-size", "128", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_all_modes_color_smoke",
        (str(DATA / "sample.171.fits"), "--hdu", "1", "--all-modes-color-smoke", "--render-size", "128", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_all_modes_color_diff_smoke",
        (str(DATA / "sample.171.fits"), "--hdu", "1", "--all-modes-color-diff-smoke", "--render-size", "128", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_differential_rotation",
        ("--differential-rotation", "--render-size", "128", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_distinct_wcs_slots",
        ("--distinct-wcs-slots", "--render-size", "128", "--backend", "default"),
        "electron",
    ),
    ValidationRun(
        "electron_default_planar_masks",
        ("--planar-masks", "--render-size", "128", "--backend", "default"),
        "electron",
    ),
)


# The default suite answers the highest-value rendering questions without
# running every historical diagnostic. The complete catalog remains available
# through --extended or --only.
CORE_RUN_NAMES = {
    "glsl_syntax",
    "java_metadata",
    "hpc_render_compare",
    "hpc_render_compare_arc_punch",
    "hpc_render_compare_hi1",
    "hpc_render_compare_wispr_2222",
    "all_pixels_zpn_wispr_1211",
    "surface_map_render_compare_car_sunerf",
    "surface_map_render_compare_cea",
}

CORE_ELECTRON_RUN_NAMES = {
    "electron_default_all_modes_sample_texture",
    "electron_default_differential_rotation",
    "electron_default_distinct_wcs_slots",
    "electron_default_hpc_projection_cases_sample_texture",
    "electron_default_planar_masks",
    "electron_default_surface_map_cases_sample_texture",
}

SWIFTSHADER_CORE_SOURCE_NAMES = {
    "electron_default_differential_rotation",
    "electron_default_distinct_wcs_slots",
    "electron_default_hpc_projection_cases_sample_texture",
    "electron_default_planar_masks",
    "electron_default_surface_map_cases_sample_texture",
}


def core_variant(run: ValidationRun) -> ValidationRun:
    args = list(run.args)
    for index, arg in enumerate(args[:-1]):
        if arg == "--render-size":
            args[index + 1] = "512"
        elif arg == "--surface-map-grid-factor":
            args[index + 1] = "1"
    return ValidationRun(run.name, tuple(args), run.validator)


def swiftshader_variant(run: ValidationRun) -> ValidationRun:
    args = tuple("swiftshader" if arg == "default" else arg for arg in run.args) + ("--report-validity-mismatches",)
    return ValidationRun(run.name.replace("electron_default_", "electron_swiftshader_"), args, run.validator)


CORE_RUNS = tuple(core_variant(run) for run in RUNS if run.name in CORE_RUN_NAMES)
CORE_ELECTRON_RUNS = tuple(core_variant(run) for run in ELECTRON_RUNS if run.name in CORE_ELECTRON_RUN_NAMES)
SWIFTSHADER_CORE_RUNS = tuple(
    swiftshader_variant(core_variant(run))
    for run in ELECTRON_RUNS
    if run.name in SWIFTSHADER_CORE_SOURCE_NAMES
)
ALL_ELECTRON_RUNS = ELECTRON_RUNS + tuple(swiftshader_variant(run) for run in ELECTRON_RUNS)


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
    parser.add_argument(
        "--extended",
        action="store_true",
        help="Run the complete historical catalog instead of the prioritized core",
    )
    parser.add_argument(
        "--jobs",
        type=int,
        default=1,
        help="Number of validation runs to execute in parallel",
    )
    parser.add_argument(
        "--include-electron",
        action="store_true",
        help="Include the focused Metal/ANGLE and SwiftShader validation runs",
    )
    parser.add_argument(
        "--electron-only",
        action="store_true",
        help="Run only the Metal/ANGLE and SwiftShader validation runs",
    )
    return parser.parse_args()


def available_runs(extended: bool, include_electron: bool, electron_only: bool, only: list[str] | None) -> list[ValidationRun]:
    if only:
        return list(RUNS + ALL_ELECTRON_RUNS)
    if electron_only:
        return list(ALL_ELECTRON_RUNS if extended else CORE_ELECTRON_RUNS + SWIFTSHADER_CORE_RUNS)
    runs = list(RUNS if extended else CORE_RUNS)
    if include_electron:
        runs.extend(ALL_ELECTRON_RUNS if extended else CORE_ELECTRON_RUNS + SWIFTSHADER_CORE_RUNS)
    return runs


def selected_runs(runs: list[ValidationRun], only: list[str] | None) -> list[ValidationRun]:
    if not only:
        return runs
    requested = set(only)
    known = {run.name for run in runs}
    missing = sorted(requested - known)
    if missing:
        raise SystemExit(f"Unknown run name(s): {', '.join(missing)}")
    return [run for run in runs if run.name in requested]


def validator_for(run: ValidationRun) -> Path:
    if run.validator == "glsl":
        return GLSL_VALIDATOR
    if run.validator == "electron":
        return ELECTRON_VALIDATOR
    if run.validator == "java_metadata":
        return JAVA_METADATA_VALIDATOR
    return VALIDATOR


def run_case(run: ValidationRun) -> ValidationResult:
    validator = validator_for(run)
    cmd = [sys.executable, str(validator), *run.args]
    artifact_dir: Path | None = None
    if run.validator in ("electron", "wcs"):
        artifact_dir = Path(tempfile.mkdtemp(prefix=f"jhv-{run.name}-"))
        cmd.extend(("--output-dir", str(artifact_dir)))
    completed = subprocess.run(
        cmd,
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
    )
    stderr = completed.stderr
    if artifact_dir is not None:
        if completed.returncode == 0:
            shutil.rmtree(artifact_dir)
        else:
            stderr += f"\nFailure artifacts retained at {artifact_dir}\n"
    return ValidationResult(
        run=run,
        returncode=completed.returncode,
        stdout=completed.stdout,
        stderr=stderr,
    )


def print_result(result: ValidationResult) -> None:
    validator = validator_for(result.run)
    cmd = [sys.executable, str(validator), *result.run.args]
    print(f"\n== {result.run.name} ==")
    print(" ".join(cmd))
    if result.stdout:
        print(result.stdout, end="" if result.stdout.endswith("\n") else "\n")
    if result.stderr:
        print(result.stderr, end="" if result.stderr.endswith("\n") else "\n", file=sys.stderr)


def run_sequential(runs: list[ValidationRun], keep_going: bool) -> list[ValidationResult]:
    results: list[ValidationResult] = []
    for run in runs:
        result = run_case(run)
        print_result(result)
        results.append(result)
        if result.returncode != 0 and not keep_going:
            break
    return results


def run_parallel(runs: list[ValidationRun], keep_going: bool, jobs: int) -> list[ValidationResult]:
    results_by_name: dict[str, ValidationResult] = {}
    submitted: dict[Future[ValidationResult], ValidationRun] = {}
    stop_submitting = False
    next_index = 0

    with ThreadPoolExecutor(max_workers=jobs) as executor:
        while next_index < len(runs) and len(submitted) < jobs:
            run = runs[next_index]
            submitted[executor.submit(run_case, run)] = run
            next_index += 1

        while submitted:
            done, _ = wait(submitted.keys(), return_when=FIRST_COMPLETED)
            for future in done:
                run = submitted.pop(future)
                result = future.result()
                results_by_name[run.name] = result
                if result.returncode != 0 and not keep_going:
                    stop_submitting = True
                    for pending in submitted:
                        pending.cancel()
                    submitted.clear()
                    break

            if stop_submitting:
                break

            while next_index < len(runs) and len(submitted) < jobs:
                run = runs[next_index]
                submitted[executor.submit(run_case, run)] = run
                next_index += 1

    ordered_results: list[ValidationResult] = []
    for run in runs:
        result = results_by_name.get(run.name)
        if result is None:
            break
        print_result(result)
        ordered_results.append(result)
        if result.returncode != 0 and not keep_going:
            break
    return ordered_results


def main() -> int:
    args = parse_args()
    all_runs = available_runs(args.extended, args.include_electron, args.electron_only, args.only)
    if args.list:
        for run in all_runs:
            print(run.name)
        return 0

    runs = selected_runs(all_runs, args.only)
    if args.jobs < 1:
        raise SystemExit("--jobs must be at least 1")

    results = run_parallel(runs, args.keep_going, args.jobs) if args.jobs > 1 else run_sequential(runs, args.keep_going)
    failures = [result.run.name for result in results if result.returncode != 0]

    if failures and not args.keep_going:
        return next(result.returncode for result in results if result.returncode != 0)

    if failures:
        print("\nFAILED:")
        for name in failures:
            print(f"- {name}")
        return 1

    print(f"\nAll {len(results)} validation run(s) passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
