#!/usr/bin/env python3

"""Full-fragment regression cases for JHV display geometry and FITS WCS sampling.

Astropy supplies angular-coordinate/pixel conversion, not JHV's display modes.
The orthographic geometry below is an independent ray/sphere/plane construction.
Coordinate jobs keep production main() and replace only getColor(). Color jobs
run the complete, unchanged shader, using known R8/R16F images and a colored LUT.
"""

import argparse
import json
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


def orthographic_geometry(x, y, rotation, surface=False, horizon=0):
    """Intersect parallel display rays, then express the hit in image coordinates."""
    x, y = np.broadcast_arrays(x, y)
    on_disk = x * x + y * y <= 1
    points = np.stack((x, y, np.sqrt(np.maximum(0, 1 - x * x - y * y))), axis=-1)
    world = points @ rotation
    valid = on_disk.copy()
    if not surface:
        plane = ~on_disk | ((world[..., 2] <= 0) if horizon == 0 else (world[..., 2] < horizon))
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


def reference_pixels(header, world, slit=(0, 1), adjustment=None, mode=None):
    """Convert independently constructed 3D points through the input FITS WCS."""
    meta = cpu.build_jhv_meta(header)
    wcs = cpu.build_astropy_pixel_wcs(header)
    shape = world.shape[:-1]
    points = world.reshape(-1, 3)
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
    if meta.projection in ("CAR", "CEA"):
        # WCSLIB supplies the native projection plane. Enumerate whole turns;
        # the production shader instead intersects integer-turn intervals.
        native = wcs.wcs.s2p(np.degrees(angles), 1)
        plane = native["imgcrd"]
        # Use [-180, 180) for the principal native longitude, including
        # WCSLIB's exact +180 endpoint; this is not a pixel-width wrap.
        plane[:, 0] -= 360 * np.floor((native["phi"] + 180) / 360)
        rotation = np.eye(2) if adjustment is None else np.asarray(adjustment)
        corners = np.array([[.5, .5], [meta.pixel_width + .5, .5],
                            [.5, meta.pixel_height + .5], [meta.pixel_width + .5, meta.pixel_height + .5]])
        native_corners = np.linalg.solve(rotation, ((corners - wcs.wcs.crpix) @ wcs.pixel_scale_matrix.T).T).T
        finite = np.isfinite(plane).all(axis=1)
        uv = np.full((len(points), 2), np.nan)
        valid = np.zeros(len(points), dtype=bool)
        if finite.any():
            first = math.floor((native_corners[:, 0].min() - plane[finite, 0].max()) / 360)
            last = math.ceil((native_corners[:, 0].max() - plane[finite, 0].min()) / 360)
            for turn in sorted(range(first, last + 1), key=abs):
                candidate_plane = (plane + [360 * turn, 0]) @ rotation.T
                candidate = np.linalg.solve(wcs.pixel_scale_matrix, candidate_plane.T).T + wcs.wcs.crpix
                candidate = cpu.fits_pixel_to_texture_pixel(candidate, meta) / [meta.pixel_width, meta.pixel_height]
                inside = (np.isfinite(candidate).all(axis=1) &
                          (candidate >= [slit[0], 0]).all(axis=1) & (candidate <= [slit[1], 1]).all(axis=1))
                take = inside & ~valid
                uv[take] = candidate[take]
                valid |= inside
        return uv.reshape((*shape, 2)), valid.reshape(shape)
    if mode == "ortho" and meta.projection == "TAN":
        # Independent display policy: source XY is the small-angle plane, not
        # a perspective direction. Apply CRVAL before the user plane rotation.
        plane = np.degrees(points[:, :2] / meta.plane_units_per_rad) - wcs.wcs.crval
        if adjustment is not None:
            plane = plane @ np.asarray(adjustment).T
        pixels = np.linalg.solve(wcs.pixel_scale_matrix, plane.T).T + wcs.wcs.crpix
    elif adjustment is None:
        pixels = wcs.wcs_world2pix(np.degrees(angles), 1)
    else:
        plane = wcs.wcs.s2p(np.degrees(angles), 1)["imgcrd"] @ np.asarray(adjustment).T
        pixels = np.linalg.solve(wcs.pixel_scale_matrix, plane.T).T + wcs.wcs.crpix
    pixels = cpu.fits_pixel_to_texture_pixel(pixels, meta)
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


def verify_coordinates(pixels, uv, valid, source_size=(128, 128), tolerance=0.05, boundary=None):
    if not np.isfinite(pixels).all():
        raise AssertionError("Non-finite coordinate output")
    valid = verify_footprint(pixels[..., 3] > 0.5, valid, boundary)
    if not valid.any():
        raise AssertionError("Empty reference footprint")
    error = np.abs(pixels[..., :2] - uv) * source_size
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


def local_geometry_cases(root, backend):
    # Constant screen points exercise production main() without raster-edge ambiguity.
    # Expected motion advances heliographic longitude, independently of shader quaternions.
    jobs, checks = [], []

    def add(name, mode, bounds, points, orientations, times=(0, 0), inverse=None, projection="TAN", distance=10, reference_longitude=0, enhanced=False):
        header = image_header(projection, distance)
        header["CRVAL1"], header["CRVAL2"] = reference_longitude, 0
        for key in ("PC1_1", "PC1_2", "PC2_1", "PC2_2"):
            header.remove(key, ignore_missing=True)
        meta = cpu.build_jhv_meta(header)
        job = gpu.common_job(mode, Path(name), 1, root, meta, bounds, True, backend, name)
        secondary_header = header.copy()
        secondary_header["CRPIX1"] += 3
        fields = []
        expected = []
        for index, (point, quat, dt) in enumerate(zip(points, orientations, times, strict=True)):
            fields.append({**gpu.wcs_job_fields(meta if index == 0 else cpu.build_jhv_meta(secondary_header)),
                           "solarToSource": quat, "sourceToView": [-quat[0], -quat[1], -quat[2], quat[3]],
                           "deltaT": dt})
            if point is not None:
                uv, valid = reference_pixels(header if index == 0 else secondary_header,
                                             np.array(point), mode=mode)
                assert valid, (name, point, uv)
                expected.extend(uv)
        if any(point is None for point in points):
            expected = [0, 0, 0, 0]
        job.update(fields[0])
        job.update({"secondary": fields[1], "difference": True, "productionCoordinates": True, "enhanced": enhanced})
        if inverse is not None:
            job["inverseMVP"] = inverse.T.ravel().tolist()
        jobs.append(job)
        checks.append(("local", np.array(expected), None, None))

    tilts = np.radians([7, 15])
    rotations = [np.array([[1, 0, 0], [0, math.cos(a), -math.sin(a)],
                           [0, math.sin(a), math.cos(a)]]) for a in tilts]
    quats = [[math.sin(a / 2), 0, 0, math.cos(a / 2)] for a in tilts]
    for latitude in (90, 30):
        lat, lon = math.radians(latitude), math.radians(20)
        # Use the exact Y-axis pole rather than cos(pi/2) residue.
        world = (np.array([0., 1., 0.]) if latitude == 90 else
                 np.array([math.cos(lat) * math.sin(lon), math.sin(lat), math.cos(lat) * math.cos(lon)]))
        for mode in gpu.ALL_MODES:
            # HPC and warp inputs start in source-observer coordinates; Ortho/Lati start in solar-world.
            start = world if mode in ("ortho", "lati_zenithal") else rotations[0] @ world
            # Mixed times exercise direct-ray and sphere sampling independently in the two images.
            # Elapsed milliseconds * 1e-9 is elapsed seconds * 1e-6.
            for motion, times in (("static", (0, 0)), ("both", (86400e-6, -2 * 86400e-6)),
                                  ("current", (86400e-6, 0)), ("comparison", (0, -2 * 86400e-6))):
                points = []
                for rotation, dt in zip(rotations, times, strict=True):
                    solar = world if mode in ("ortho", "lati_zenithal") else rotation.T @ start
                    source_lat = math.atan2(solar[1], math.hypot(solar[0], solar[2]))
                    source_lon = math.atan2(solar[0], solar[2])
                    sin_lat2 = math.sin(source_lat) ** 2
                    advance = dt * (2.879 - math.radians(14.1844) / 86400 * 1e6 - 0.339 * sin_lat2 - 0.485 * sin_lat2 ** 2)
                    moved = [math.cos(source_lat) * math.sin(source_lon - advance),
                             math.sin(source_lat), math.cos(source_lat) * math.cos(source_lon - advance)]
                    points.append(rotation @ moved)
                inverse = np.eye(4)
                if mode == "ortho":
                    inverse[:2, :2] = 0
                    inverse[:2, 3] = world[:2]
                    bounds = (0, 1, 0, 1)
                elif mode == "lati_zenithal":
                    bounds = (20, 20, latitude, latitude)
                else:
                    phi = math.atan2(start[0], 10 - start[2])
                    theta = math.atan2(start[1], math.hypot(start[0], 10 - start[2]))
                    bounds = (*([math.degrees(phi)] * 2), *([math.degrees(theta)] * 2))
                    if mode in gpu.WARP_MODES:
                        plane = 10 * start[:2] / (10 - start[2])
                        radius = np.linalg.norm(plane)
                        if mode == "radial_warp":
                            inverse[:2, :2] = 0
                            inverse[:2, 3] = plane / (2 * 2)
                            bounds = (0, 360, 0, 2)
                        else:
                            angle = math.degrees(math.atan2(-plane[0], plane[1])) % 360
                            inverse[:2, :2] = 0
                            inverse[:2, 3] = [angle / 360 - .5, radius / 2 - .5]
                            bounds = (0, 360, 0, 2)  # lambda=1 gives a linear radial map.
                add(f"local_rotation_{mode}_{latitude}_{motion}", mode, bounds, points, quats, times, inverse)

    for distance, z in ((2, .75), (2, .5), (2, .25), (10, .2), (10, .05), (215, .01), (215, .002)):
        # At D=2,z=.5 the encoded Y quaternion has q.y=.5, so source.z is exactly .5.
        quat = [0, math.sqrt((1 - z) / 2), 0, math.sqrt((1 + z) / 2)]
        point = [math.sqrt(1 - z*z), 0, z]
        for slot in (0, 1):
            orientations = [[0, 0, 0, 1], [0, 0, 0, 1]]
            points = [[0, 0, 1], [0, 0, 1]]
            orientations[slot] = quat
            points[slot] = point if distance*z >= 1 else None
            add(f"local_visibility_{distance}_{z}_{slot}", "lati_zenithal", (0, 0, 0, 0),
                points, orientations, distance=distance)
    # Perspective projections use the source observer's tangent plane, while
    # Orthographic TAN deliberately keeps its parallel-ray display policy.
    for projection in ("TAN", "ARC", "AZP", "ZPN"):
        for z in (.05, .2):
            quat = [0, math.sqrt((1 - z) / 2), 0, math.sqrt((1 + z) / 2)]
            point = [math.sqrt(1 - z*z), 0, z]
            expected = point if projection == "TAN" or z >= .1 else None
            for slot in (0, 1):
                points = [[0, 0, 1], [0, 0, 1]]
                orientations = [[0, 0, 0, 1], [0, 0, 0, 1]]
                points[slot], orientations[slot] = expected, quat
                add(f"local_ortho_horizon_{projection}_{z}_{slot}", "ortho", (0, 0, 0, 0),
                    points, orientations, projection=projection)

    # A week of the actual differential correction carries this initially visible
    # equatorial point across the source horizon. Both image slots must reject it.
    theta = -math.acos(.1) + .003
    start = np.array([math.sin(theta), 0, math.cos(theta)])
    phi = math.atan2(start[0], 10 - start[2])
    plane = 10 * start[:2] / (10 - start[2])
    for mode in gpu.ALL_MODES:
        inverse = np.eye(4)
        if mode == "ortho":
            inverse[:2, :2] = 0
            inverse[:2, 3] = start[:2]
            bounds = (0, 1, 0, 1)
        elif mode == "lati_zenithal":
            bounds = (math.degrees(theta), math.degrees(theta), 0, 0)
        elif mode == "hpc":
            bounds = (math.degrees(phi), math.degrees(phi), 0, 0)
        else:
            inverse[:2, :2] = 0
            bounds = (0, 360, 0, 2)
            if mode == "radial_warp":
                inverse[:2, 3] = plane / 4
            else:
                inverse[:2, 3] = [.25 - .5, np.linalg.norm(plane) / 2 - .5]
        for slot in (0, 1):
            points = [start, start]
            points[slot] = None
            times = [0, 0]
            times[slot] = 7 * 86400e-6
            add(f"local_differential_horizon_{mode}_{slot}", mode, bounds,
                points, [[0, 0, 0, 1]] * 2, times, inverse, projection="ARC")

    for projection in ("CAR", "CEA"):
        add(f"local_full_surface_{projection}", "lati_zenithal", (0, 0, 0, 0),
            [[0, 0, 1]] * 2, [[0, 1, 0, 0]] * 2, projection=projection)

    # Surface maps require a sphere hit and ignore observer-image time correction
    # and enhancement, including when both are enabled.
    for projection in ("CAR", "CEA"):
        for angle in (0, 178, 180):
            for moving, enhanced in ((False, False), (True, False), (False, True), (True, True)):
                add(f"local_directed_ray_{projection}_{angle}_{moving}_{enhanced}", "hpc", (angle, angle, 0, 0),
                    [[0, 0, 1]] * 2 if angle == 0 else [None, None], [[0, 0, 0, 1]] * 2,
                    times=(86400e-6, -2 * 86400e-6) if moving else (0, 0), projection=projection, enhanced=enhanced)
    # Wide-field observer images retain rear-facing rays. A point on each ray
    # supplies independent WCSLIB coordinates, without requiring a Sun-plane hit.
    for degrees in (45, 89, 90, 110, 178):
        angle = math.radians(degrees)
        point = [math.sin(angle), 0, 10 - math.cos(angle)]
        for moving, enhanced in ((False, False), (True, False), (False, True), (True, True)):
            add(f"local_image_{degrees}_{moving}_{enhanced}", "hpc", (degrees, degrees, 0, 0),
                [point, point], [[0, 0, 0, 1]] * 2,
                times=(86400e-6, -2 * 86400e-6) if moving else (0, 0), reference_longitude=degrees, enhanced=enhanced)
    return jobs, checks


def surface_coverage_cases(root, backend, size):
    jobs, checks = [], []
    yy, xx = np.indices((size, size), dtype=float)
    lon, lat = np.radians(360 * (xx + .5) / size - 180), np.radians(160 * (yy + .5) / size - 80)
    world = np.stack((np.cos(lat)*np.sin(lon), np.sin(lat), np.cos(lat)*np.cos(lon)), axis=-1)
    # Degrees per pixel. Include a vertical turn displacement and footprints
    # several turns away, so neither texture-X modulo nor +/- one turn suffices.
    cases = (
        ("partial", [[60/128, 0], [0, 120/128]], [64.5, 64.5], (0, 1), None),
        ("shifted", [[60/128, 0], [0, 120/128]], [-450.5, 64.5], (0, 1), None),
        ("negative", [[-60/128, 0], [0, -120/128]], [64.5, 64.5], (.2, .8), None),
        ("sheared", [[300/128, 80/128], [40/128, 120/128]], [160.5, 70.5], (.15, .85), None),
        ("vertical", [[0, -360/128], [120/128, 0]], [64.5, 200.5], (.2, .8), None),
        ("wide_slit", [[720/128, 0], [0, 120/128]], [96.5, 64.5], (.05, .3), None),
        ("far_shift", [[60/128, 0], [0, 120/128]], [-3007.5, 64.5], (0, 1), None),
        ("user_rotation", [[120/128, 0], [0, 360/128]], [64.5, 200.5], (.2, .8), [[0, -1], [1, 0]]),
    )
    for projection in ("CAR", "CEA"):
        for name, matrix, reference, slit, adjustment in cases:
            header = image_header(projection)
            header["CRVAL1"] = header["CRVAL2"] = 0
            header["CRPIX1"], header["CRPIX2"] = reference
            for row in range(2):
                for col in range(2):
                    header[f"CD{row+1}_{col+1}"] = matrix[row][col]
            secondary = header.copy()
            secondary["CRPIX1"] += 11
            secondary["CRPIX2"] -= 9
            uv, valid = reference_pixels(header, world, slit=slit, adjustment=adjustment)
            other_uv, other_valid = reference_pixels(secondary, world, slit=slit, adjustment=adjustment)
            name = f"coverage_{projection}_{name}"
            meta = cpu.build_jhv_meta(header)
            job = gpu.common_job("lati_zenithal", Path(name), size, root, meta, (-180, 180, -80, 80), True, backend, name)
            job.update(productionCoordinates=True, slit=slit, planeAdjustment=adjustment,
                       coordinateSize=[meta.pixel_width, meta.pixel_height])
            jobs.append(job)
            checks.append(("coordinates", uv, valid, None))
            jobs.append({**job, "name": name + "_comparison", "difference": True,
                         "secondary": {**gpu.wcs_job_fields(cpu.build_jhv_meta(secondary)), "planeAdjustment": adjustment},
                         "outputPath": str(root / f"{name}_comparison.rgba32f")})
            assert (valid & other_valid).any(), name
            checks.append(("comparison", np.concatenate((uv, other_uv), axis=-1), valid & other_valid, None))
        for name, matrix, reference, slit, expected in (
            ("right_edge", [[360/128, 0], [0, 180/128]], [128.5, 64.5], (0, 1), [1, .5]),
            ("shift_to_edge", [[360/128, 0], [0, 180/128]], [-127.5, 64.5], (0, 1), [0, .5]),
            ("vertical_edge", [[0, 360/128], [180/128, 0]], [64.5, 256.5], (0, 1), [.5, 0]),
            ("slit_point", [[360/128, 0], [0, 180/128]], [64.5, 64.5], (.5, .5), [.5, .5]),
            ("empty_slit_turns", [[360/128, 0], [0, 180/128]], [32.5, 64.5], (.5, .5), None),
            ("longitude_seam", [[360/128, 0], [0, 180/128]], [64.5, 64.5], (0, 1), [0, .5]),
        ):
            header = image_header(projection)
            header["CRVAL1"] = header["CRVAL2"] = 0
            header["CRPIX1"], header["CRPIX2"] = reference
            for row in range(2):
                for col in range(2):
                    header[f"CD{row+1}_{col+1}"] = matrix[row][col]
            seam = name == "longitude_seam"
            uv, valid = reference_pixels(header, np.array([0., 0., -1. if seam else 1.]), slit=slit)
            assert bool(valid) == (expected is not None), name
            if expected is not None:
                np.testing.assert_allclose(uv, expected, rtol=0, atol=1e-12)
            name = f"coverage_{projection}_{name}"
            job = gpu.common_job("ortho" if seam else "lati_zenithal", Path(name), 1, root, cpu.build_jhv_meta(header), (0, 0, 0, 0), True, backend, name)
            job.update(productionCoordinates=True, difference=True, slit=slit)
            if seam:
                job["solarToSource"] = [0, 1, 0, 0]
            jobs.append(job)
            checks.append(("local", np.tile(uv, 2) if valid else np.zeros(4), None, None))
    # Retain the real AIA header, including its half-pixel longitude origin.
    with fits.open(gpu.REPO_ROOT / "extra/test/data/syn_AIA_171_2026-01-12T00-00-00_f_V3.fits") as hdul:
        header = cpu.find_image_hdu(hdul, None).header
        meta = cpu.build_jhv_meta(header)
        uv, valid = reference_pixels(header, world)
        job = gpu.common_job("lati_zenithal", Path("coverage_AIA"), size, root, meta, (-180, 180, -80, 80), True, backend, "coverage_AIA")
        job.update(productionCoordinates=True, coordinateSize=[meta.pixel_width, meta.pixel_height])
        jobs.append(job)
        checks.append(("coordinates", uv, valid, None))
    return jobs, checks


def native_frame_cases(root, backend, size):
    jobs, checks = [], []
    yy, xx = np.indices((size, size), dtype=float)
    for projection in ("TAN", "ARC", "AZP", "ZPN", "CAR", "CEA"):
        surface = projection in ("CAR", "CEA")
        bounds = (-179.123, 179.987, -84.321, 84.987) if surface else (15.123, 54.987, 4.321, 35.987)
        lon = np.radians(bounds[0] + (xx + .5) / size * (bounds[1] - bounds[0]))
        lat = np.radians(bounds[2] + (yy + .5) / size * (bounds[3] - bounds[2]))
        if surface:
            world = np.stack((np.cos(lat)*np.sin(lon), np.sin(lat), np.cos(lat)*np.cos(lon)), axis=-1)
        else:
            # A point on each directed observer ray, independently constructed.
            world = np.stack((np.cos(lat)*np.sin(lon), np.sin(lat), 10-np.cos(lat)*np.cos(lon)), axis=-1)
        for variant, normalization in (("tilted", .9), ("pole", 1.1), ("fiducial", 1.), ("offset", .93)):
            # WcsInterpreter fixes map units to radians; only observer normalization varies.
            normalization = 1. if surface else normalization
            header = image_header(projection)
            header["CRVAL1"], header["CRVAL2"] = 35., 20.
            if variant == "pole":
                header["LONPOLE"] = 30. if surface else 90.
            if variant in ("fiducial", "offset"):
                header["PV1_1"], header["PV1_2"] = 10., 15. if surface else 75.
            if variant == "offset":
                header["PV1_0"] = 1.
            if projection == "ZPN":
                header["PV2_0"] = .004
            secondary = header.copy()
            secondary["CRVAL1"] += 3
            secondary["CRVAL2"] -= 2
            adjustment = [[.96, -.28], [.28, .96]] if variant == "offset" else None
            uv, valid = reference_pixels(header, world, adjustment=adjustment)
            other_uv, other_valid = reference_pixels(secondary, world, adjustment=adjustment)
            name = f"native_{projection}_{variant}"
            job = gpu.common_job("lati_zenithal" if surface else "hpc", Path(name), size, root,
                                 cpu.build_jhv_meta(header), bounds, True, backend, name)
            job.update(productionCoordinates=True)
            if adjustment is not None:
                job["planeAdjustment"] = adjustment
            secondary_fields = gpu.wcs_job_fields(cpu.build_jhv_meta(secondary))
            # Change internal plane units without changing the FITS WCS or its source pixels.
            for image in (job, secondary_fields):
                image["wcsHeader"]["unitsPerRad"] *= normalization
            assert valid.any(), name
            jobs.append(job)
            checks.append(("coordinates", uv, valid, None))
            jobs.append({**job, "name": name + "_comparison", "difference": True,
                         "secondary": {**secondary_fields,
                                       **({"planeAdjustment": adjustment} if adjustment is not None else {})},
                         "outputPath": str(root / f"{name}_comparison.rgba32f")})
            assert (valid & other_valid).any(), name
            checks.append(("comparison", np.concatenate((uv, other_uv), axis=-1), valid & other_valid, None))
    return jobs, checks


def orthographic_tan_cases(root, backend):
    # Real validation-note headers retain their original image scale and CRVAL.
    # Compare source pixels directly with an affine solve, independently of WCSLIB.
    jobs, checks = [], []
    data = Path(__file__).resolve().parent.parent / "data"
    for filename in ("sample.171.fits", "solo_L2_eui-fsi174-image_20251002T150055171_V00.fits", "20241224_194245_d4c2A.fts"):
        with fits.open(data / filename) as hdul:
            header = cpu.find_image_hdu(hdul, None).header.copy()
        meta = cpu.build_jhv_meta(header)
        for adjusted in (False, True):
            # Orthographic TAN follows source XY, independently of the formal native pole.
            if adjusted:
                header["LONPOLE"] = 90.
            delta = np.array([60., -45.]) if adjusted else np.zeros(2)
            adjustment = [[.96, -.28], [.28, .96]] if adjusted else None
            name = f"orthographic_TAN_{filename}_{adjusted}"
            job = gpu.common_job("ortho", Path(name), 1, root, meta, (0, 1, 0, 1), True, backend, name)
            inverse = np.eye(4)
            inverse[:2, :2] = 0
            inverse[:2, 3] = [.48, -.36]
            job.update(productionCoordinates=True, inverseMVP=inverse.T.ravel().tolist(),
                       coordinateSize=[meta.pixel_width, meta.pixel_height])
            if adjustment is not None:
                job["planeAdjustment"] = adjustment
            job["wcsHeader"].update(deltaCRVAL1=float(delta[0]), deltaCRVAL2=float(delta[1]))
            secondary = header.copy()
            secondary["CRPIX1"] += 7
            secondary["CRPIX2"] -= 3
            job["secondary"] = gpu.wcs_job_fields(cpu.build_jhv_meta(secondary))
            job["secondary"]["wcsHeader"].update(deltaCRVAL1=float(delta[0]), deltaCRVAL2=float(delta[1]))
            if adjustment is not None:
                job["secondary"]["planeAdjustment"] = adjustment
            coordinates = []
            for image_header in (header, secondary):
                expected_header = image_header.copy()
                # Convert the user arcseconds into the header's own angular units.
                for axis in (1, 2):
                    degree_per_unit = cpu.angular_header_value_to_deg(1., cpu.default_angular_cunit(header, axis))
                    expected_header[f"CRVAL{axis}"] = header.get(f"CRVAL{axis}", 0.) + delta[axis - 1] / 3600 / degree_per_unit
                uv, valid = reference_pixels(expected_header, np.array([.48, -.36, .8]),
                                             adjustment=adjustment, mode="ortho")
                assert valid, (name, uv)
                coordinates.append(uv)
            jobs.append(job)
            checks.append(("coordinates", coordinates[0].reshape(1, 1, 2), np.ones((1, 1), bool), None))
            jobs.append({**job, "name": name + "_comparison", "difference": True,
                         "outputPath": str(root / f"{name}_comparison.rgba32f")})
            checks.append(("comparison", np.concatenate(coordinates).reshape(1, 1, 4), np.ones((1, 1), bool), None))
    return jobs, checks


def run_ortho_hpc_comparison(root, backend, electron):
    # Match a straight-ahead +/-1-Rsun Ortho frame to HPC angles +/-atan(1/D).
    # Use the same fixed dense raster on both backends, independently of the
    # smaller raster used by the general production-fragment cases.
    size = 513
    yy, xx = np.indices((size, size), dtype=float)
    x, y = 2 * (xx + .5) / size - 1, 2 * (yy + .5) / size - 1
    disk = x*x + y*y <= 1
    world = np.stack((x, y, np.sqrt(np.maximum(0, 1-x*x-y*y))), axis=-1)
    jobs, contexts = [], []
    for label, filename, hdu_index in gpu.TAN_SCREEN_CASES:
        with fits.open(gpu.REPO_ROOT / "extra/test/data" / filename) as hdul:
            header = cpu.find_image_hdu(hdul, hdu_index).header.copy()
        meta = cpu.build_jhv_meta(header)
        alpha = math.atan2(1., meta.observer_distance)
        bounds = np.degrees([-alpha, alpha, -alpha, alpha]).tolist()
        # A point on each HPC observer ray. Astropy supplies the FITS conversion;
        # reference_pixels() independently solves the Orthographic TAN affine mapping.
        hpc_world = np.stack((meta.observer_distance * np.tan(x*alpha),
                              meta.observer_distance * np.tan(y*alpha) / np.cos(x*alpha),
                              np.zeros_like(x)), axis=-1)
        references = []
        for variant in ("orthographic", "hpc", "formal"):
            mode = "hpc" if variant == "hpc" else "ortho"
            name = f"ortho_hpc_{label}_{variant}"
            job = gpu.common_job(mode, Path(filename), size, root, meta,
                                 bounds if mode == "hpc" else (0, 1, 0, 1), False, backend, name)
            job.update(productionCoordinates=True, formalTanControl=variant == "formal")
            jobs.append(job)
            uv, valid = reference_pixels(header, hpc_world if mode == "hpc" else world,
                                         mode="ortho" if variant == "orthographic" else None)
            if not valid.all():
                raise AssertionError(f"{label}: fixture no longer covers the complete comparison frame")
            references.append(uv)
        contexts.append((label, meta, references))

    results = gpu.run_electron_jobs(electron, jobs, backend)
    summary = {"backend": backend, "render_size": size,
               "alignment": "identity camera/source rotations, zero time and user adjustments; Ortho +/-1 Rsun, HPC +/-atan(1/D)",
               "images": []}
    for index, (label, meta, references) in enumerate(contexts):
        image_jobs = jobs[3*index:3*index+3]
        pixels = [gpu.read_job_pixels(job) for job in image_jobs]
        source_size = np.array([meta.pixel_width, meta.pixel_height])
        entry = {"image": label, "source_size": source_size.tolist(),
                 "renderer": results[3*index]["renderer"], "reference_max_source_px": {}, "regions": {}}
        for variant, actual, expected in zip(("orthographic", "hpc", "formal"), pixels, references, strict=True):
            # Direct-ray HPC must avoid cancellation at observer-sized coordinates.
            # Coverage must match everywhere, including beyond the nominal solar radius.
            verify_coordinates(actual, expected, np.ones((size, size), bool), source_size=source_size,
                               tolerance=.002 if variant == "hpc" else .05)
            entry["reference_max_source_px"][variant] = float(np.max(np.abs(actual[..., :2] - expected) * source_size))
        # EUV emission can extend past the nominal limb; this band must remain fully sampled.
        annulus = (x*x + y*y >= .95**2) & (x*x + y*y <= 1.05**2)
        reference_error = np.max(np.abs(pixels[1][..., :2] - references[1]) * source_size, axis=-1)
        for region, mask in (("disk", disk), ("off_limb", ~disk), ("limb_annulus", annulus), ("full_frame", np.ones_like(disk))):
            metrics = {"samples": int(mask.sum()),
                       "hpc_vs_reference": {"max_source_px": float(reference_error[mask].max()),
                                            "rms_source_px": float(np.sqrt(np.mean(reference_error[mask]**2)))}}
            for variant, actual in (("orthographic", pixels[0]), ("formal", pixels[2])):
                error = np.max(np.abs(actual[..., :2].astype(float) - pixels[1][..., :2]) * source_size, axis=-1)
                metrics[variant + "_vs_hpc"] = {"max_source_px": float(error[mask].max()),
                                                "rms_source_px": float(np.sqrt(np.mean(error[mask]**2)))}
            entry["regions"][region] = metrics
            print(f"Ortho/HPC {label} {region}: Orthographic TAN max/RMS "
                  f"{metrics['orthographic_vs_hpc']['max_source_px']:.9g}/{metrics['orthographic_vs_hpc']['rms_source_px']:.9g} px; "
                  f"formal max/RMS {metrics['formal_vs_hpc']['max_source_px']:.9g}/{metrics['formal_vs_hpc']['rms_source_px']:.9g} px")
        summary["images"].append(entry)
    (root / "ortho-hpc-comparison.json").write_text(json.dumps(summary, indent=2) + "\n")
    for entry in summary["images"]:
        for region in ("disk", "full_frame"):
            metrics = entry["regions"][region]
            # This is the display-policy regression: the aligned Orthographic TAN
            # mapping stays subpixel and much closer to HPC than formal TAN.
            orthographic = metrics["orthographic_vs_hpc"]["max_source_px"]
            formal = metrics["formal_vs_hpc"]["max_source_px"]
            if orthographic >= 1 or orthographic >= formal / 10:
                raise AssertionError(f"{entry['image']} {region}: Orthographic TAN {orthographic} px, formal TAN {formal} px")
    print(f"PASS: {len(jobs)} Ortho/HPC production-fragment cases with independent references and formal-TAN controls")


def trig_cases(root, backend):
    # Dense float inputs plus adjacent floats at every quadrant and range-reduction boundary.
    angles = np.linspace(-4*np.pi, 4*np.pi, 256*128, dtype=np.float32)
    critical = np.array([k*np.pi/4 for k in range(-16, 17)], np.float32)
    critical = np.concatenate((critical, np.nextafter(critical, np.float32(np.inf)),
                               np.nextafter(critical, np.float32(-np.inf))))
    angles[:len(critical)] = critical
    angles = angles.reshape(128, 256)
    path = root / "trig-input.f32"
    angles.tofile(path)
    expected = np.stack((np.sin(angles.astype(float)), np.cos(angles.astype(float))), axis=-1)
    jobs, checks = [], []
    for variant in ("selected", "polynomial", "builtin"):
        name = "input_trig_" + variant
        job = gpu.common_job("trig", Path(name), 256, root, cpu.build_jhv_meta(image_header("TAN")),
                             (0, 1, 0, 1), True, backend, name)
        job.update(height=128, textureWidth=256, textureHeight=128,
                   sourceTexture={"path": str(path), "format": "R32F"},
                   trigBuiltin=variant == "builtin", trigPolynomial=variant == "polynomial")
        jobs.append(job)
        checks.append(("trig", expected, angles, variant == "builtin"))
    return jobs, checks


def run_cases(root, backend, size, electron, local_only=False, coverage_only=False, ortho_hpc_only=False):
    if ortho_hpc_only:
        run_ortho_hpc_comparison(root, backend, electron)
        return 0
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
    for projection in (() if local_only or coverage_only else ("TAN", "ARC", "AZP", "ZPN", "CAR", "CEA")):
        surface = projection in ("CAR", "CEA")
        other_modes = ("hpc", "lati_zenithal", "radial_warp", "rect_warp")
        for mode, distance, degrees, scale, format in (
            *variants,
            *((mode, distance, degrees, 0.5, "R16F") for mode in other_modes
              for distance in ((10, 215) if surface and mode != "lati_zenithal" else (10,))
              for degrees in ((0, 50, 120) if surface and mode != "lati_zenithal" else (0,))),
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
            if surface and mode in gpu.WARP_MODES:
                bounds = (0, 360, 0, 3)  # A coronagraph alongside the map exposes off-limb rays.
            if mode == "ortho":
                x, y = nx * scale + 0.12, ny * scale - 0.08
                world, geometry_valid, on_disk = orthographic_geometry(
                    x, y, rotation, surface, 0 if projection == "TAN" else 1 / distance
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
                    np.ones(nx.shape, dtype=bool) if surface else distance * world[..., 2] >= 1
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
            if surface and mode in ("hpc", "radial_warp", "rect_warp"):
                # Intersect rays from the observer through the display plane with the unit sphere,
                # then undo the displayed view rotation before asking Astropy for map pixels.
                observer = np.array([0, 0, distance])
                ray = world - observer
                ray /= np.linalg.norm(ray, axis=-1, keepdims=True)
                b = distance * ray[..., 2]
                discriminant = b * b - (distance * distance - 1)
                geometry_valid &= discriminant >= 0
                near = -b - np.sqrt(np.maximum(0, discriminant))
                world = (observer + near[..., None] * ray) @ rotation
            uv, image_valid = reference_pixels(header, world, mode=mode)
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
                    "sourceToView": [0, math.sin(angle / 2), 0, math.cos(angle / 2)],
                    "solarToSource": [0, math.sin(angle / 2), 0, math.cos(angle / 2)],
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
                secondary_header, world, mode=mode
            )
            diff_valid = valid & secondary_valid
            difference = (
                sample_image(image, uv, diff_valid)
                - sample_image(diff_image, secondary_uv, diff_valid)
            ) * 0.9 * 2.5 + 0.5
            secondary = {
                **gpu.wcs_job_fields(cpu.build_jhv_meta(secondary_header)),
                "sourceToView": job["sourceToView"],
                "solarToSource": job["solarToSource"],
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

            if projection == "ZPN" and degrees == 50 and mode == "ortho":
                # A tilted source plane replaces some sphere hits. Its depth must
                # use the view-space plane hit, independently of the comparison's pose.
                sphere_z = np.sqrt(np.maximum(0, 1 - x*x - y*y))
                fallback = ~on_disk | (math.sin(angle)*x + math.cos(angle)*sphere_z < 1 / distance)
                initial_depth = np.where(on_disk, .5 - sphere_z / 20, 1)
                plane_depth = .5 + math.tan(angle)*x / 20
                other_world, other_geometry_valid, _ = orthographic_geometry(x, y, np.eye(3), horizon=1 / distance)
                _, other_valid = reference_pixels(secondary_header, other_world, mode=mode)
                for calculate_depth in (False, True):
                    image_depth = np.where(fallback, plane_depth, initial_depth) if calculate_depth else initial_depth
                    for difference_mode in (False, True):
                        present = valid & other_geometry_valid & other_valid if difference_mode else valid
                        depth = np.where(present, image_depth, 1)
                        for probe in (.47, .51):
                            add(f"_tilted_depth_{calculate_depth}_{difference_mode}_{probe}", {
                                "colorSmoke": True, "calculateDepth": calculate_depth,
                                "difference": difference_mode, "depthProbe": probe,
                                "secondary": {**secondary, "sourceToView": [0, 0, 0, 1]},
                                "diffSourceTexture": diff_source,
                            }, ("depth", probe <= depth, None, None))

    if not coverage_only:
        new_jobs, new_checks = local_geometry_cases(root, backend)
        jobs.extend(new_jobs)
        checks.extend(new_checks)
    if not local_only:
        new_jobs, new_checks = surface_coverage_cases(root, backend, size)
        jobs.extend(new_jobs)
        checks.extend(new_checks)
    if not local_only and not coverage_only:
        for new_jobs, new_checks in (native_frame_cases(root, backend, size), orthographic_tan_cases(root, backend), trig_cases(root, backend)):
            jobs.extend(new_jobs)
            checks.extend(new_checks)
    results = gpu.run_electron_jobs(electron, jobs, backend)
    failed = []
    for job, result, check in zip(jobs, results, checks, strict=True):
        pixels = gpu.read_job_pixels(job)
        kind, expected, valid, boundary = check
        try:
            if kind == "trig":
                np.testing.assert_array_equal(pixels[..., 2], valid)
                error = float(np.max(np.abs(pixels[..., :2] - expected)))
                print(f"{job['name']}: max absolute error {error:.9g}")
                if not boundary and error > 2e-7:
                    raise AssertionError(f"Input sine/cosine error {error} exceeds 2e-7")
                if job["name"] == "input_trig_selected":
                    control = "input_trig_polynomial" if "SwiftShader" in result["renderer"] else "input_trig_builtin"
                    control_job = next(item for item in jobs if item["name"] == control)
                    np.testing.assert_array_equal(pixels, gpu.read_job_pixels(control_job))
            elif kind == "local":
                if np.all(expected == 0):
                    np.testing.assert_array_equal(pixels, 0)
                else:
                    np.testing.assert_allclose(pixels[0, 0], expected, rtol=0, atol=0.05 / 128)
            elif kind == "comparison":
                # Both UVs may legitimately contain zero; alpha is a coordinate
                # in this diagnostic, so classify discards from the whole pixel.
                present = np.any(pixels != 0, axis=-1)
                verify_footprint(present, valid)
                source_size = np.tile(job.get("coordinateSize", (128, 128)), 2)
                np.testing.assert_allclose(pixels[valid] * source_size, expected[valid] * source_size, rtol=0, atol=.05)
            elif kind == "coordinates":
                verify_coordinates(
                    pixels,
                    expected,
                    valid,
                    source_size=job.get("coordinateSize", (128, 128)),
                    boundary=boundary,
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
    if not local_only and not coverage_only:
        run_ortho_hpc_comparison(root, backend, electron)
    return int(bool(failed))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--backend", choices=("default", "swiftshader"), default="default"
    )
    parser.add_argument("--electron", type=Path, default=gpu.DEFAULT_ELECTRON)
    parser.add_argument("--render-size", type=int, default=96)
    group = parser.add_mutually_exclusive_group()
    group.add_argument("--local-geometry-only", action="store_true")
    group.add_argument("--surface-coverage-only", action="store_true")
    group.add_argument("--ortho-hpc-only", action="store_true", help="Compare aligned TAN Ortho/HPC mappings and formal-TAN controls at 513x513")
    parser.add_argument("--output-dir", type=Path)
    args = parser.parse_args()
    if args.output_dir:
        args.output_dir.mkdir(parents=True, exist_ok=True)
        return run_cases(args.output_dir, args.backend, args.render_size, args.electron, args.local_geometry_only, args.surface_coverage_only, args.ortho_hpc_only)
    with TemporaryDirectory(prefix="jhv-wcs-rendering-") as directory:
        return run_cases(Path(directory), args.backend, args.render_size, args.electron, args.local_geometry_only, args.surface_coverage_only, args.ortho_hpc_only)


if __name__ == "__main__":
    raise SystemExit(main())
