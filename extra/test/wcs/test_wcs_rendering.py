#!/usr/bin/env python3

"""Full-fragment regression cases for JHV display geometry and FITS WCS sampling.

Astropy supplies angular-coordinate/pixel conversion, not JHV's display modes.
The orthographic geometry below is an independent ray/sphere/plane construction.
Coordinate jobs keep production main() and replace only getColor(). Color jobs
run the complete, unchanged shader, using known R8/R16F images and a colored LUT.
"""

import argparse
import math
from pathlib import Path
from tempfile import TemporaryDirectory

import numpy as np
from astropy.io import fits

import validate_jhv_wcs_against_astropy as cpu
import validate_jhv_wcs_with_electron as gpu


def image_header(projection, distance=10.0):
    width = 128
    surface = projection in ("CAR", "CEA")
    header = fits.Header(
        {
            "NAXIS": 2,
            "NAXIS1": width,
            "NAXIS2": width,
            "CTYPE1": f"{'CRLN' if surface else 'HPLN'}-{projection}",
            "CTYPE2": f"{'CRLT' if surface else 'HPLT'}-{projection}",
            "CUNIT1": "deg",
            "CUNIT2": "deg",
            "CRPIX1": 64.5,
            "CRPIX2": 64.5,
            "CRVAL1": 0.0 if surface else 0.8 / distance,
            "CRVAL2": 0.0 if surface else -0.6 / distance,
            "CDELT1": 360 / width if surface else 2.7 / distance,
            "CDELT2": (
                (180 / width if projection == "CAR" else math.degrees(2) / width)
                if surface
                else 2.4 / distance
            ),
            "DSUN_OBS": distance * 695700000.0,
        }
    )
    if not surface:
        header.update({"PC1_1": 0.96, "PC1_2": -0.28, "PC2_1": 0.21, "PC2_2": 1.02})
    if projection == "AZP":
        header.update({"PV2_1": 0.7, "PV2_2": 12.0})
    if projection == "ZPN":
        header.update({"PV2_1": 1.0, "PV2_3": 0.7})
    return header


def orthographic_geometry(x, y, rotation, surface=False):
    """Intersect parallel display rays, then express the hit in image coordinates."""
    x, y = np.broadcast_arrays(x, y)
    on_disk = x * x + y * y <= 1
    points = np.stack((x, y, np.sqrt(np.maximum(0, 1 - x * x - y * y))), axis=-1)
    world = points @ rotation
    valid = on_disk.copy()
    if not surface:
        plane = ~on_disk | (world[..., 2] <= 0)
        normal = rotation[:, 2]
        if abs(normal[2]) < 1e-8:
            valid &= ~plane
        else:
            z = -(x * normal[0] + y * normal[1]) / normal[2]
            plane_points = np.stack((x, y, z), axis=-1) @ rotation
            visible_plane = (~on_disk | ((normal[2] > 0) & (z >= 0))) & (
                np.sum(plane_points**2, axis=-1) > 1
            )
            valid = np.where(plane, visible_plane, valid)
            world = np.where(plane[..., None], plane_points, world)
    return world, valid, on_disk


def reference_pixels(header, world, simple_tan=True):
    """Convert independently constructed 3D points through the input FITS WCS."""
    meta = cpu.build_jhv_meta(header)
    wcs = cpu.build_astropy_pixel_wcs(header)
    shape = world.shape[:-1]
    points = world.reshape(-1, 3)
    if meta.projection == "TAN" and simple_tan:
        # JHV's simple-TAN convention is affine in the image-aligned x/y plane.
        plane = np.degrees(points[:, :2] / meta.plane_units_per_rad) - wcs.wcs.crval
        pixels = np.linalg.solve(wcs.pixel_scale_matrix, plane.T).T + wcs.wcs.crpix
    else:
        if meta.projection in ("CAR", "CEA"):
            angles = np.column_stack(
                (
                    np.arctan2(points[:, 0], points[:, 2]),
                    np.arcsin(np.clip(points[:, 1], -1, 1)),
                )
            )
        else:
            delta = points - [0, 0, meta.observer_distance]
            angles = np.column_stack(
                (
                    np.arctan2(delta[:, 0], -delta[:, 2]),
                    np.arcsin(delta[:, 1] / np.linalg.norm(delta, axis=1)),
                )
            )
        pixels = wcs.wcs_world2pix(np.degrees(angles), 1)
    pixels = cpu.fits_pixel_to_texture_pixel(pixels, meta)
    if meta.projection in ("CAR", "CEA"):
        pixels[:, 0] %= meta.pixel_width
    uv = pixels / [meta.pixel_width, meta.pixel_height]
    valid = np.isfinite(uv).all(axis=1) & (uv >= 0).all(axis=1) & (uv <= 1).all(axis=1)
    return uv.reshape((*shape, 2)), valid.reshape(shape)


def texture(root, name, format, secondary=False):
    y, x = np.indices((128, 128))
    values = (
        (0.24 + 0.3 * x / 127 + 0.16 * y / 127)
        if not secondary
        else (0.3 + 0.2 * x / 127 + 0.2 * y / 127)
    )
    data = (
        np.rint(values * 255).astype(np.uint8)
        if format == "R8"
        else values.astype(np.float16)
    )
    path = root / f"{name}.bin"
    data.tofile(path)
    # The CPU sampler treats row zero as the image's top, GL as its bottom.
    decoded = data.astype(float) / 255 if format == "R8" else data.astype(float)
    return {"path": str(path), "format": format}, decoded[::-1]


def sample_image(image, uv, valid):
    values = np.zeros(valid.shape)
    for index in zip(*np.nonzero(valid)):
        values[index] = cpu.sample_texture_linear(image, tuple(uv[index]))
    return values


def edge_band(uv, tolerance=0.05 / 128):
    return (
        np.isfinite(uv).all(axis=-1)
        & (uv >= -tolerance).all(axis=-1)
        & (uv <= 1 + tolerance).all(axis=-1)
        & (np.minimum(abs(uv), abs(uv - 1)).min(axis=-1) <= tolerance)
    )


def verify_footprint(actual, valid, boundary=None):
    mismatch = actual != valid
    if boundary is None:
        np.testing.assert_array_equal(actual, valid)
    elif np.any(mismatch & ~boundary) or np.count_nonzero(mismatch) > max(
        1, valid.size // 1000
    ):
        raise AssertionError(
            f"Unexpected footprint: {np.count_nonzero(mismatch & ~boundary)} interior and {np.count_nonzero(mismatch & boundary)} boundary mismatches"
        )
    elif mismatch.any():
        print(
            f"  crop_boundary_mismatches={np.count_nonzero(mismatch)} (within 0.05 source pixel)"
        )
    return valid & actual


def verify_coordinates(
    pixels, uv, valid, width=128, tolerance=0.05, boundary=None, wrap_longitude=False
):
    if not np.isfinite(pixels).all():
        raise AssertionError("Non-finite coordinate output")
    valid = verify_footprint(pixels[..., 3] > 0.5, valid, boundary)
    if not valid.any():
        raise AssertionError("Empty reference footprint")
    error = np.abs(pixels[..., :2] - uv) * width
    if wrap_longitude:
        # Only a full-longitude surface map has equivalent coordinates at this seam.
        error[..., 0] = np.minimum(error[..., 0], np.abs(width - error[..., 0]))
    if np.max(error[valid]) > tolerance:
        raise AssertionError(
            f"Coordinate error {np.max(error[valid]):.6g} px exceeds {tolerance}"
        )


def verify_colors(
    pixels, values, valid, lut, color, filter_error=1 / 2048, boundary=None
):
    if not np.isfinite(pixels).all():
        raise AssertionError("Non-finite color output")
    valid = verify_footprint(pixels[..., 3] > 0, valid, boundary)
    if not valid.any():
        raise AssertionError("Empty reference footprint")
    # Production dithering is intentionally backend-dependent, bounded by 1/255.
    # Accept only LUT entries reachable within that bound plus filtering precision.
    radius = 1 / 255 + filter_error + 1e-5
    low = np.clip(np.floor((values - radius) * 256), 0, 255).astype(int)
    high = np.clip(np.floor((values + radius) * 256), 0, 255).astype(int)
    matches = np.zeros(valid.shape, dtype=bool)
    for offset in range(int(np.max(high - low)) + 1):
        index = np.minimum(low + offset, high)
        expected = lut[index] / 255 * color
        matches |= np.max(np.abs(pixels - expected), axis=-1) < 2e-5
    if not np.all(matches[valid]):
        raise AssertionError(
            f"Incorrect colors in {np.count_nonzero(valid & ~matches)} pixels"
        )


def run_cases(root, backend, size, electron):
    jobs, checks = [], []
    yy, xx = np.indices((size, size), dtype=float)
    nx, ny = 2 * (xx + 0.5) / size - 1, 2 * (yy + 0.5) / size - 1
    # Deliberately colored and discontinuous, so grayscale or interpolated LUTs fail.
    index = np.arange(256)
    lut = np.column_stack(
        (index, 255 - index, np.where(index < 128, 40, 220), np.full(256, 255))
    )
    color = np.array([0.4, 0.3, 0.2, 0.5])  # Premultiplied layer tint.
    variants = (
        ("ortho", 10, 0, 1.6, "R8"),
        ("ortho", 215, 50, 1.4, "R16F"),
        ("ortho", 10, 120, 1.6, "R16F"),
    )
    for projection in ("TAN", "ARC", "AZP", "ZPN", "CAR", "CEA"):
        surface = projection in ("CAR", "CEA")
        other_modes = (
            ("lati_zenithal",)
            if surface
            else ("hpc", "lati_zenithal", "radial_warp", "rect_warp")
        )
        for mode, distance, degrees, scale, format in (
            *variants,
            *((mode, 10, 0, 0.5, "R16F") for mode in other_modes),
        ):
            header = image_header(projection, distance)
            meta = cpu.build_jhv_meta(header)
            angle = math.radians(degrees)
            rotation = np.array(
                [
                    [math.cos(angle), 0, math.sin(angle)],
                    [0, 1, 0],
                    [-math.sin(angle), 0, math.cos(angle)],
                ]
            )
            bounds = (
                gpu.hpc_bounds_degrees(meta, 1)
                if mode == "hpc"
                else gpu.bounds_for_mode(mode, meta)
            )
            if mode == "ortho":
                x, y = nx * scale + 0.12, ny * scale - 0.08
                world, geometry_valid, on_disk = orthographic_geometry(
                    x, y, rotation, surface
                )
            elif mode == "lati_zenithal":
                longitude = np.radians(
                    bounds[0] + (nx + 1) / 2 * (bounds[1] - bounds[0])
                )
                latitude = np.radians(
                    bounds[2] + (ny + 1) / 2 * (bounds[3] - bounds[2])
                )
                world = np.stack(
                    (
                        np.cos(latitude) * np.sin(longitude),
                        np.sin(latitude),
                        np.cos(latitude) * np.cos(longitude),
                    ),
                    axis=-1,
                )
                geometry_valid = (
                    np.ones(nx.shape, dtype=bool) if surface else world[..., 2] >= 0
                )
            elif mode == "hpc":
                longitude = np.radians(
                    bounds[0] + (nx + 1) / 2 * (bounds[1] - bounds[0])
                )
                latitude = np.radians(
                    bounds[2] + (ny + 1) / 2 * (bounds[3] - bounds[2])
                )
                world = np.stack(
                    (
                        distance * np.tan(longitude),
                        distance * np.tan(latitude) / np.cos(longitude),
                        np.zeros(nx.shape),
                    ),
                    axis=-1,
                )
                geometry_valid = np.ones(nx.shape, dtype=bool)
            else:
                # Warp geometry is JHV-specific. Astropy independently checks its
                # downstream WCS mapping, not the choice of warp function.
                world = np.zeros((*nx.shape, 3))
                for iy, ix in np.ndindex(nx.shape):
                    world[iy, ix, :2] = gpu.warp_hpc_xy(
                        mode, (ix + 0.5) / size, (iy + 0.5) / size, bounds[3], 1
                    )
                geometry_valid = np.isfinite(world).all(axis=-1)
            uv, image_valid = reference_pixels(header, world, mode == "ortho")
            valid = geometry_valid & image_valid
            name = f"{projection}_{mode}_{distance}_{degrees}_{format}"
            source, image = texture(root, name, format)
            diff_source, diff_image = texture(root, name + "_diff", format, True)
            job = gpu.common_job(
                mode, Path(name), size, root, meta, bounds, True, backend, name
            )
            inverse = np.diag([scale, scale, -10, 1]).astype(float)
            if mode == "ortho":
                inverse[0, 3], inverse[1, 3] = 0.12, -0.08
            job.update(
                {
                    "inverseMVP": inverse.T.ravel().tolist(),
                    "sourceTexture": source,
                    "cameraDiff": [0, math.sin(angle / 2), 0, math.cos(angle / 2)],
                    "sourceViewQuat": [0, math.sin(angle / 2), 0, math.cos(angle / 2)],
                    "lutData": lut.ravel().tolist(),
                    "color": color.tolist(),
                    "brightness": [-0.04, 0.9],
                }
            )

            def add(suffix, options, check):
                jobs.append(
                    {
                        **job,
                        **options,
                        "name": name + suffix,
                        "outputPath": str(root / f"{name}{suffix}.rgba32f"),
                    }
                )
                checks.append(check)

            boundary = edge_band(uv)
            add(
                "_coordinates",
                {"productionCoordinates": True},
                ("coordinates", uv, valid, boundary),
            )
            values = sample_image(image, uv, valid) * 0.9 - 0.04
            add("_color", {"colorSmoke": True}, ("color", values, valid, boundary))

            secondary_header = header.copy()
            secondary_header["CRPIX1"] += 7
            secondary_header["CRPIX2"] -= 3
            secondary_uv, secondary_valid = reference_pixels(
                secondary_header, world, mode == "ortho"
            )
            diff_valid = valid & secondary_valid
            difference = (
                sample_image(image, uv, diff_valid)
                - sample_image(diff_image, secondary_uv, diff_valid)
            ) * 0.9 * 2.5 + 0.5
            secondary = {
                **gpu.wcs_job_fields(cpu.build_jhv_meta(secondary_header)),
                "cameraDiff": job["cameraDiff"],
                "sourceViewQuat": job["sourceViewQuat"],
            }
            add(
                "_difference",
                {
                    "colorDiffSmoke": True,
                    "secondary": secondary,
                    "diffSourceTexture": diff_source,
                },
                ("color", difference, diff_valid, boundary | edge_band(secondary_uv)),
            )

            if projection == "ZPN" and degrees == 0 and mode == "ortho":
                # Test image depth alone as well as its interaction with the sphere.
                # The second probe must remain visible off-limb, where depth is 1.
                sphere_depth = np.where(
                    on_disk, 0.5 - np.sqrt(np.maximum(0, 1 - x * x - y * y)) / 20, 1
                )
                for sphere in (False, True):
                    depth = sphere_depth if sphere else np.where(valid, sphere_depth, 1)
                    for probe in (0.47, 0.51):
                        add(
                            f"_depth_{sphere}_{probe}",
                            {
                                "colorSmoke": True,
                                "depthProbe": probe,
                                "solarSphere": sphere,
                            },
                            ("depth", probe <= depth, None, None),
                        )

    results = gpu.run_electron_jobs(electron, jobs, backend)
    failed = []
    for job, result, check in zip(jobs, results, checks, strict=True):
        pixels = gpu.read_job_pixels(job)
        kind, expected, valid, boundary = check
        try:
            if kind == "coordinates":
                verify_coordinates(
                    pixels,
                    expected,
                    valid,
                    boundary=boundary,
                    wrap_longitude=job["projectionCode"]
                    in (gpu.PROJECTION_CODES["CAR"], gpu.PROJECTION_CODES["CEA"]),
                )
            elif kind == "color":
                # Each source lookup can round to half precision on Metal.
                # Subtraction amplifies both errors by brightness and BOOST.
                filter_error = (
                    (2 * 2.5 if job.get("colorDiffSmoke") else 1) * 0.9 / 2048
                )
                verify_colors(
                    pixels, expected, valid, lut, color, filter_error, boundary
                )
            else:
                magenta = np.all(pixels == [1, 0, 1, 1], axis=-1)
                np.testing.assert_array_equal(magenta, expected)
                if not magenta.any() or magenta.all():
                    raise AssertionError("Depth probe did not exercise both outcomes")
            print(f"PASS {job['name']} ({result['renderer']})")
        except AssertionError as error:
            print(f"FAIL {job['name']}: {error}")
            failed.append(job["name"])
    print(f"{len(jobs)} production-fragment cases, {len(failed)} failures")
    return int(bool(failed))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--backend", choices=("default", "swiftshader"), default="default"
    )
    parser.add_argument("--electron", type=Path, default=gpu.DEFAULT_ELECTRON)
    parser.add_argument("--render-size", type=int, default=96)
    parser.add_argument("--output-dir", type=Path)
    args = parser.parse_args()
    if args.output_dir:
        args.output_dir.mkdir(parents=True, exist_ok=True)
        return run_cases(args.output_dir, args.backend, args.render_size, args.electron)
    with TemporaryDirectory(prefix="jhv-wcs-rendering-") as directory:
        return run_cases(Path(directory), args.backend, args.render_size, args.electron)


if __name__ == "__main__":
    raise SystemExit(main())
