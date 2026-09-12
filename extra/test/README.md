# JHV tests

Run the routine offline regression suites from the repository root:

```sh
python3 extra/test/run_tests.py
```

Requires Python 3, JDK 25, and Ant on `PATH`. The runner compiles JHV first,
then runs maintenance, model, JPIP, timeline, and event tests. Java tests use
temporary class and home directories, leaving normal JHV settings and caches
alone. JPIP and HAPI tests need permission to open loopback sockets. Model and
event tests use bundled native libraries, but need no graphics context.

Select suites when working on a particular area:

```sh
python3 extra/test/run_tests.py j2k timelines
python3 extra/test/run_tests.py --no-build model
```

`--no-build` skips the initial application build. The separate WCS metadata
validator still runs its own compile step. Each requested suite prints PASS or FAIL. The runner continues with other suites after a failure and exits
nonzero if any failed. Missing dependencies fail the selected suite rather than
silently skipping it. Some failure-path tests deliberately log exceptions.

## Suites

| Suite / directory | What it checks | Default run |
| --- | --- | --- |
| `maintenance` (`extra/licenses/`, `extra/ffmpeg/`) | License synchronization and FFmpeg update scripts, using mocked downloads and temporary files | Yes |
| `model` | Model URI recognition, Assimp metadata, generated glTF loading, geometry, materials, and layer metadata | Yes |
| `j2k` | Protocol parsing, serialization, cache initialization failures, HTTP response boundaries, socket cleanup and queued responses | Yes |
| `timelines` | HAPI server configuration and catalog loading, partial failures and cancellation, timeline data, request coverage, cache, export, and Java2D drawing | Yes |
| `event` | HEK fixture parsing, event database persistence, filtering, cache/download coordination, and SWEK configuration | Yes |
| `shaders` (`opengl/`) | GLSL syntax and program linking using `glslangValidator` | No |
| `opengl` | Production ANGLE grids, image shaders, colored vertices and models, framebuffer readback, and export geometry/state | No |
| `uri` | FITS Rice decoding, provider selection, buffer boundaries, and compressed/uncompressed loader comparisons using nom-tam fixtures | No |
| `wcs` | FITS coordinate comparisons against Astropy, Java metadata, validator failure checks, and GLSL syntax | No |

`data/` holds shared FITS and model fixtures. `event/hek-events.json.gz` is the
captured HEK fixture for offline tests. Test classes retain their application
packages so they can exercise package-private code without widening its API.

## Graphics and WCS

```sh
python3 extra/test/run_tests.py shaders opengl
python3 extra/test/run_tests.py wcs
```

`shaders` needs `glslangValidator` on `PATH`. `opengl` needs a supported
JHV native platform and a working ANGLE graphics backend. It renders offscreen
but still exercises the graphics driver. A pass on one backend does not establish
behavior on every platform or verify the interactive GUI.

`wcs` needs NumPy, Astropy, Pillow, and `glslangValidator`. Use a Python environment
containing these dependencies. Its detailed runner
supports named subsets and optional Electron/WebGL checks:

```sh
python3 extra/test/wcs/run_jhv_wcs_hpc_validation_suite.py --list
python3 extra/test/wcs/run_jhv_wcs_hpc_validation_suite.py --only java_metadata
python3 extra/test/wcs/run_jhv_wcs_hpc_validation_suite.py --include-electron
```

Set `JHV_ELECTRON` to the Electron executable for GPU validation. See the
[WCS validation guide](../../docs/wcs-validation/jhv_wcs_hpc_validation_note.md)
for modes, reference assumptions, and diagnostic output. Diagnostic runs are
reported separately from correctness assertions.

ANGLE updates and old/new image comparisons are documented in
[the ANGLE updater guide](../angle/README.md).

## Live services and benchmarks

These remain explicit and are never part of the default run:

```sh
python3 extra/test/j2k/run_j2k_tests.py --live
python3 extra/test/event/check_hek_server.py
python3 extra/test/timelines/run_timelines_tests.py --benchmark
python3 extra/test/timelines/run_timelines_tests.py --hapi-benchmark /path/to/capture
```

The [JPIP guide](j2k/README.md) describes the ROB retrieval, movie, disk-cache,
and Callisto decoding checks. The HEK probe checks live records and pagination.
Live failures can reflect service availability or changed server data.

To capture timeline data for repeatable offline benchmarks:

```sh
python3 extra/test/timelines/run_timelines_tests.py \
  --capture-stix /path/to/new-capture \
  --start 2026-09-01T00:00:00Z --end 2026-09-08T00:00:00Z
```

Capture into a new directory outside the repository. Replay writes plot images
there. Benchmarks report elapsed time and allocations, not CPU time or
whole-application memory usage.

## Maintaining tests

Add checks to the relevant suite and ensure its runner actually executes them.
Prefer behavioral assertions with local fixtures over network-dependent tests.
Keep live probes and benchmarks opt-in. Do not retain tests of removed APIs or
historical implementations as compatibility scaffolding.

The COCONUT conversion example lives in
[`extra/examples/create_coconut_scene.py`](../examples/create_coconut_scene.py).
It generates an example asset and is not a regression test.

Maintenance tests live with their tools: [licenses](../licenses/README.md) and
[FFmpeg](../ffmpeg/README.md). The `maintenance` suite runs both.

## FITS loading and compression

The [URI/FITS guide](uri/README.md) covers the Rice verifier, FITS loading
benchmark, and testing candidate nom-tam libraries.

```sh
python3 extra/test/run_tests.py uri
extra/test/uri/run-fast-rice-verifier.sh /path/to/nom-tam-fits
extra/test/uri/run-benchmark.sh --mode Buffer /path/to/fits/files
```

The `uri` suite requires nom-tam test fixtures at `~/git/nom-tam-fits`. Use the
standalone verifier to supply another location. Loading benchmarks require
explicit input files and remain outside the regression run.
