#!/usr/bin/env python3
"""Create reference-quality JHV volume and geometry products from COCONUT CFmesh.

The input CFmesh does not identify its observation time or spatial frame.  This converter assumes
that its Cartesian axes are Carrington-aligned and requires the solution time on the command line.
It resamples the solution once in that model frame, then rotates both output products into the
observer-aligned Heliocentric (SOLX/SOLY/SOLZ) frame used by JHelioviewer.

The volume contains a display-ready, clipped logarithm of electron density.  Qorona converts the
model-normalized mass density to a *relative* electron-density shape by dividing by its mean
molecular weight per electron (1.27 by default); it deliberately supplies no absolute density
normalization.  ASSUMED_ELECTRON_DENSITY_SCALE_M3 below is therefore a producer assumption, not a
calibration recovered from the CFmesh file.  Replace it only when an appropriate model-specific
calibration is known, and preserve the assumption in the product metadata.

The settings below select one high-quality reference conversion.  The density display interval is
configurable because choosing it belongs to the producer.  Resolution, resampling, tracing, and
density calibration remain explicit source settings rather than hidden fast/best modes.  Both
completed products are reopened and validated before the script reports that they were written.

Developed and validated with Qorona 0.4.0, PyVista, Matplotlib, and pygltflib.  Qorona's standard
installation supplies NumPy, SciPy, Astropy, SunPy, and optional Numba acceleration for resampling
and field-line tracing; PyVista installs VTK.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

import astropy.units as u
import numpy as np
import pyvista as pv
import qorona
from astropy.io import fits
from astropy.time import Time
from astropy.wcs import WCS
from matplotlib import colormaps
from pygltflib import GLTF2
from scipy.ndimage import map_coordinates
from sunpy.coordinates.sun import B0, L0, earth_distance
from vtkmodules.vtkIOExport import vtkGLTFExporter

from qorona.field.density import DensityVolume, MEAN_MOLECULAR_WEIGHT
from qorona.field.sampled import SampledField
from qorona.io.readers.coconut.cfmesh import CFmeshReader
from qorona.render.fieldlines import polarity_colours
from qorona.resample import KnnMlsResampler, LogarithmicSpacing, SphericalGrid
from qorona.resample.grid import pad_field
from qorona.trace import lonlat_seeds, trace_field_lines

RSUN_REF = 695_700_000.0
VOLUME_EXTENT = 6.0
VOLUME_SIZE = 256
DEFAULT_LOG_DENSITY_RANGE = (11.2, 13.5)
BLANK = -32768
ASSUMED_ELECTRON_DENSITY_SCALE_M3 = 1.0e14
VOLUME_COMPRESSION = "GZIP_2"

FIELD_N_R = 192
FIELD_N_THETA = 180
FIELD_N_PHI = 360
SEED_N_THETA = 18
SEED_N_PHI = 36
TRACE_RTOL = 1.0e-8
TRACE_CFL = 0.125
CURRENT_SHEET_OPACITY = 0.35
# COOLFluiD's "corona" normalization uses v0 = 4.8e7 cm/s (Guo et al. 2024).
CURRENT_SHEET_VELOCITY_SCALE_KM_S = 480.0
CURRENT_SHEET_VELOCITY_MIN_KM_S = -30.0
CURRENT_SHEET_VELOCITY_MAX_KM_S = 300.0
CURRENT_SHEET_COLORMAP = "turbo"


def main() -> None:
    args = arguments()
    args.output_directory.mkdir(parents=True, exist_ok=True)

    timestamp = normalized_utc_timestamp(args.timestamp)
    source_sha256 = sha256(args.input)
    observer = observer_metadata(timestamp)
    world_to_sol = observer_basis(observer["CRLN_OBS"], observer["CRLT_OBS"])
    solution = CFmeshReader().read(args.input, show_progress=True)
    resampler = KnnMlsResampler()
    field, velocity = build_field(solution, resampler)
    log_density_min, log_density_max = args.log_density_range
    processing = {
        "qoronaVersion": qorona.__version__,
        "source": args.input.name,
        "sourceSha256": source_sha256,
        "sourceCellCount": int(solution.cell_centers.shape[0]),
        "resampler": "k-nearest-neighbour degree-1 moving least squares",
        "minimumNeighbors": resampler.n_neighbors,
        "referenceCellCount": resampler.reference_cell_count,
        "ridge": resampler.ridge,
        "fieldGrid": [FIELD_N_R, FIELD_N_THETA, FIELD_N_PHI],
        "fieldGridRadialSpacing": "logarithmic",
        "volumeInterpolator": "Keys tricubic",
        "meanMolecularWeightPerElectron": MEAN_MOLECULAR_WEIGHT,
    }

    volume_path = args.output_directory / "coconut-corona-density-16.fits"
    write_volume(
        field,
        world_to_sol,
        observer,
        timestamp,
        processing,
        log_density_min,
        log_density_max,
        volume_path,
    )

    scene_path = args.output_directory / "coconut-corona-scene.glb"
    write_scene(
        field, velocity, world_to_sol, observer, timestamp, processing, scene_path
    )

    print(f"Wrote {volume_path}")
    print(f"Wrote {scene_path}")


def arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "input", type=Path, help="COCONUT .CFmesh or .CFmesh.xz solution"
    )
    parser.add_argument(
        "--timestamp", required=True, help="solution observation time (ISO-8601 UTC)"
    )
    parser.add_argument(
        "--output-directory", type=Path, default=Path("extra/test/data")
    )
    parser.add_argument(
        "--log-density-range",
        type=float,
        nargs=2,
        metavar=("MIN", "MAX"),
        default=DEFAULT_LOG_DENSITY_RANGE,
        help="clipped display interval for log10(ne / m^-3) "
        f"(default: {DEFAULT_LOG_DENSITY_RANGE[0]} {DEFAULT_LOG_DENSITY_RANGE[1]})",
    )
    args = parser.parse_args()
    minimum, maximum = args.log_density_range
    if not np.isfinite(minimum) or not np.isfinite(maximum) or minimum >= maximum:
        parser.error("--log-density-range requires two finite values with MIN < MAX")
    return args


def normalized_utc_timestamp(value: str) -> str:
    try:
        time = Time(value, scale="utc")
    except ValueError as error:
        raise ValueError(f"invalid UTC observation time {value!r}") from error
    time.precision = 3
    return time.utc.isot


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def observer_metadata(timestamp: str) -> dict[str, float]:
    time = Time(timestamp, scale="utc")
    return {
        "DSUN_OBS": float(earth_distance(time).to_value(u.m)),
        "CRLN_OBS": float(L0(time).to_value(u.deg)),
        "CRLT_OBS": float(B0(time).to_value(u.deg)),
        "RSUN_REF": RSUN_REF,
    }


def observer_basis(longitude_degrees: float, latitude_degrees: float) -> np.ndarray:
    """Return the orthonormal rotation from Carrington-aligned xyz to SOLX/SOLY/SOLZ.

    The returned rows are the solar-west, solar-north, and toward-observer unit vectors expressed
    in Carrington-aligned model coordinates.  For column vectors, ``sol = basis @ world`` and
    ``world = basis.T @ sol``.
    """
    longitude = np.deg2rad(longitude_degrees)
    latitude = np.deg2rad(latitude_degrees)
    toward_observer = np.array(
        [
            np.cos(latitude) * np.cos(longitude),
            np.cos(latitude) * np.sin(longitude),
            np.sin(latitude),
        ]
    )
    north = np.array([0.0, 0.0, 1.0]) - np.sin(latitude) * toward_observer
    north /= np.linalg.norm(north)
    west = np.cross(north, toward_observer)
    return np.stack((west, north, toward_observer))


def build_field(
    solution, resampler: KnnMlsResampler
) -> tuple[SampledField, np.ndarray]:
    grid = SphericalGrid(
        spacing=LogarithmicSpacing(inner=1.0, outer=VOLUME_EXTENT),
        n_r=FIELD_N_R,
        n_theta=FIELD_N_THETA,
        n_phi=FIELD_N_PHI,
    )
    names = ("Bx", "By", "Bz", "rho", "vx", "vy", "vz")
    missing = [name for name in names if name not in solution.variables]
    if missing:
        raise ValueError(f"COCONUT solution lacks variables: {', '.join(missing)}")
    components = resampler.resample(solution, grid, names, show_progress=True)
    magnetic_field = pad_field(
        np.stack([components[name] for name in names[:3]], axis=-1)
    )
    density = DensityVolume.from_grid_values(grid, components["rho"])
    field = SampledField(
        grid, magnetic_field, solution.metadata.normalization, density=density
    )
    velocity = np.stack([components[name] for name in names[4:]], axis=-1)
    return field, velocity


def write_volume(
    field: SampledField,
    world_to_sol: np.ndarray,
    observer: dict[str, float],
    timestamp: str,
    processing: dict[str, object],
    log_density_min: float,
    log_density_max: float,
    output: Path,
) -> None:
    if field.density is None:
        raise RuntimeError("COCONUT solution has no density field")
    step = 2 * VOLUME_EXTENT / VOLUME_SIZE
    coordinates = -VOLUME_EXTENT + step * (np.arange(VOLUME_SIZE) + 0.5)
    bscale = (log_density_max - log_density_min) / 65534
    bzero = (log_density_min + log_density_max) / 2
    stored = np.full((VOLUME_SIZE,) * 3, BLANK, dtype=np.int16)
    for z_start in range(0, VOLUME_SIZE, 8):
        z_stop = min(z_start + 8, VOLUME_SIZE)
        z, y, x = np.meshgrid(
            coordinates[z_start:z_stop], coordinates, coordinates, indexing="ij"
        )
        sol_points = np.column_stack((x.ravel(), y.ravel(), z.ravel()))
        radius_squared = np.sum(sol_points * sol_points, axis=1)
        valid = (radius_squared >= 1.0) & (radius_squared <= VOLUME_EXTENT**2)
        density = np.zeros(len(sol_points))
        # DensityVolume.sample expects model-frame points.  With row vectors, the inverse of
        # sol = world_to_sol @ world is world = sol @ world_to_sol.
        density[valid] = field.density.sample(sol_points[valid] @ world_to_sol)
        valid &= np.isfinite(density) & (density > 0.0)
        values = np.zeros(len(sol_points))
        values[valid] = np.log10(density[valid] * ASSUMED_ELECTRON_DENSITY_SCALE_M3)
        block = stored[z_start:z_stop].reshape(-1)
        block[valid] = np.rint(
            (np.clip(values[valid], log_density_min, log_density_max) - bzero) / bscale
        ).astype(np.int16)

    if not np.any(stored != BLANK):
        raise RuntimeError("volume conversion produced no defined voxels")
    header = fits.PrimaryHDU(stored).header
    header["OBJECT"] = ("COCONUT electron density", "display-ready scalar volume")
    header["BTYPE"] = ("log10 assumed electron density", "log10(ne / m^-3)")
    header["BSCALE"] = (bscale, "log10(ne / m^-3) per stored integer")
    header["BZERO"] = (bzero, "stored zero in log10(ne / m^-3)")
    header["BLANK"] = (BLANK, "undefined voxel")
    header["WCSNAME"] = "Heliocentric-cartesian"
    for axis, ctype in enumerate(("SOLX", "SOLY", "SOLZ"), start=1):
        header[f"CTYPE{axis}"] = ctype
        header[f"CUNIT{axis}"] = "solRad"
        header[f"CRPIX{axis}"] = (VOLUME_SIZE + 1) / 2
        header[f"CRVAL{axis}"] = 0.0
        header[f"CDELT{axis}"] = step
    add_observer_metadata(header, observer, timestamp)
    header["HISTORY"] = (
        f"Generated with Qorona {processing['qoronaVersion']} from {processing['source']}"
    )
    header["HISTORY"] = f"Source SHA-256: {processing['sourceSha256']}"
    header["HISTORY"] = (
        f"MLS resampling: minimum k={processing['minimumNeighbors']}, reference cells="
        f"{processing['referenceCellCount']}, source cells={processing['sourceCellCount']}"
    )
    header["HISTORY"] = (
        f"{FIELD_N_R}x{FIELD_N_THETA}x{FIELD_N_PHI} "
        f"{processing['fieldGridRadialSpacing']} spherical field grid"
    )
    header["HISTORY"] = (
        f"{processing['volumeInterpolator']} interpolation to Cartesian voxel centres"
    )
    header["HISTORY"] = (
        f"Qorona density is model-normalized rho/{MEAN_MOLECULAR_WEIGHT} (relative shape only)"
    )
    density_scale = f"{ASSUMED_ELECTRON_DENSITY_SCALE_M3:.0e}".replace("e+", "e")
    header["HISTORY"] = (
        f"Assumed ne=(Qorona density)*{density_scale} m^-3; not calibrated by CFmesh"
    )
    header["HISTORY"] = (
        f"Stored scalar is log10(ne/m^-3), clipped to "
        f"[{log_density_min}, {log_density_max}]"
    )
    write_compressed_fits_volume(output, stored, header)
    validate_fits_volume(output, stored, header)


def write_compressed_fits_volume(
    output: Path, stored: np.ndarray, image_header: fits.Header
) -> None:
    header = image_header.copy()
    for keyword in ("SIMPLE", "EXTEND", "CHECKSUM", "DATASUM"):
        header.remove(keyword, ignore_missing=True)
    bscale = header["BSCALE"]
    bzero = header["BZERO"]
    compressed = fits.CompImageHDU(
        data=stored,
        header=header,
        compression_type=VOLUME_COMPRESSION,
        # Astropy uses NumPy axis order here. One xy plane per tile is efficient and is the
        # largest higher-dimensional tiling accepted by JHV's nom-tam-fits reader.
        tile_shape=(1, stored.shape[1], stored.shape[2]),
        do_not_scale_image_data=True,
        uint=False,
    )
    # CompImageHDU treats the supplied integer array as physical values and removes these cards
    # while constructing the logical image header. The array actually contains stored integers,
    # so restore their physical interpretation before writing the compressed image.
    compressed.header["BSCALE"] = bscale
    compressed.header["BZERO"] = bzero
    fits.HDUList([fits.PrimaryHDU(), compressed]).writeto(output, overwrite=True)
    # Add checksums to the physical HDUs after compression. A fixed comment makes an otherwise
    # identical generated fixture byte-for-byte reproducible instead of embedding the wall time.
    checksum_comment = "COCONUT sample product"
    with fits.open(
        output, mode="update", disable_image_compression=True
    ) as physical_hdus:
        for hdu in physical_hdus:
            hdu.add_datasum(when=checksum_comment)
            hdu.add_checksum(when=checksum_comment, override_datasum=True)


def validate_fits_volume(
    path: Path, expected: np.ndarray, expected_header: fits.Header
) -> None:
    # First inspect the physical binary-table HDU so CHECKSUM and DATASUM validate the bytes that
    # are actually stored in the file rather than Astropy's decompressed logical image facade.
    with fits.open(
        path, disable_image_compression=True, checksum=True
    ) as physical_hdus:
        physical_hdus.verify("exception")
        for hdu in physical_hdus:
            if hdu.header.get("CHECKSUM") is not None and hdu.verify_checksum() != 1:
                raise RuntimeError("FITS checksum validation failed")
            if hdu.header.get("DATASUM") is not None and hdu.verify_datasum() != 1:
                raise RuntimeError("FITS datasum validation failed")

    # Then reopen through the tiled-image convention and prove that the completed file restores
    # the exact stored integers and the logical image metadata expected by JHV.
    with fits.open(path, do_not_scale_image_data=True, uint=False) as hdus:
        hdus.verify("exception")
        compressed_hdus = [hdu for hdu in hdus if isinstance(hdu, fits.CompImageHDU)]
        if len(compressed_hdus) != 1:
            raise RuntimeError(
                "FITS file does not contain exactly one compressed image"
            )
        hdu = compressed_hdus[0]
        header = hdu.header
        if (
            header["BITPIX"] != 16
            or hdu.data.dtype.kind != "i"
            or hdu.data.shape != expected.shape
            or hdu.compression_type != VOLUME_COMPRESSION
            or tuple(hdu.tile_shape) != (1, expected.shape[1], expected.shape[2])
        ):
            raise RuntimeError(
                f"unexpected compressed FITS array: {hdu.data.dtype} {hdu.data.shape}"
            )
        if header["BLANK"] != BLANK:
            raise RuntimeError("unexpected FITS BLANK value")
        if not np.array_equal(hdu.data, expected):
            raise RuntimeError(
                "compressed FITS pixels do not match the generated volume"
            )
        metadata_keys = (
            "OBJECT",
            "BTYPE",
            "BSCALE",
            "BZERO",
            "BLANK",
            "WCSNAME",
            "CTYPE1",
            "CTYPE2",
            "CTYPE3",
            "CUNIT1",
            "CUNIT2",
            "CUNIT3",
            "CRPIX1",
            "CRPIX2",
            "CRPIX3",
            "CRVAL1",
            "CRVAL2",
            "CRVAL3",
            "CDELT1",
            "CDELT2",
            "CDELT3",
            "DATE-OBS",
            "DSUN_OBS",
            "CRLN_OBS",
            "CRLT_OBS",
            "RSUN_REF",
        )
        for keyword in metadata_keys:
            actual_value = header.get(keyword)
            expected_value = expected_header.get(keyword)
            # Floating-point header values are serialized as decimal text in 80-character FITS
            # cards. Compare their recovered values at that representation's precision; image
            # integers, checksums, and all non-floating metadata remain exact checks.
            matches = (
                np.isclose(actual_value, expected_value, rtol=1.0e-14, atol=0.0)
                if isinstance(expected_value, float)
                else actual_value == expected_value
            )
            if not matches:
                raise RuntimeError(f"unexpected FITS {keyword} value")
        if list(header["HISTORY"]) != list(expected_header["HISTORY"]):
            raise RuntimeError("unexpected FITS HISTORY")

        reference_pixel = (VOLUME_SIZE + 1) / 2
        world = WCS(header, fix=False).all_pix2world(
            [[1.0] * 3, [reference_pixel] * 3, [float(VOLUME_SIZE)] * 3], 1
        )
        half_step = VOLUME_EXTENT / VOLUME_SIZE
        expected_world = np.array(
            [
                [-VOLUME_EXTENT + half_step] * 3,
                [0.0] * 3,
                [VOLUME_EXTENT - half_step] * 3,
            ]
        )
        if not np.allclose(world, expected_world, rtol=0.0, atol=1.0e-12):
            raise RuntimeError("unexpected FITS voxel-centre coordinates")


def add_observer_metadata(
    header: fits.Header, observer: dict[str, float], timestamp: str
) -> None:
    header["DATE-OBS"] = (timestamp, "solution observation time")
    header["DSUN_OBS"] = (observer["DSUN_OBS"], "[m] observer distance")
    header["CRLN_OBS"] = (
        observer["CRLN_OBS"],
        "[deg] Carrington longitude of observer",
    )
    header["CRLT_OBS"] = (observer["CRLT_OBS"], "[deg] Carrington latitude of observer")
    header["RSUN_REF"] = (observer["RSUN_REF"], "[m] assumed physical solar radius")


def write_scene(
    field: SampledField,
    velocity: np.ndarray,
    world_to_sol: np.ndarray,
    observer: dict[str, float],
    timestamp: str,
    processing: dict[str, object],
    output: Path,
) -> None:
    seeds = lonlat_seeds(1.0, n_theta=SEED_N_THETA, n_phi=SEED_N_PHI)
    lines = trace_field_lines(
        field,
        seeds,
        store_path=True,
        show_progress=True,
        device="cpu",
        precision="float64",
        rtol=TRACE_RTOL,
        cfl=TRACE_CFL,
    )
    colors = polarity_colours(field, lines, 1.0, VOLUME_EXTENT)
    current_sheet = extract_current_sheet(field, velocity, world_to_sol)
    open_boundary_points = (
        lines.feet[lines.is_open].reshape(-1, 3) @ world_to_sol.T
    ).astype(np.float32)
    open_boundary_colors = np.rint(
        np.column_stack(
            (
                np.repeat(colors[lines.is_open], 2, axis=0),
                np.ones(len(open_boundary_points)),
            )
        )
        * 255
    ).astype(np.uint8)

    positions = []
    vertex_colors = []
    polylines = []
    vertex_count = 0
    for path, color, complete in zip(
        lines.paths, colors, lines.is_complete, strict=True
    ):
        if not complete:
            continue
        # Field-line paths are model-frame row vectors; world_to_sol.T applies the forward
        # world-to-SOL rotation to that representation.  Rotate in float64, then perform the one
        # unavoidable conversion to the float32 position representation required by glTF.
        transformed = (np.asarray(path, dtype=np.float64) @ world_to_sol.T).astype(
            np.float32
        )
        # Boundary landing can append a point which rounds to the preceding float32 position.
        # GL_LINES must not contain zero-length edges: Assimp otherwise separates them as points.
        transformed = transformed[
            np.concatenate(
                ([True], np.any(transformed[1:] != transformed[:-1], axis=1))
            )
        ]
        if len(transformed) < 2:
            continue
        first = vertex_count
        vertex_count += len(transformed)
        positions.append(transformed)
        rgba = np.rint(np.append(color, 1.0) * 255).astype(np.uint8)
        vertex_colors.append(np.tile(rgba, (len(transformed), 1)))
        polylines.append(
            np.concatenate(([len(transformed)], np.arange(first, vertex_count)))
        )

    if not positions:
        raise RuntimeError("field-line tracing produced no complete paths")
    position_array = np.concatenate(positions)
    color_array = np.concatenate(vertex_colors)
    polyline_array = np.concatenate(polylines)
    metadata = {
        "DATE-OBS": timestamp,
        **observer,
        "CTYPE1": "SOLX",
        "CTYPE2": "SOLY",
        "CTYPE3": "SOLZ",
        "CUNIT1": "solRad",
        "CUNIT2": "solRad",
        "CUNIT3": "solRad",
        "WCSNAME": "Heliocentric-cartesian",
        "PROCESSING": {
            **processing,
            "tracer": "DOPRI5",
            "tracerDevice": "cpu",
            "rtol": TRACE_RTOL,
            "cfl": TRACE_CFL,
            "precision": "float64",
            "seedGrid": [SEED_N_THETA, SEED_N_PHI],
            "incompletePathsDiscarded": True,
            "currentSheet": {
                "definition": "B_r=0",
                "grid": [FIELD_N_R, FIELD_N_THETA, FIELD_N_PHI],
                "meshing": "VTK flying edges",
                "velocityInterpolation": "trilinear on the spherical field grid",
                "colorQuantity": "radial velocity",
                "colorMap": CURRENT_SHEET_COLORMAP,
                "colorRangeKmPerS": [
                    CURRENT_SHEET_VELOCITY_MIN_KM_S,
                    CURRENT_SHEET_VELOCITY_MAX_KM_S,
                ],
                "modelVelocityUnitKmPerS": CURRENT_SHEET_VELOCITY_SCALE_KM_S,
                "dataRangeKmPerS": [
                    float(np.min(current_sheet.point_data["radialVelocity"])),
                    float(np.max(current_sheet.point_data["radialVelocity"])),
                ],
                "vertices": current_sheet.n_points,
                "triangles": current_sheet.n_cells,
            },
            "openFieldBoundaryPoints": {
                "definition": "inner and outer boundary endpoints of complete open field lines",
                "count": len(open_boundary_points),
                "colorQuantity": "polarity of the corresponding field line",
            },
        },
    }
    segment_count = sum(len(position) - 1 for position in positions)
    write_scene_glb(
        output,
        position_array,
        color_array,
        polyline_array,
        segment_count,
        current_sheet,
        open_boundary_points,
        open_boundary_colors,
        metadata,
    )


def extract_current_sheet(
    field: SampledField, velocity: np.ndarray, world_to_sol: np.ndarray
) -> pv.PolyData:
    grid = field.grid
    if velocity.shape != (grid.n_r, grid.n_theta, grid.n_phi, 3):
        raise ValueError(
            "velocity must contain one Cartesian vector per field-grid node"
        )
    if not np.isfinite(velocity).all():
        raise ValueError("velocity contains non-finite values")
    theta = grid.colatitudes[:, None]
    phi = grid.azimuths[None, :]
    radial_direction = np.stack(
        np.broadcast_arrays(
            np.sin(theta) * np.cos(phi),
            np.sin(theta) * np.sin(phi),
            np.cos(theta) * np.ones_like(phi),
        ),
        axis=-1,
    )
    b_radial = np.einsum(
        "rtpc,tpc->rtp", field.b_at_nodes(), radial_direction, optimize=True
    )
    if not np.isfinite(b_radial).all() or not (
        np.min(b_radial) <= 0.0 <= np.max(b_radial)
    ):
        raise RuntimeError("resampled magnetic field has no finite B_r=0 surface")

    # Close the periodic longitude axis before contouring. ImageData keeps the logical grid
    # implicit, avoiding another full Cartesian copy of the high-resolution magnetic field.
    b_radial = np.concatenate((b_radial, b_radial[:, :, :1]), axis=2).astype(np.float32)
    logical_grid = pv.ImageData(dimensions=b_radial.shape)
    logical_grid.point_data["B_r"] = b_radial.ravel(order="F")
    surface = logical_grid.contour(
        [0.0],
        scalars="B_r",
        compute_normals=False,
        compute_scalars=False,
        method="flying_edges",
    ).triangulate()
    if surface.n_points == 0 or surface.n_cells == 0:
        raise RuntimeError("B_r=0 contouring produced an empty current sheet")

    logical = np.asarray(surface.points, dtype=np.float64)
    periodic_velocity = np.concatenate((velocity, velocity[:, :, :1]), axis=2)
    surface_velocity = np.column_stack(
        [
            map_coordinates(
                periodic_velocity[..., component],
                logical.T,
                order=1,
                mode="nearest",
                prefilter=False,
            )
            for component in range(3)
        ]
    )
    radius = grid.spacing.radius(logical[:, 0] / (grid.n_r - 1))
    colatitude = (logical[:, 1] + 0.5) * (np.pi / grid.n_theta)
    azimuth = logical[:, 2] * (2.0 * np.pi / grid.n_phi)
    sin_colatitude = np.sin(colatitude)
    model_points = np.column_stack(
        (
            radius * sin_colatitude * np.cos(azimuth),
            radius * sin_colatitude * np.sin(azimuth),
            radius * np.cos(colatitude),
        )
    )
    radial_velocity = (
        np.einsum("ij,ij->i", surface_velocity, model_points / radius[:, None])
        * CURRENT_SHEET_VELOCITY_SCALE_KM_S
    )
    normalized_velocity = np.clip(
        (radial_velocity - CURRENT_SHEET_VELOCITY_MIN_KM_S)
        / (CURRENT_SHEET_VELOCITY_MAX_KM_S - CURRENT_SHEET_VELOCITY_MIN_KM_S),
        0.0,
        1.0,
    )
    rgba = colormaps[CURRENT_SHEET_COLORMAP](normalized_velocity, bytes=True)
    rgba[:, 3] = round(255 * CURRENT_SHEET_OPACITY)
    surface.points = np.asarray(model_points @ world_to_sol.T, dtype=np.float32)
    surface.point_data["RGBA"] = np.ascontiguousarray(rgba, dtype=np.uint8)
    surface.point_data["radialVelocity"] = radial_velocity
    surface = surface.clean(tolerance=1.0e-6, absolute=True)
    # Joining the coincident longitude seam can collapse a handful of seam triangles. VTK's
    # cleaner preserves those degeneracies as line or point cells; retain only the polygonal
    # faces so the exported object remains a pure triangle mesh.
    polygon_surface = pv.PolyData(surface.points, surface.faces)
    polygon_surface.point_data["RGBA"] = surface.point_data["RGBA"]
    polygon_surface.point_data["radialVelocity"] = surface.point_data["radialVelocity"]
    surface = polygon_surface.remove_unused_points()
    if not surface.is_all_triangles:
        raise RuntimeError("current-sheet contour is not a triangle mesh")
    return surface


def write_scene_glb(
    output: Path,
    positions: np.ndarray,
    colors: np.ndarray,
    polylines: np.ndarray,
    segment_count: int,
    current_sheet: pv.PolyData,
    boundary_points: np.ndarray,
    boundary_colors: np.ndarray,
    metadata: dict[str, object],
) -> None:
    if (
        positions.ndim != 2
        or positions.shape[1] != 3
        or not np.isfinite(positions).all()
    ):
        raise ValueError("positions must be a finite Nx3 array")
    if colors.shape != (len(positions), 4):
        raise ValueError("colors must contain one RGBA value per position")
    if polylines.ndim != 1 or len(polylines) == 0:
        raise ValueError("polylines must be a non-empty VTK line-cell array")
    if current_sheet.n_points == 0 or current_sheet.n_cells == 0:
        raise ValueError("current sheet must be a non-empty triangle mesh")
    if (
        boundary_points.ndim != 2
        or boundary_points.shape[1] != 3
        or len(boundary_points) == 0
        or not np.isfinite(boundary_points).all()
    ):
        raise ValueError("boundary points must be a finite, non-empty Nx3 array")
    if boundary_colors.shape != (len(boundary_points), 4):
        raise ValueError("boundary colors must contain one RGBA value per point")
    boundary_radii = np.linalg.norm(boundary_points, axis=1)
    inner_boundary = np.isclose(boundary_radii, 1.0, atol=1.0e-5)
    outer_boundary = np.isclose(boundary_radii, VOLUME_EXTENT, atol=1.0e-5)
    if not np.all(inner_boundary | outer_boundary) or not (
        np.any(inner_boundary) and np.any(outer_boundary)
    ):
        raise ValueError("boundary points must include both model boundaries")
    if (
        np.any(boundary_colors[:, 3] != 255)
        or len(np.unique(boundary_colors[:, :3], axis=0)) < 2
    ):
        raise ValueError(
            "boundary-point colors must be opaque and encode both polarities"
        )
    surface_colors = np.asarray(current_sheet.point_data.get("RGBA"))
    if surface_colors.shape != (current_sheet.n_points, 4):
        raise ValueError("current sheet must contain one RGBA value per vertex")
    if (
        np.any(surface_colors[:, 3] != round(255 * CURRENT_SHEET_OPACITY))
        or len(np.unique(surface_colors[:, :3], axis=0)) < 2
    ):
        raise ValueError(
            "current-sheet colors must vary in RGB and use the configured opacity"
        )

    mesh = pv.PolyData(
        np.ascontiguousarray(positions, dtype=np.float32), lines=polylines
    )
    mesh.point_data["RGBA"] = np.ascontiguousarray(colors, dtype=np.uint8)
    point_cloud = pv.PolyData(np.ascontiguousarray(boundary_points, dtype=np.float32))
    point_cloud.point_data["RGBA"] = np.ascontiguousarray(
        boundary_colors, dtype=np.uint8
    )
    plotter = pv.Plotter(off_screen=True)
    try:
        plotter.add_mesh(
            mesh,
            name="COCONUT magnetic field lines",
            scalars="RGBA",
            rgba=True,
            color="white",
            lighting=False,
            show_scalar_bar=False,
        )
        plotter.add_mesh(
            current_sheet,
            name="Heliospheric current sheet",
            scalars="RGBA",
            rgba=True,
            color="white",
            lighting=False,
            show_scalar_bar=False,
        )
        plotter.add_mesh(
            point_cloud,
            name="Open-field-line boundary endpoints",
            style="points",
            scalars="RGBA",
            rgba=True,
            color="white",
            lighting=False,
            show_scalar_bar=False,
        )
        # PyVista's export_gltf() only writes files.  Its underlying VTK exporter can return the
        # identical glTF document in memory, which avoids creating an intermediate .gltf file.
        exporter = vtkGLTFExporter()
        exporter.SetRenderWindow(plotter.render_window)
        exporter.SetInlineData(True)
        exporter.SetSaveNormal(False)
        document = json.loads(exporter.WriteToString())
    finally:
        plotter.close()

    scene_index = document.get("scene", 0)
    scene = document["scenes"][scene_index]
    scene["name"] = "COCONUT corona"
    scene["extras"] = metadata

    line_meshes = []
    surface_meshes = []
    point_meshes = []
    for mesh_index, exported_mesh in enumerate(document.get("meshes", [])):
        modes = {primitive.get("mode", 4) for primitive in exported_mesh["primitives"]}
        if modes == {1}:
            line_meshes.append(mesh_index)
        elif modes == {4}:
            surface_meshes.append(mesh_index)
        elif modes == {0}:
            point_meshes.append(mesh_index)
    if len(line_meshes) != 1 or len(surface_meshes) != 1 or len(point_meshes) != 1:
        raise RuntimeError(
            "VTK did not export one line, one triangle, and one point mesh"
        )

    line_mesh = line_meshes[0]
    surface_mesh = surface_meshes[0]
    point_mesh = point_meshes[0]
    document["meshes"][line_mesh]["name"] = "COCONUT magnetic field lines"
    document["meshes"][surface_mesh]["name"] = "Heliospheric current sheet"
    document["meshes"][point_mesh]["name"] = "Open-field-line boundary endpoints"
    for node in document.get("nodes", []):
        if node.get("mesh") == line_mesh:
            node["name"] = "COCONUT magnetic field lines"
        elif node.get("mesh") == surface_mesh:
            node["name"] = "Heliospheric current sheet"
        elif node.get("mesh") == point_mesh:
            node["name"] = "Open-field-line boundary endpoints"

    surface_primitive = document["meshes"][surface_mesh]["primitives"][0]
    material_index = surface_primitive.get("material")
    if material_index is None or not 0 <= material_index < len(
        document.get("materials", [])
    ):
        raise RuntimeError("VTK current-sheet mesh has no valid material")
    surface_material = document["materials"][material_index]
    surface_material["alphaMode"] = "BLEND"
    surface_material["doubleSided"] = True

    # VTK owns the glTF geometry and buffers.  pygltflib only packages that in-memory document as
    # one binary GLB while preserving the application-specific scene metadata added above.
    GLTF2.gltf_from_json(json.dumps(document)).save_binary(output)

    # Validate the finished file, not the in-memory document: this catches packaging errors and
    # proves that the metadata and rendering attributes survived the GLB round trip.
    validate_scene_glb(
        output,
        len(positions),
        segment_count,
        current_sheet.n_points,
        current_sheet.n_cells,
        len(boundary_points),
        metadata,
    )


def validate_scene_glb(
    path: Path,
    expected_vertex_count: int,
    expected_segment_count: int,
    expected_surface_vertex_count: int,
    expected_surface_triangle_count: int,
    expected_point_count: int,
    expected_metadata: dict[str, object],
) -> None:
    document = GLTF2().load(path)
    scene_index = document.scene if document.scene is not None else 0
    if (
        document.asset is None
        or document.asset.version != "2.0"
        or document.scenes is None
        or len(document.scenes) != 1
        or not 0 <= scene_index < len(document.scenes)
        or document.meshes is None
        or len(document.meshes) != 3
        or document.materials is None
        or len(document.materials) != 3
        or document.accessors is None
        or document.buffers is None
        or len(document.buffers) != 1
    ):
        raise RuntimeError("completed GLB has an unexpected scene structure")

    scene = document.scenes[scene_index]
    primitives = [
        primitive
        for mesh in document.meshes
        for primitive in (mesh.primitives if mesh.primitives is not None else [])
    ]
    line_primitives = [primitive for primitive in primitives if primitive.mode == 1]
    surface_primitives = [primitive for primitive in primitives if primitive.mode == 4]
    point_primitives = [primitive for primitive in primitives if primitive.mode == 0]
    if (
        len(primitives) != 3
        or len(line_primitives) != 1
        or len(surface_primitives) != 1
        or len(point_primitives) != 1
    ):
        raise RuntimeError(
            "completed GLB does not contain one line, one triangle, and one point primitive"
        )

    line = line_primitives[0]
    surface = surface_primitives[0]
    points = point_primitives[0]
    if (
        line.attributes is None
        or line.attributes.POSITION is None
        or line.attributes.COLOR_0 is None
        or line.indices is None
        or line.material is None
        or surface.attributes is None
        or surface.attributes.POSITION is None
        or surface.attributes.COLOR_0 is None
        or surface.indices is None
        or surface.material is None
        or points.attributes is None
        or points.attributes.POSITION is None
        or points.attributes.COLOR_0 is None
        or points.indices is None
        or points.material is None
        or any(
            index is None or not 0 <= index < len(document.accessors)
            for index in (
                line.attributes.POSITION,
                line.attributes.COLOR_0,
                line.indices,
                surface.attributes.POSITION,
                surface.attributes.COLOR_0,
                surface.indices,
                points.attributes.POSITION,
                points.attributes.COLOR_0,
                points.indices,
            )
        )
        or not 0 <= line.material < len(document.materials)
        or not 0 <= surface.material < len(document.materials)
        or not 0 <= points.material < len(document.materials)
        or document.materials[line.material].pbrMetallicRoughness is None
        or document.materials[surface.material].pbrMetallicRoughness is None
        or document.materials[points.material].pbrMetallicRoughness is None
    ):
        raise RuntimeError("completed GLB contains invalid primitive references")

    line_position = document.accessors[line.attributes.POSITION]
    line_color = document.accessors[line.attributes.COLOR_0]
    line_indices = document.accessors[line.indices]
    surface_position = document.accessors[surface.attributes.POSITION]
    surface_color = document.accessors[surface.attributes.COLOR_0]
    surface_indices = document.accessors[surface.indices]
    point_position = document.accessors[points.attributes.POSITION]
    point_color = document.accessors[points.attributes.COLOR_0]
    point_indices = document.accessors[points.indices]
    line_base_color = document.materials[
        line.material
    ].pbrMetallicRoughness.baseColorFactor
    surface_material = document.materials[surface.material]
    surface_base_color = surface_material.pbrMetallicRoughness.baseColorFactor
    point_base_color = document.materials[
        points.material
    ].pbrMetallicRoughness.baseColorFactor
    binary_blob = document.binary_blob()
    if (
        scene.name != "COCONUT corona"
        or scene.extras != expected_metadata
        or line_position.count != expected_vertex_count
        or line_color.count != expected_vertex_count
        or line_indices.count != 2 * expected_segment_count
        or line_color.componentType != 5121
        or line_color.type != "VEC4"
        or line_color.normalized is not True
        or line_base_color != [1.0, 1.0, 1.0, 1.0]
        or surface_position.count != expected_surface_vertex_count
        or surface_color.count != expected_surface_vertex_count
        or surface_indices.count != 3 * expected_surface_triangle_count
        or surface_color.componentType != 5121
        or surface_color.type != "VEC4"
        or surface_color.normalized is not True
        or surface_material.alphaMode != "BLEND"
        or surface_material.doubleSided is not True
        or surface_base_color != [1.0, 1.0, 1.0, 1.0]
        or point_position.count != expected_point_count
        or point_color.count != expected_point_count
        or point_indices.count != expected_point_count
        or point_color.componentType != 5121
        or point_color.type != "VEC4"
        or point_color.normalized is not True
        or point_base_color != [1.0, 1.0, 1.0, 1.0]
        or document.buffers[0].uri is not None
        or binary_blob is None
        or len(binary_blob) != document.buffers[0].byteLength
    ):
        raise RuntimeError("completed GLB does not contain the expected geometry scene")


if __name__ == "__main__":
    main()
