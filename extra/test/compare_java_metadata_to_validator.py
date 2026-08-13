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
    cmd = ["java", f"-Duser.home={java_out}", "-cp", java_classpath(java_out), JAVA_CLASS, str(file_path)]
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


def write_generated_case(validator, output_dir: Path, name: str, header) -> Path:
    file_path = output_dir / f"{name}.fits"
    validator.fits.PrimaryHDU(
        data=validator.np.zeros((8, 8), dtype=validator.np.int16),
        header=header,
    ).writeto(file_path, output_verify="silentfix")
    return file_path


def generated_matrix_cases(
    validator,
    output_dir: Path,
) -> tuple[list[tuple[Path, int | None]], set[Path], Path]:
    source = SCRIPT_DIR / "data" / "sample.171.fits"
    with validator.fits.open(source) as hdul:
        source_header = validator.find_image_hdu(hdul, 1).header.copy()

    observer_cases = (
        (
            "pc_unequal_negative",
            {
                "CDELT1": -0.7,
                "CDELT2": 1.1,
                "PC1_1": 0.9,
                "PC1_2": 0.2,
                "PC2_1": -0.1,
                "PC2_2": 1.05,
                # Irrelevant PV cards must not leak into a TAN interpretation.
                "PV2_3": 123.0,
            },
        ),
        (
            "pc_partial",
            {
                "CDELT1": -0.7,
                "CDELT2": 1.1,
                "PC1_2": 0.15,
            },
        ),
        (
            "crota_unequal",
            {
                "CDELT1": -0.7,
                "CDELT2": 1.1,
                "CROTA2": 23.0,
            },
        ),
        (
            "pc_radian_units",
            {
                "CUNIT1": "rad",
                "CUNIT2": "rad",
                "CDELT1": -3.4e-6,
                "CDELT2": 5.3e-6,
                "CRVAL1": 2e-5,
                "CRVAL2": -1e-5,
                "PC1_1": 0.95,
                "PC1_2": -0.12,
                "PC2_1": 0.08,
                "PC2_2": 1.02,
            },
        ),
        (
            "pc_mixed_angular_units",
            {
                "CUNIT1": "arcmin",
                "CUNIT2": "mas",
                "CDELT1": -0.012,
                "CDELT2": 730.0,
                "CRVAL1": 0.03,
                "CRVAL2": -1200.0,
                "PC1_1": 0.98,
                "PC1_2": -0.08,
                "PC2_1": 0.06,
                "PC2_2": 1.01,
            },
        ),
        (
            "pc_precedes_crota",
            {
                "CDELT1": -0.7,
                "CDELT2": 1.1,
                "PC1_1": 0.91,
                "PC1_2": 0.17,
                "PC2_1": -0.13,
                "PC2_2": 1.04,
                "CROTA2": 71.0,
            },
        ),
        (
            "missing_observer_cunit",
            {
                "CDELT1": -0.7,
                "CDELT2": 1.1,
                "PC1_1": 0.97,
                "PC1_2": 0.12,
                "PC2_1": -0.07,
                "PC2_2": 1.02,
            },
        ),
        (
            "lasco_transform_suppression",
            {
                "TELESCOP": "SOHO",
                "INSTRUME": "LASCO",
                "DETECTOR": "C2",
                "DATE-OBS": "2012-08-31",
                "TIME_OBS": "17:34:11.34",
                "CDELT1": 11.4,
                "CDELT2": 12.1,
                "PC1_1": 0.96,
                "PC1_2": -0.28,
                "PC2_1": 0.28,
                "PC2_2": 0.96,
            },
        ),
    )

    generated: list[tuple[Path, int | None]] = []
    astropy_cases: set[Path] = set()
    lasco_case = output_dir / "lasco_transform_suppression.fits"
    reset_keys = (
        "CROTA", "CROTA1", "CROTA2",
        "PC1_1", "PC1_2", "PC2_1", "PC2_2",
        "PV2_0", "PV2_1", "PV2_2", "PV2_3", "PV2_4", "PV2_5",
    )

    for name, values in observer_cases:
        header = source_header.copy()
        for key in reset_keys:
            header.remove(key, ignore_missing=True, remove_all=True)
        if name == "missing_observer_cunit":
            header.remove("CUNIT1", ignore_missing=True, remove_all=True)
            header.remove("CUNIT2", ignore_missing=True, remove_all=True)
        header["CRPIX1"] = 4.5
        header["CRPIX2"] = 4.5
        for key, value in values.items():
            header[key] = value

        file_path = write_generated_case(validator, output_dir, name, header)
        generated.append((file_path, 0))
        if file_path != lasco_case and name != "missing_observer_cunit":
            astropy_cases.add(file_path)

    surface_cases = (
        (
            "car_pc_unequal_negative",
            SCRIPT_DIR / "data" / "sunerf_map.fits",
            {
                "CDELT1": -0.08,
                "CDELT2": 0.05,
                "PC1_1": 0.94,
                "PC1_2": 0.18,
                "PC2_1": -0.11,
                "PC2_2": 1.03,
            },
        ),
        (
            "cea_pc_unequal_negative",
            SCRIPT_DIR / "data" / "mrzqs260301t2314c2308_169.fits",
            {
                "CDELT1": -0.8,
                "CDELT2": 0.006,
                "PC1_1": 0.999,
                "PC1_2": 0.01,
                "PC2_1": -0.01,
                "PC2_2": 0.999,
            },
        ),
    )
    for name, source, values in surface_cases:
        with validator.fits.open(source) as hdul:
            source_hdu = validator.find_image_hdu(hdul, None)
            header = source_hdu.header.copy()
        header["CRPIX1"] = 4.5
        header["CRPIX2"] = 4.5
        for key, value in values.items():
            header[key] = value

        file_path = write_generated_case(validator, output_dir, name, header)
        generated.append((file_path, 0))
        astropy_cases.add(file_path)

    return generated, astropy_cases, lasco_case


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
        "crval_internal_x": (1e-15, 1e-12),
        "crval_internal_y": (1e-15, 1e-12),
        "observer_distance": (1e-2, 1e-4),
    }

    failures: list[str] = []

    with TemporaryDirectory() as temp_dir:
        java_out = Path(temp_dir)
        compile_java_helper(java_out)
        generated_cases, astropy_cases, lasco_case = generated_matrix_cases(validator, java_out)
        cases = suite_cases() + generated_cases

        for file_path, hdu in cases:
            with validator.fits.open(file_path) as hdul:
                image_hdu = validator.find_image_hdu(hdul, hdu)
                py_meta = validator.build_jhv_meta(image_hdu.header)
            java_meta = java_dump(java_out, file_path, hdu)

            case_errors: list[str] = []

            if file_path in astropy_cases:
                _, _, projection_wcs, pixel_wcs = validator.load_validation_context(file_path, hdu)
                astropy_status = validator.run_forward_validation(
                    file_path, projection_wcs, pixel_wcs, py_meta,
                    samples=0, seed=0, report_worst=0,
                    all_pixels=True, max_error_px=1e-7,
                )
                if astropy_status != 0:
                    case_errors.append("generated matrix case disagrees with Astropy")

            if java_meta["projection"] != py_meta.projection:
                case_errors.append(
                    f"projection: java={java_meta['projection']!r} python={py_meta.projection!r}"
                )

            for field, (abs_tol, rel_tol) in float_fields.items():
                py_value = float(getattr(py_meta, field))
                java_value = float(java_meta[field])
                mismatch = compare_scalars(field, java_value, py_value, abs_tol, rel_tol)
                if mismatch is not None:
                    case_errors.append(mismatch)

            for index, (java_value, py_value) in enumerate(zip(
                java_meta["image_to_plane"], py_meta.image_to_plane, strict=True
            )):
                mismatch = compare_scalars(
                    f"image_to_plane[{index}]", float(java_value), float(py_value), 1e-12, 1e-12
                )
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

            if (py_meta.projection not in validator.PV2_PROJECTIONS
                    and any(float(value) != 0.0 for value in java_meta["pv2"])):
                case_errors.append(f"irrelevant PV parameters were retained: {java_meta['pv2']!r}")

            if file_path == lasco_case and java_meta["image_to_plane"] != [1.0, 0.0, 0.0, 1.0]:
                case_errors.append(
                    f"LASCO image transform was not suppressed: {java_meta['image_to_plane']!r}"
                )

            render_rect = validator.wcsRect(py_meta)
            for index, (java_value, py_value) in enumerate(zip(java_meta["render_rect"], render_rect, strict=True)):
                mismatch = compare_scalars(f"render_rect[{index}]", float(java_value), float(py_value), 1e-12, 1e-12)
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

    print(f"\nAll {len(cases)} Java/Python metadata comparison case(s) matched.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
