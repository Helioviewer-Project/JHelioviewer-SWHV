#!/usr/bin/env python3
"""Convert the supplied COCONUT demonstration solution to the JHV glTF scene profile.

The CFmesh file does not contain an observation time or coordinate-frame declaration.  This script
requires the time, writes it as DATE-OBS, and assumes that the solution's Qorona Cartesian axes are
Carrington-aligned: +x at Carrington longitude 0, +y at longitude 90 degrees, and +z toward solar
north.  The timestamp identifies the generated asset within a model time sequence.  The script writes
a self-contained GLB containing magnetic field lines, a triangulated B_r=0 current sheet, the boundary
endpoints of open field lines, and a deliberately artificial set of thick lit tubes used to
demonstrate normals and lighting.  The tube centerlines follow selected CFmesh field lines, but their
selection, radius, color, and representation as solid tubes have no scientific meaning.

The output declares a reference direction at Carrington longitude and latitude zero.  Its SOLX,
SOLY, and SOLZ components are therefore the solution's y, z, and x components respectively.  This
is a fixed change of Cartesian axis order, not a rotation to Earth's position at the solution time.

The constants and validation checks below are tailored to that solution.  They specify one
high-quality example conversion, not requirements of the JHV interface.  Reconstruction and
tracing are followed by display-geometry reduction, color assignment, and export.  Bounded
polyline simplification and scalar-aware surface decimation reduce the exported geometry without
changing the reconstruction or tracing settings.  The finished GLB is reopened to check its
metadata, geometry counts, attribute formats, and materials, not to compare every stored value.

Requires Qorona 0.4.0, PyVista/VTK, and pygltflib.  Qorona's standard installation supplies
NumPy, SciPy, and Matplotlib, which the script also imports.  Numba is not used directly; Qorona
uses it to accelerate the moving-least-squares resampling when it is available.  Tracing here
uses Qorona's NumPy CPU path because the complete field-line paths are retained for export.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from datetime import datetime
from pathlib import Path

import numpy as np
import pyvista as pv
import qorona
from matplotlib import colormaps
from pygltflib import FLOAT, GLTF2, LINES, POINTS, TRIANGLES, UNSIGNED_BYTE, VEC3, VEC4
from qorona.field.sampled import SampledField
from qorona.io.readers.coconut.cfmesh import CFmeshReader
from qorona.render.fieldlines import polarity_colours
from qorona.resample import KnnMlsResampler, LogarithmicSpacing, SphericalGrid
from qorona.resample.grid import pad_field
from qorona.trace import lonlat_seeds, trace_field_lines
from scipy.ndimage import map_coordinates
from vtkmodules.vtkIOExport import vtkGLTFExporter

RSUN_REF = 695_700_000.0
REFERENCE_DSUN_M = 149_597_870_700.0
MODEL_OUTER_RADIUS = 6.0

# This example assumes that Qorona (x, y, z) is aligned with (Carrington longitude 0,
# longitude 90 degrees, north). For the declared reference direction (0, 0), glTF
# (SOLX, SOLY, SOLZ) is therefore simply (y, z, x). COCONUT needs only that axis
# permutation; the matrix form demonstrates how a producer can instead apply a general
# rotation when its model axes have a different orientation.
SOLUTION_TO_SOL = np.array(
    ((0.0, 1.0, 0.0), (0.0, 0.0, 1.0), (1.0, 0.0, 0.0)), dtype=np.float64
)

TIMESTAMP_PATTERN = re.compile(
    r"(?P<date>\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(?P<fraction>\d{1,9}))?"
)

FIELD_N_R = 192
FIELD_N_THETA = 180
FIELD_N_PHI = 360
SEED_N_THETA = 18
SEED_N_PHI = 36
TRACE_RTOL = 1.0e-8
TRACE_CFL = 0.125

# Display-geometry post-processing. Reconstruction and tracing retain the settings above; these
# operations reduce only the geometry stored in the demonstration product.
POLYLINE_MAX_DEVIATION_RSUN = 1.0e-5
CURRENT_SHEET_TARGET_REDUCTION = 0.5
CURRENT_SHEET_SCALAR_WEIGHT = 1.0

CURRENT_SHEET_OPACITY = 0.35
# COOLFluiD's "corona" normalization uses v0 = 4.8e7 cm/s (Guo et al. 2024).
CURRENT_SHEET_VELOCITY_SCALE_KM_S = 480.0
CURRENT_SHEET_VELOCITY_MIN_KM_S = -30.0
CURRENT_SHEET_VELOCITY_MAX_KM_S = 300.0
CURRENT_SHEET_COLORMAP = "turbo"

# Display-only test geometry. These values were chosen to make lighting conspicuous; they do not
# describe a physical flux rope or any other structure in the COCONUT solution.
FIELD_TUBE_LONGITUDE_MIN_DEGREES = 32.0
FIELD_TUBE_LONGITUDE_MAX_DEGREES = 42.0
FIELD_TUBE_SEED_COUNT = 11
FIELD_TUBE_LATITUDE_DEGREES = 6.0
FIELD_TUBE_RADIUS_M = 4_000_000.0
FIELD_TUBE_SIDES = 16
FIELD_TUBE_COLOR = (1.0, 191.0 / 255.0, 0.0)


def main() -> None:
    args = arguments()
    args.output.parent.mkdir(parents=True, exist_ok=True)

    source_sha256 = sha256(args.input)
    solution = CFmeshReader().read(args.input, show_progress=True)
    resampler = KnnMlsResampler()
    field, velocity = build_field(solution, resampler)
    processing = {
        "qoronaVersion": qorona.__version__,
        "source": args.input.name,
        "sourceSha256": source_sha256,
        "sourceCellCount": int(solution.cell_centers.shape[0]),
        "resampler": "k-nearest-neighbor degree-1 moving least squares",
        "minimumNeighbors": resampler.n_neighbors,
        "referenceCellCount": resampler.reference_cell_count,
        "ridge": resampler.ridge,
        "fieldGrid": [FIELD_N_R, FIELD_N_THETA, FIELD_N_PHI],
        "fieldGridRadialSpacing": "logarithmic",
        "solutionAxes": [
            "Carrington longitude 0",
            "Carrington longitude 90",
            "solar north",
        ],
        "gltfPositionComponents": ["solution y", "solution z", "solution x"],
    }

    write_scene(field, velocity, args.timestamp, processing, args.output)

    print(f"Wrote and validated {args.output}")


def arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "input",
        type=Path,
        help="supplied COCONUT demonstration .CFmesh or .CFmesh.xz solution",
    )
    parser.add_argument(
        "--timestamp",
        required=True,
        type=normalized_utc_timestamp,
        help="UTC solution time written as DATE-OBS, without a timezone designator or offset",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("extra/test/data/coconut-corona-scene.glb"),
        help="output GLB file (default: extra/test/data/coconut-corona-scene.glb)",
    )
    args = parser.parse_args()
    if args.output.suffix.lower() != ".glb":
        parser.error("--output must end in .glb")
    return args


def normalized_utc_timestamp(value: str) -> str:
    match = TIMESTAMP_PATTERN.fullmatch(value)
    if match is None:
        raise argparse.ArgumentTypeError(
            "observation time must have the form YYYY-MM-DDTHH:mm:ss[.fraction] "
            "without a timezone designator or offset"
        )
    try:
        time = datetime.strptime(match.group("date"), "%Y-%m-%dT%H:%M:%S")
    except ValueError as error:
        raise argparse.ArgumentTypeError(
            f"invalid observation time {value!r}"
        ) from error
    milliseconds = (match.group("fraction") or "").ljust(3, "0")[:3]
    return f"{time:%Y-%m-%dT%H:%M:%S}.{milliseconds}"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def build_field(
    solution, resampler: KnnMlsResampler
) -> tuple[SampledField, np.ndarray]:
    """Reconstruct magnetic field and velocity together on the same spherical grid."""
    grid = SphericalGrid(
        spacing=LogarithmicSpacing(inner=1.0, outer=MODEL_OUTER_RADIUS),
        n_r=FIELD_N_R,
        n_theta=FIELD_N_THETA,
        n_phi=FIELD_N_PHI,
    )
    names = ("Bx", "By", "Bz", "vx", "vy", "vz")
    missing = [name for name in names if name not in solution.variables]
    if missing:
        raise ValueError(f"COCONUT solution lacks variables: {', '.join(missing)}")
    components = resampler.resample(solution, grid, names, show_progress=True)
    magnetic_field = pad_field(
        np.stack([components[name] for name in names[:3]], axis=-1)
    )
    field = SampledField(grid, magnetic_field, solution.metadata.normalization)
    velocity = np.stack([components[name] for name in names[3:]], axis=-1)
    return field, velocity


def write_scene(
    field: SampledField,
    velocity: np.ndarray,
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
    colors = polarity_colours(field, lines, 1.0, MODEL_OUTER_RADIUS)
    current_sheet = extract_current_sheet(field, velocity)

    # Reduce only the extracted display geometry, after reconstruction and tracing are complete.
    current_sheet_input_vertices = current_sheet.n_points
    current_sheet_input_triangles = current_sheet.n_cells
    current_sheet = decimate_current_sheet(current_sheet)
    color_current_sheet(current_sheet)
    field_tubes, tube_input_vertices, tube_output_vertices = create_field_tubes(field)
    open_boundary_points = lines.feet[lines.is_open].reshape(-1, 3)
    open_boundary_points = (open_boundary_points @ SOLUTION_TO_SOL.T).astype(np.float32)
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
    input_vertex_count = 0
    for path, color, complete in zip(
        lines.paths, colors, lines.is_complete, strict=True
    ):
        if not complete:
            continue
        path = np.asarray(path, dtype=np.float64)
        input_vertex_count += len(path)
        transformed = prepare_polyline(path)
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
        "DSUN_OBS": REFERENCE_DSUN_M,
        "CRLN_OBS": 0.0,
        "CRLT_OBS": 0.0,
        "RSUN_REF": RSUN_REF,
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
            "fieldLineGeometry": {
                "postprocessing": "Ramer-Douglas-Peucker polyline simplification",
                "maximumDeviationSolarRadii": POLYLINE_MAX_DEVIATION_RSUN,
                "inputVertices": input_vertex_count,
                "outputVertices": len(position_array),
            },
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
                "postprocessing": "scalar-aware quadric decimation",
                "targetTriangleReduction": CURRENT_SHEET_TARGET_REDUCTION,
                "scalarWeight": CURRENT_SHEET_SCALAR_WEIGHT,
                "inputVertices": current_sheet_input_vertices,
                "inputTriangles": current_sheet_input_triangles,
                "outputVertices": current_sheet.n_points,
                "outputTriangles": current_sheet.n_cells,
            },
            "openFieldBoundaryPoints": {
                "definition": "inner and outer boundary endpoints of complete open field lines",
                "count": len(open_boundary_points),
                "colorQuantity": "polarity of the corresponding field line",
            },
            "fieldTubes": {
                "definition": "display-only lit tubes following selected traced field-line centerlines; selection, radius, color, and solid-tube representation are artificial",
                "seedLongitudeRangeDegrees": [
                    FIELD_TUBE_LONGITUDE_MIN_DEGREES,
                    FIELD_TUBE_LONGITUDE_MAX_DEGREES,
                ],
                "seedLatitudeDegrees": FIELD_TUBE_LATITUDE_DEGREES,
                "fieldLineCount": FIELD_TUBE_SEED_COUNT,
                "tubeRadiusM": FIELD_TUBE_RADIUS_M,
                "tubeSides": FIELD_TUBE_SIDES,
                "centerlinePostprocessing": "Ramer-Douglas-Peucker polyline simplification",
                "maximumDeviationSolarRadii": POLYLINE_MAX_DEVIATION_RSUN,
                "inputCenterlineVertices": tube_input_vertices,
                "outputCenterlineVertices": tube_output_vertices,
                "vertices": field_tubes.n_points,
                "triangles": field_tubes.n_cells,
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
        field_tubes,
        open_boundary_points,
        open_boundary_colors,
        metadata,
    )


def prepare_polyline(points: np.ndarray) -> np.ndarray:
    """Simplify in model coordinates, then convert to the exported float32 SOL frame."""
    points = simplify_polyline(points, POLYLINE_MAX_DEVIATION_RSUN)
    points = (points @ SOLUTION_TO_SOL.T).astype(np.float32)
    if len(points) < 2:
        return points
    # Assimp turns zero-length line segments into points, so remove adjacent vertices that
    # become equal when stored as float32.
    return points[np.concatenate(([True], np.any(points[1:] != points[:-1], axis=1)))]


def simplify_polyline(points: np.ndarray, max_deviation: float) -> np.ndarray:
    """Remove interior vertices while bounding their distance from the simplified line."""
    if points.ndim != 2 or points.shape[1] != 3 or not np.isfinite(points).all():
        raise ValueError("polyline points must be a finite Nx3 array")
    if max_deviation <= 0.0:
        raise ValueError("polyline maximum deviation must be positive")
    if len(points) < 3:
        return points

    keep = np.zeros(len(points), dtype=bool)
    keep[[0, -1]] = True
    pending = [(0, len(points) - 1)]
    maximum_squared = max_deviation * max_deviation

    while pending:
        first, last = pending.pop()
        interior = points[first + 1 : last]
        if len(interior) == 0:
            continue

        segment = points[last] - points[first]
        segment_squared = np.dot(segment, segment)
        relative = interior - points[first]
        if segment_squared == 0.0:
            distance_squared = np.einsum("ij,ij->i", relative, relative)
        else:
            fraction = np.clip(relative @ segment / segment_squared, 0.0, 1.0)
            offset = relative - fraction[:, None] * segment
            distance_squared = np.einsum("ij,ij->i", offset, offset)

        farthest = int(np.argmax(distance_squared))
        if distance_squared[farthest] > maximum_squared:
            split = first + farthest + 1
            keep[split] = True
            pending.append((first, split))
            pending.append((split, last))

    return points[keep]


def create_field_tubes(field: SampledField) -> tuple[pv.PolyData, int, int]:
    longitudes = np.deg2rad(
        np.linspace(
            FIELD_TUBE_LONGITUDE_MIN_DEGREES,
            FIELD_TUBE_LONGITUDE_MAX_DEGREES,
            FIELD_TUBE_SEED_COUNT,
        )
    )
    latitude = np.deg2rad(FIELD_TUBE_LATITUDE_DEGREES)
    radius = 1.0 + 1.0e-9
    seeds = np.column_stack(
        (
            radius * np.cos(latitude) * np.cos(longitudes),
            radius * np.cos(latitude) * np.sin(longitudes),
            np.full_like(longitudes, radius * np.sin(latitude)),
        )
    )
    lines = trace_field_lines(
        field,
        seeds,
        store_path=True,
        show_progress=False,
        device="cpu",
        precision="float64",
        rtol=TRACE_RTOL,
        cfl=TRACE_CFL,
    )
    if not np.all(lines.is_closed):
        raise RuntimeError("field-tube seeds did not all produce closed paths")

    positions = []
    polylines = []
    vertex_count = 0
    input_vertex_count = 0
    for path in lines.paths:
        path = np.asarray(path, dtype=np.float64)
        input_vertex_count += len(path)
        transformed = prepare_polyline(path)
        if len(transformed) < 2:
            continue
        first = vertex_count
        vertex_count += len(transformed)
        positions.append(transformed)
        polylines.append(
            np.concatenate(([len(transformed)], np.arange(first, vertex_count)))
        )

    if not positions:
        raise RuntimeError("field-tube tracing produced no complete closed paths")
    line_mesh = pv.PolyData(np.concatenate(positions), lines=np.concatenate(polylines))
    tubes = line_mesh.tube(
        radius=FIELD_TUBE_RADIUS_M / RSUN_REF,
        n_sides=FIELD_TUBE_SIDES,
        capping=True,
    ).triangulate()
    normals = np.asarray(tubes.point_data["TubeNormals"], dtype=np.float32)
    tubes.clear_data()
    tubes.point_data["NORMAL"] = normals
    if (
        tubes.n_points == 0
        or tubes.n_cells == 0
        or not tubes.is_all_triangles
        or not np.isfinite(tubes.points).all()
        or not np.isfinite(normals).all()
        or not np.allclose(np.linalg.norm(normals, axis=1), 1.0, atol=1.0e-5)
    ):
        raise RuntimeError("field-line tube generation failed")
    return tubes, input_vertex_count, vertex_count


def extract_current_sheet(field: SampledField, velocity: np.ndarray) -> pv.PolyData:
    """Mesh B_r=0 and attach radial velocity before decimation and color assignment."""
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
    surface.points = (model_points @ SOLUTION_TO_SOL.T).astype(np.float32)
    surface.point_data["radialVelocity"] = radial_velocity
    surface = surface.clean(tolerance=1.0e-6, absolute=True)
    # Joining the coincident longitude seam can collapse a handful of seam triangles. VTK's
    # cleaner preserves those degeneracies as line or point cells; retain only the polygonal
    # faces so the exported object remains a pure triangle mesh.
    polygon_surface = pv.PolyData(surface.points, surface.faces)
    polygon_surface.point_data["radialVelocity"] = surface.point_data["radialVelocity"]
    surface = polygon_surface.remove_unused_points()
    if not surface.is_all_triangles:
        raise RuntimeError("current-sheet contour is not a triangle mesh")
    return surface


def decimate_current_sheet(surface: pv.PolyData) -> pv.PolyData:
    """Reduce triangles using both geometry and radial velocity in the error criterion."""
    surface.set_active_scalars("radialVelocity", preference="point")
    decimated = surface.decimate(
        CURRENT_SHEET_TARGET_REDUCTION,
        scalars=True,
        scalars_weight=CURRENT_SHEET_SCALAR_WEIGHT,
    )
    radial_velocity = np.asarray(decimated.point_data.get("radialVelocity"))
    if (
        decimated.n_points == 0
        or decimated.n_cells == 0
        or decimated.n_cells >= surface.n_cells
        or not decimated.is_all_triangles
        or not np.isfinite(decimated.points).all()
        or radial_velocity.shape != (decimated.n_points,)
        or not np.isfinite(radial_velocity).all()
    ):
        raise RuntimeError("current-sheet decimation failed")
    return decimated


def color_current_sheet(surface: pv.PolyData) -> None:
    radial_velocity = np.asarray(surface.point_data["radialVelocity"])
    normalized_velocity = np.clip(
        (radial_velocity - CURRENT_SHEET_VELOCITY_MIN_KM_S)
        / (CURRENT_SHEET_VELOCITY_MAX_KM_S - CURRENT_SHEET_VELOCITY_MIN_KM_S),
        0.0,
        1.0,
    )
    rgba = colormaps[CURRENT_SHEET_COLORMAP](normalized_velocity, bytes=True)
    rgba[:, 3] = round(255 * CURRENT_SHEET_OPACITY)
    surface.point_data["RGBA"] = np.ascontiguousarray(rgba, dtype=np.uint8)


def write_scene_glb(
    output: Path,
    positions: np.ndarray,
    colors: np.ndarray,
    polylines: np.ndarray,
    segment_count: int,
    current_sheet: pv.PolyData,
    field_tubes: pv.PolyData,
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
    tube_normals = np.asarray(field_tubes.point_data.get("NORMAL"))
    if (
        field_tubes.n_points == 0
        or field_tubes.n_cells == 0
        or not field_tubes.is_all_triangles
        or tube_normals.shape != (field_tubes.n_points, 3)
    ):
        raise ValueError("field tubes must be a triangle mesh with vertex normals")
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
    outer_boundary = np.isclose(boundary_radii, MODEL_OUTER_RADIUS, atol=1.0e-5)
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
        # Keep the current-sheet velocity colors and line/point polarity colors
        # unshaded. Light only the artificial tubes, whose shading reveals their
        # round cross-section and three-dimensional shape.
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
            field_tubes,
            name="Selected closed field-line tubes",
            color=FIELD_TUBE_COLOR,
            lighting=True,
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
        # Use VTK directly because PyVista's export_gltf() only writes to a file.
        exporter = vtkGLTFExporter()
        exporter.SetRenderWindow(plotter.render_window)
        exporter.SetInlineData(True)
        exporter.SetSaveNormal(True)
        document = json.loads(exporter.WriteToString())
    finally:
        plotter.close()

    scene_index = document.get("scene", 0)
    scene = document["scenes"][scene_index]
    scene["name"] = "COCONUT corona"
    scene["extras"] = metadata

    # Identify the four objects by their geometry and attributes, not VTK's export order.
    meshes = document.get("meshes", [])
    line_meshes = []
    sheet_meshes = []
    tube_meshes = []
    point_meshes = []
    for mesh_index, exported_mesh in enumerate(meshes):
        primitives = exported_mesh["primitives"]
        if len(primitives) != 1:
            continue
        primitive = primitives[0]
        mode = primitive.get("mode", TRIANGLES)
        attributes = primitive.get("attributes", {})
        if mode == LINES:
            line_meshes.append(mesh_index)
        elif mode == POINTS:
            point_meshes.append(mesh_index)
        elif mode == TRIANGLES:
            if "COLOR_0" in attributes and "NORMAL" not in attributes:
                sheet_meshes.append(mesh_index)
            elif "NORMAL" in attributes and "COLOR_0" not in attributes:
                tube_meshes.append(mesh_index)
    if (
        len(meshes) != 4
        or len(line_meshes) != 1
        or len(sheet_meshes) != 1
        or len(tube_meshes) != 1
        or len(point_meshes) != 1
    ):
        raise RuntimeError(
            "VTK did not export the expected line, surface, tube, and point meshes"
        )

    line_mesh = line_meshes[0]
    surface_mesh = sheet_meshes[0]
    tube_mesh = tube_meshes[0]
    point_mesh = point_meshes[0]
    mesh_names = {
        line_mesh: "COCONUT magnetic field lines",
        surface_mesh: "Heliospheric current sheet",
        tube_mesh: "Selected closed field-line tubes",
        point_mesh: "Open-field-line boundary endpoints",
    }
    for mesh_index, name in mesh_names.items():
        meshes[mesh_index]["name"] = name
    for node in document.get("nodes", []):
        if node.get("mesh") in mesh_names:
            node["name"] = mesh_names[node["mesh"]]

    materials = document.get("materials", [])
    for mesh_index, name in mesh_names.items():
        material_index = meshes[mesh_index]["primitives"][0].get("material")
        if material_index is None or not 0 <= material_index < len(materials):
            raise RuntimeError(f"VTK mesh {name} has no valid material")

    line_material_index = meshes[line_mesh]["primitives"][0]["material"]
    surface_material_index = meshes[surface_mesh]["primitives"][0]["material"]
    tube_material_index = meshes[tube_mesh]["primitives"][0]["material"]
    point_material_index = meshes[point_mesh]["primitives"][0]["material"]
    unlit_material_indices = {
        line_material_index,
        surface_material_index,
        point_material_index,
    }
    if tube_material_index in unlit_material_indices:
        raise RuntimeError("VTK field tubes do not have an independent lit material")
    if surface_material_index in (
        line_material_index,
        tube_material_index,
        point_material_index,
    ):
        raise RuntimeError("VTK current sheet does not have an independent material")
    surface_material = materials[surface_material_index]
    surface_material["alphaMode"] = "BLEND"
    surface_material["doubleSided"] = True

    # VTK does not carry PyVista's lighting=False setting into glTF, so mark
    # these materials explicitly. The separate tube material remains lit.
    extensions_used = document.setdefault("extensionsUsed", [])
    if "KHR_materials_unlit" not in extensions_used:
        extensions_used.append("KHR_materials_unlit")
    for material_index in unlit_material_indices:
        material = materials[material_index]
        material.setdefault("extensions", {})["KHR_materials_unlit"] = {}

    # Package VTK's in-memory glTF document as one GLB.
    GLTF2.gltf_from_json(json.dumps(document)).save_binary(output)

    # Check the packaged file as well as the arrays prepared above.
    validate_scene_glb(
        output,
        len(positions),
        segment_count,
        current_sheet.n_points,
        current_sheet.n_cells,
        field_tubes.n_points,
        field_tubes.n_cells,
        len(boundary_points),
        metadata,
    )


def validate_scene_glb(
    path: Path,
    expected_vertex_count: int,
    expected_segment_count: int,
    expected_surface_vertex_count: int,
    expected_surface_triangle_count: int,
    expected_tube_vertex_count: int,
    expected_tube_triangle_count: int,
    expected_point_count: int,
    expected_metadata: dict[str, object],
) -> None:
    """Check this example's metadata and export structure, not individual binary values."""
    document = GLTF2().load(path)
    scene_index = document.scene if document.scene is not None else 0
    if document.asset is None or document.asset.version != "2.0":
        raise RuntimeError("completed GLB is not a glTF 2.0 asset")
    if (
        document.scenes is None
        or len(document.scenes) != 1
        or not 0 <= scene_index < len(document.scenes)
    ):
        raise RuntimeError("completed GLB does not contain one valid default scene")
    if document.meshes is None or len(document.meshes) != 4:
        raise RuntimeError("completed GLB does not contain four meshes")
    if document.materials is None:
        raise RuntimeError("completed GLB contains no materials")
    if document.accessors is None:
        raise RuntimeError("completed GLB contains no accessors")
    if document.buffers is None or len(document.buffers) != 1:
        raise RuntimeError("completed GLB does not contain one binary buffer")
    if (
        document.extensionsUsed is None
        or "KHR_materials_unlit" not in document.extensionsUsed
    ):
        raise RuntimeError("completed GLB does not declare KHR_materials_unlit")

    scene = document.scenes[scene_index]
    accessors = document.accessors
    materials = document.materials
    primitives = [
        primitive
        for mesh in document.meshes
        for primitive in (mesh.primitives if mesh.primitives is not None else [])
    ]
    line_primitives = [primitive for primitive in primitives if primitive.mode == LINES]
    triangle_primitives = [
        primitive for primitive in primitives if primitive.mode == TRIANGLES
    ]
    point_primitives = [
        primitive for primitive in primitives if primitive.mode == POINTS
    ]
    surface_primitives = [
        primitive
        for primitive in triangle_primitives
        if primitive.attributes is not None
        and primitive.attributes.COLOR_0 is not None
        and primitive.attributes.NORMAL is None
    ]
    tube_primitives = [
        primitive
        for primitive in triangle_primitives
        if primitive.attributes is not None
        and primitive.attributes.NORMAL is not None
        and primitive.attributes.COLOR_0 is None
    ]
    if (
        len(primitives) != 4
        or len(line_primitives) != 1
        or len(surface_primitives) != 1
        or len(tube_primitives) != 1
        or len(point_primitives) != 1
    ):
        raise RuntimeError(
            "completed GLB does not contain the expected line, surface, tube, and point primitives"
        )

    line = line_primitives[0]
    surface = surface_primitives[0]
    tubes = tube_primitives[0]
    points = point_primitives[0]
    expected_attributes = (
        ("line", line, ("POSITION", "COLOR_0")),
        ("current sheet", surface, ("POSITION", "COLOR_0")),
        ("field tubes", tubes, ("POSITION", "NORMAL")),
        ("boundary points", points, ("POSITION", "COLOR_0")),
    )
    for name, primitive, attribute_names in expected_attributes:
        if primitive.attributes is None:
            raise RuntimeError(f"completed GLB {name} primitive has no attributes")
        if primitive.indices is None or primitive.material is None:
            raise RuntimeError(
                f"completed GLB {name} primitive has no indices or material"
            )
        accessor_indices = [
            getattr(primitive.attributes, attribute_name)
            for attribute_name in attribute_names
        ]
        if any(
            index is None or not 0 <= index < len(accessors)
            for index in accessor_indices
        ):
            raise RuntimeError(f"completed GLB {name} has invalid attributes")
        if not 0 <= primitive.indices < len(accessors):
            raise RuntimeError(f"completed GLB {name} has invalid indices")
        if not 0 <= primitive.material < len(materials):
            raise RuntimeError(f"completed GLB {name} has an invalid material")
        if materials[primitive.material].pbrMetallicRoughness is None:
            raise RuntimeError(f"completed GLB {name} material has no base color")

    for name, primitive in (
        ("line", line),
        ("current sheet", surface),
        ("boundary points", points),
    ):
        extensions = materials[primitive.material].extensions
        if extensions is None or "KHR_materials_unlit" not in extensions:
            raise RuntimeError(f"completed GLB {name} material is not unlit")
    tube_extensions = materials[tubes.material].extensions
    if tube_extensions is not None and "KHR_materials_unlit" in tube_extensions:
        raise RuntimeError("completed GLB field-tube material is unlit")
    if surface.material in (line.material, tubes.material, points.material):
        raise RuntimeError("completed GLB current-sheet material is shared")

    line_position = accessors[line.attributes.POSITION]
    line_color = accessors[line.attributes.COLOR_0]
    line_indices = accessors[line.indices]
    surface_position = accessors[surface.attributes.POSITION]
    surface_color = accessors[surface.attributes.COLOR_0]
    surface_indices = accessors[surface.indices]
    tube_position = accessors[tubes.attributes.POSITION]
    tube_normal = accessors[tubes.attributes.NORMAL]
    tube_indices = accessors[tubes.indices]
    point_position = accessors[points.attributes.POSITION]
    point_color = accessors[points.attributes.COLOR_0]
    point_indices = accessors[points.indices]
    line_base_color = materials[line.material].pbrMetallicRoughness.baseColorFactor
    surface_material = materials[surface.material]
    surface_base_color = surface_material.pbrMetallicRoughness.baseColorFactor
    tube_base_color = materials[tubes.material].pbrMetallicRoughness.baseColorFactor
    point_base_color = materials[points.material].pbrMetallicRoughness.baseColorFactor
    binary_blob = document.binary_blob()
    if scene.name != "COCONUT corona" or scene.extras != expected_metadata:
        raise RuntimeError("completed GLB scene metadata does not match the input")

    if (
        line_position.componentType != FLOAT
        or line_position.type != VEC3
        or line_position.count != expected_vertex_count
        or line_color.count != expected_vertex_count
        or line_indices.count != 2 * expected_segment_count
        or line_color.componentType != UNSIGNED_BYTE
        or line_color.type != VEC4
        or line_color.normalized is not True
        or line_base_color != [1.0, 1.0, 1.0, 1.0]
    ):
        raise RuntimeError("completed GLB line data does not match the input")

    if (
        surface_position.componentType != FLOAT
        or surface_position.type != VEC3
        or surface_position.count != expected_surface_vertex_count
        or surface_color.count != expected_surface_vertex_count
        or surface_indices.count != 3 * expected_surface_triangle_count
        or surface_color.componentType != UNSIGNED_BYTE
        or surface_color.type != VEC4
        or surface_color.normalized is not True
        or surface_material.alphaMode != "BLEND"
        or surface_material.doubleSided is not True
        or surface_base_color != [1.0, 1.0, 1.0, 1.0]
    ):
        raise RuntimeError("completed GLB current-sheet data does not match the input")

    if (
        tube_position.componentType != FLOAT
        or tube_position.type != VEC3
        or tube_position.count != expected_tube_vertex_count
        or tube_normal.count != expected_tube_vertex_count
        or tube_indices.count != 3 * expected_tube_triangle_count
        or tube_normal.componentType != FLOAT
        or tube_normal.type != VEC3
        or tube_base_color != [*FIELD_TUBE_COLOR, 1.0]
    ):
        raise RuntimeError("completed GLB field-tube data does not match the input")

    if (
        point_position.componentType != FLOAT
        or point_position.type != VEC3
        or point_position.count != expected_point_count
        or point_color.count != expected_point_count
        or point_indices.count != expected_point_count
        or point_color.componentType != UNSIGNED_BYTE
        or point_color.type != VEC4
        or point_color.normalized is not True
        or point_base_color != [1.0, 1.0, 1.0, 1.0]
    ):
        raise RuntimeError("completed GLB point data does not match the input")

    if (
        document.buffers[0].uri is not None
        or binary_blob is None
        or len(binary_blob) != document.buffers[0].byteLength
    ):
        raise RuntimeError("completed GLB does not contain one embedded binary buffer")


if __name__ == "__main__":
    main()
