#!/usr/bin/env python3

from __future__ import annotations

import importlib.util
import json
import subprocess
import sys
from pathlib import Path
from tempfile import TemporaryDirectory


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent.parent
VALIDATOR = SCRIPT_DIR / "validate_jhv_wcs_against_astropy.py"
SUITE = SCRIPT_DIR / "run_jhv_wcs_hpc_validation_suite.py"
JAVA_SOURCE = SCRIPT_DIR / "JHVMetadataDump.java"
JAVA_CLASS = "org.helioviewer.jhv.metadata.JHVMetadataDump"


def load_module(path: Path, name: str):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


def java_classpath(java_out: Path) -> str:
    jars = sorted((REPO_ROOT / "lib").glob("**/*.jar"))
    parts = [str(REPO_ROOT / "bin"), str(java_out), *map(str, jars)]
    return ":".join(parts)


def compile_java_helper(java_out: Path) -> None:
    subprocess.run(["ant", "compile"], cwd=REPO_ROOT, check=True)
    subprocess.run(
        ["javac", "-cp", java_classpath(java_out), "-d", str(java_out), str(JAVA_SOURCE)],
        cwd=REPO_ROOT,
        check=True,
    )


def java_dump(java_out: Path, file_path: Path, hdu: int | None) -> dict:
    cmd = ["java", "-cp", java_classpath(java_out), JAVA_CLASS, str(file_path)]
    if hdu is not None:
        cmd.extend(["--hdu", str(hdu)])
    completed = subprocess.run(cmd, cwd=REPO_ROOT, text=True, capture_output=True)
    if completed.returncode != 0:
        raise RuntimeError(
            f"Java metadata helper failed for {file_path}\n"
            f"stdout={completed.stdout}\n"
            f"stderr={completed.stderr}"
        )
    return json.loads(completed.stdout)


def suite_cases() -> list[tuple[Path, int | None]]:
    suite_module = load_module(SUITE, "jhv_suite")
    cases: list[tuple[Path, int | None]] = []
    seen: set[tuple[str, int | None]] = set()
    for run in suite_module.RUNS:
        if run.validator != "wcs":
            continue
        file_path = Path(run.args[0])
        hdu = None
        if "--hdu" in run.args:
            idx = run.args.index("--hdu")
            hdu = int(run.args[idx + 1])
        key = (str(file_path), hdu)
        if key in seen:
            continue
        seen.add(key)
        cases.append((file_path, hdu))
    return cases


def compare_scalars(name: str, java_value: float, py_value: float, abs_tol: float, rel_tol: float) -> str | None:
    diff = abs(java_value - py_value)
    limit = max(abs_tol, rel_tol * max(abs(java_value), abs(py_value), 1.0))
    if diff <= limit:
        return None
    return f"{name}: java={java_value!r} python={py_value!r} diff={diff:.3e} limit={limit:.3e}"


def main() -> int:
    validator = load_module(VALIDATOR, "jhv_validator")

    float_fields = {
        "arcsec_per_pixel_x": (1e-9, 1e-12),
        "arcsec_per_pixel_y": (1e-9, 1e-12),
        "unit_per_arcsec": (1e-15, 1e-12),
        "unit_per_pixel_x": (1e-15, 1e-12),
        "unit_per_pixel_y": (1e-15, 1e-12),
        "plane_units_per_rad": (1e-6, 1e-7),
        "crpix1_gl": (0.0, 0.0),
        "crpix2_gl": (0.0, 0.0),
        "crval_internal_x": (1e-15, 1e-12),
        "crval_internal_y": (1e-15, 1e-12),
        "crota_rad": (1e-12, 1e-12),
        "observer_distance": (1e-2, 1e-4),
    }

    failures: list[str] = []

    with TemporaryDirectory() as temp_dir:
        java_out = Path(temp_dir)
        compile_java_helper(java_out)

        for file_path, hdu in suite_cases():
            with validator.fits.open(file_path) as hdul:
                image_hdu = validator.find_image_hdu(hdul, hdu)
                py_meta = validator.build_jhv_meta(image_hdu.header)
            java_meta = java_dump(java_out, file_path, hdu)

            case_errors: list[str] = []

            for field in ("pixel_width", "pixel_height", "projection"):
                py_value = getattr(py_meta, field)
                java_value = java_meta[field]
                if java_value != py_value:
                    case_errors.append(f"{field}: java={java_value!r} python={py_value!r}")

            for field, (abs_tol, rel_tol) in float_fields.items():
                py_value = float(getattr(py_meta, field))
                java_value = float(java_meta[field])
                mismatch = compare_scalars(field, java_value, py_value, abs_tol, rel_tol)
                if mismatch is not None:
                    case_errors.append(mismatch)

            if not validator.is_surface_map_projection(py_meta):
                hpc_bounds = validator.raw_hpc_footprint_bounds_degrees(py_meta)
                for field, py_value in zip(
                    ("hpc_min_x", "hpc_max_x", "hpc_min_y", "hpc_max_y"),
                    hpc_bounds,
                    strict=True,
                ):
                    mismatch = compare_scalars(field, float(java_meta[field]), py_value, 5e-6, 1e-7)
                    if mismatch is not None:
                        case_errors.append(mismatch)

            mismatch = compare_scalars(
                "radial_bound",
                float(java_meta["radial_bound"]),
                validator.image_radial_bound(py_meta),
                1e-6,
                1e-4,
            )
            if mismatch is not None:
                case_errors.append(mismatch)

            sun_shift = validator.image_sun_shift(py_meta)
            for field, py_value in zip(("sun_shift_x", "sun_shift_y"), sun_shift, strict=True):
                mismatch = compare_scalars(field, float(java_meta[field]), py_value, 1e-6, 1e-7)
                if mismatch is not None:
                    case_errors.append(mismatch)

            py_zpn_upper_eta = validator.zpn_primary_branch_upper_eta(py_meta) if py_meta.projection == "ZPN" else 0.0
            java_zpn_upper_eta = float(validator.np.float32(java_meta["zpn_upper_eta"]))
            py_zpn_upper_eta = float(validator.np.float32(py_zpn_upper_eta))
            mismatch = compare_scalars("zpn_upper_eta", java_zpn_upper_eta, py_zpn_upper_eta, 0.0, 0.0)
            if mismatch is not None:
                case_errors.append(mismatch)

            for index, (java_value, py_value) in enumerate(zip(java_meta["pv2"], py_meta.pv2, strict=True)):
                mismatch = compare_scalars(f"pv2[{index}]", float(java_value), float(py_value), 1e-6, 1e-7)
                if mismatch is not None:
                    case_errors.append(mismatch)

            case_label = f"{file_path.name}" + (f" [hdu={hdu}]" if hdu is not None else "")
            if case_errors:
                failures.append(case_label)
                print(f"FAIL {case_label}")
                for error in case_errors:
                    print(f"  {error}")
            else:
                print(f"OK   {case_label}")

    if failures:
        print("\nFAILED:")
        for label in failures:
            print(f"- {label}")
        return 1

    print(f"\nAll {len(suite_cases())} Java/Python metadata comparison case(s) matched.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
