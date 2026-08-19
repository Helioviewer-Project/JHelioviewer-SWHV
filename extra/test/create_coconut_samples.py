#!/usr/bin/env python3
"""Create reference-quality JHV volume and field-line products from COCONUT CFmesh.

The input CFmesh does not identify its observation time or spatial frame.  This converter assumes
that its Cartesian axes are Carrington-aligned and requires the solution time on the command line.
It resamples the solution into the observer-aligned Heliocentric (SOLX/SOLY/SOLZ) frame used by
JHelioviewer.

The volume contains a display-ready, clipped logarithm of electron density.  Qorona converts the
model-normalized mass density to a *relative* electron-density shape by dividing by its mean
molecular weight per electron (1.27 by default); it deliberately supplies no absolute density
normalization.  ASSUMED_ELECTRON_DENSITY_SCALE_M3 below is therefore a producer assumption, not a
calibration recovered from the CFmesh file.  Replace it only when an appropriate model-specific
calibration is known, and preserve the assumption in the product metadata.

The fixed settings below select one high-quality reference conversion.  This script intentionally
does not offer a separate fast mode: changing resolution, resampling, tracing, clipping, or density
calibration changes the product and should be an explicit, reviewed source change.  Both completed
products are reopened and validated before the script reports that they were written.

Requires Qorona 0.4.0, PyVista, and pygltflib.  Qorona's standard installation supplies
NumPy, SciPy, Astropy, SunPy, and the optional Numba acceleration used during resampling;
PyVista installs VTK.
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
from pygltflib import GLTF2
from vtkmodules.vtkIOExport import vtkGLTFExporter

from qorona.field.density import MEAN_MOLECULAR_WEIGHT
from qorona.field.sampled import SampledField
from qorona.io.readers.coconut.cfmesh import CFmeshReader
from qorona.pipeline import sub_earth_point
from qorona.render.fieldlines import polarity_colours
from qorona.resample import KnnMlsResampler, LogarithmicSpacing, SphericalGrid
from qorona.trace import lonlat_seeds, trace_field_lines

RSUN_REF = 695_700_000.0
VOLUME_EXTENT = 6.0
VOLUME_SIZE = 256
LOG_DENSITY_MIN = 10.9
LOG_DENSITY_MAX = 14.0
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


def main() -> None:
    args = arguments()
    args.output_directory.mkdir(parents=True, exist_ok=True)

    timestamp = normalized_utc_timestamp(args.timestamp)
    source_sha256 = sha256(args.input)
    observer = observer_metadata(timestamp)
    world_to_sol = observer_basis(observer["CRLN_OBS"], observer["CRLT_OBS"])
    solution = CFmeshReader().read(args.input, show_progress=True)
    resampler = KnnMlsResampler()
    field = build_field(solution, resampler)
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
        "interpolator": "Keys tricubic",
        "meanMolecularWeightPerElectron": MEAN_MOLECULAR_WEIGHT,
    }

    volume_path = args.output_directory / "coconut-corona-density-16.fits"
    write_volume(field, world_to_sol, observer, timestamp, processing, volume_path)

    scene_path = args.output_directory / "coconut-corona-field-lines.glb"
    write_scene(field, world_to_sol, observer, timestamp, processing, scene_path)

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
    return parser.parse_args()


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
    longitude, latitude, distance_au = sub_earth_point(timestamp)
    return {
        "DSUN_OBS": float((distance_au * u.au).to_value(u.m)),
        "CRLN_OBS": longitude,
        "CRLT_OBS": latitude,
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


def build_field(solution, resampler: KnnMlsResampler) -> SampledField:
    grid = SphericalGrid(
        spacing=LogarithmicSpacing(inner=1.0, outer=VOLUME_EXTENT),
        n_r=FIELD_N_R,
        n_theta=FIELD_N_THETA,
        n_phi=FIELD_N_PHI,
    )
    return SampledField.from_solution(
        solution,
        grid,
        resampler=resampler,
        show_progress=True,
    )


def write_volume(
    field: SampledField,
    world_to_sol: np.ndarray,
    observer: dict[str, float],
    timestamp: str,
    processing: dict[str, object],
    output: Path,
) -> None:
    if field.density is None:
        raise RuntimeError("COCONUT solution has no density field")
    step = 2 * VOLUME_EXTENT / VOLUME_SIZE
    coordinates = -VOLUME_EXTENT + step * (np.arange(VOLUME_SIZE) + 0.5)
    bscale = (LOG_DENSITY_MAX - LOG_DENSITY_MIN) / 65534
    bzero = (LOG_DENSITY_MIN + LOG_DENSITY_MAX) / 2
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
            (np.clip(values[valid], LOG_DENSITY_MIN, LOG_DENSITY_MAX) - bzero) / bscale
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
        f"{processing['interpolator']} interpolation to Cartesian voxel centres"
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
        f"[{LOG_DENSITY_MIN}, {LOG_DENSITY_MAX}]"
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
            if header.get(keyword) != expected_header.get(keyword):
                raise RuntimeError(f"unexpected FITS {keyword} value")
        if list(header["HISTORY"]) != list(expected_header["HISTORY"]):
            raise RuntimeError("unexpected FITS HISTORY")


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
        },
    }
    segment_count = sum(len(position) - 1 for position in positions)
    write_line_glb(
        output,
        position_array,
        color_array,
        polyline_array,
        segment_count,
        metadata,
    )


def write_line_glb(
    output: Path,
    positions: np.ndarray,
    colors: np.ndarray,
    polylines: np.ndarray,
    segment_count: int,
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

    mesh = pv.PolyData(
        np.ascontiguousarray(positions, dtype=np.float32), lines=polylines
    )
    mesh.point_data["RGBA"] = np.ascontiguousarray(colors, dtype=np.uint8)
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

    # VTK owns the glTF geometry and buffers.  pygltflib only packages that in-memory document as
    # one binary GLB while preserving the application-specific scene metadata added above.
    GLTF2.gltf_from_json(json.dumps(document)).save_binary(output)

    # Validate the finished file, not the in-memory document: this catches packaging errors and
    # proves that the metadata and rendering attributes survived the GLB round trip.
    validate_line_glb(output, len(positions), segment_count, metadata)


def validate_line_glb(
    path: Path,
    expected_vertex_count: int,
    expected_segment_count: int,
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
        or len(document.meshes) != 1
        or document.meshes[0].primitives is None
        or len(document.meshes[0].primitives) != 1
        or document.materials is None
        or len(document.materials) != 1
        or document.accessors is None
        or document.buffers is None
        or len(document.buffers) != 1
    ):
        raise RuntimeError("completed GLB has an unexpected scene structure")

    scene = document.scenes[scene_index]
    primitive = document.meshes[0].primitives[0]
    if (
        primitive.attributes is None
        or primitive.attributes.POSITION is None
        or primitive.attributes.COLOR_0 is None
        or primitive.indices is None
        or primitive.material is None
        or not 0 <= primitive.attributes.POSITION < len(document.accessors)
        or not 0 <= primitive.attributes.COLOR_0 < len(document.accessors)
        or not 0 <= primitive.indices < len(document.accessors)
        or not 0 <= primitive.material < len(document.materials)
        or document.materials[primitive.material].pbrMetallicRoughness is None
    ):
        raise RuntimeError("completed GLB contains invalid primitive references")

    position_accessor = document.accessors[primitive.attributes.POSITION]
    color_accessor = document.accessors[primitive.attributes.COLOR_0]
    index_accessor = document.accessors[primitive.indices]
    base_color = document.materials[
        primitive.material
    ].pbrMetallicRoughness.baseColorFactor
    binary_blob = document.binary_blob()
    if (
        scene.name != "COCONUT corona"
        or scene.extras != expected_metadata
        or primitive.mode != 1
        or position_accessor.count != expected_vertex_count
        or color_accessor.count != expected_vertex_count
        or index_accessor.count != 2 * expected_segment_count
        or color_accessor.componentType != 5121
        or color_accessor.normalized is not True
        or base_color != [1.0, 1.0, 1.0, 1.0]
        or document.buffers[0].uri is not None
        or binary_blob is None
        or len(binary_blob) != document.buffers[0].byteLength
    ):
        raise RuntimeError("completed GLB does not contain the expected line scene")


if __name__ == "__main__":
    main()
