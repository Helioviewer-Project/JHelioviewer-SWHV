#!/usr/bin/env python3

from __future__ import annotations

import importlib.util
import json
import subprocess
import sys
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent.parent
VALIDATOR = SCRIPT_DIR / "validate_jhv_wcs_against_astropy.py"
SUITE = SCRIPT_DIR / "run_jhv_wcs_hpc_validation_suite.py"
JAVA_SOURCE = SCRIPT_DIR / "JHVMetadataDump.java"
JAVA_OUT = SCRIPT_DIR / ".java-bin"
JAVA_CLASS = "org.helioviewer.jhv.metadata.JHVMetadataDump"


def load_module(path: Path, name: str):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


def java_classpath() -> str:
    jars = sorted((REPO_ROOT / "lib").glob("**/*.jar"))
    parts = [str(REPO_ROOT / "bin"), str(JAVA_OUT), *map(str, jars)]
    return ":".join(parts)


def compile_java_helper() -> None:
    subprocess.run(["ant", "compile"], cwd=REPO_ROOT, check=True)
    JAVA_OUT.mkdir(exist_ok=True)
    subprocess.run(
        ["javac", "-cp", java_classpath(), "-d", str(JAVA_OUT), str(JAVA_SOURCE)],
        cwd=REPO_ROOT,
        check=True,
    )


def java_dump(file_path: Path, hdu: int | None) -> dict:
    cmd = ["java", "-cp", java_classpath(), JAVA_CLASS, str(file_path)]
    if hdu is not None:
        cmd.extend(["--hdu", str(hdu)])
    completed = subprocess.run(cmd, cwd=REPO_ROOT, check=True, text=True, capture_output=True)
    return json.loads(completed.stdout)


def suite_cases() -> list[tuple[Path, int | None]]:
    suite_module = load_module(SUITE, "jhv_suite")
    cases: list[tuple[Path, int | None]] = []
    seen: set[tuple[str, int | None]] = set()
    for run in suite_module.RUNS:
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
    compile_java_helper()

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

    for file_path, hdu in suite_cases():
        with validator.fits.open(file_path) as hdul:
            image_hdu = validator.find_image_hdu(hdul, hdu)
            py_meta = validator.build_jhv_meta(image_hdu.header)
        java_meta = java_dump(file_path, hdu)

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
