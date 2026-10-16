# JHV tests

Run the routine offline regression suites from the repository root:

```sh
python3 extra/test/run_tests.py
```

Requires Python 3, JDK 25, and Ant on `PATH`. Native resources are read from
`lib/jhv/<platform>/`. FFmpeg binaries use Git LFS; run `git lfs pull` if they
were not downloaded during checkout. The runner compiles JHV first,
then runs maintenance, model, JPIP, timeline, and event tests. Java tests use
temporary class and home directories, leaving normal JHV settings and caches
alone. JPIP and HAPI tests need permission to open loopback sockets. Model,
image-loading and event tests use bundled native libraries, but need no graphics
context.

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
| `j2k` | Image ownership/cache, worker requests, frame selection/loading, layer load completion, image request settings, disk cache failures, HTTP boundaries and JPIP socket cleanup | Yes |
| `timelines` | HAPI server configuration and catalog loading, partial failures and cancellation, timeline data, request coverage, cache, export, and Java2D drawing | Yes |
| `event` | HEK fixture parsing, event database persistence, filtering, cache/download coordination, and SWEK configuration | Yes |
| `shaders` (`opengl/`) | GLSL syntax and program linking using `glslangValidator` | No |
| `opengl` | Production ANGLE grids, image shaders, colored vertices and models, framebuffer readback, export geometry/state, and context destruction/recreation after injected loss | No |
| `uri` | FITS Rice decoding, provider selection, buffer boundaries, and compressed/uncompressed loader comparisons using nom-tam fixtures | No |
| `wcs` | FITS coordinate comparisons against Astropy, Java metadata, validator failure checks, and GLSL syntax | No |

`data/` holds shared FITS and model fixtures. `event/hek-events.json.gz` is the
captured HEK fixture for offline tests. Test classes retain their application
packages so they can exercise package-private code without widening its API.

## Production areas to tests

Use this mapping with the [execution-path architecture map](../../docs/architecture.md).
Production paths below are relative to `src/org/helioviewer/jhv/` unless stated
otherwise. Run a suite with `python3 extra/test/run_tests.py <suite>`, combining
suites when a change crosses areas. This is a guide to existing coverage, not a
claim that a suite validates every behavior in those files.

| Changed production area | Existing checks | Additional verification or limit |
| --- | --- | --- |
| `image/ImageBuffer*`, `image/DecodedImage`, `layers/FrameDecoder`, `layers/Frames`, `thread/LatestWorker` | `j2k`: [ImageBufferCacheTest](j2k/ImageBufferCacheTest.java) covers reference ownership, eviction and keyed worker requests; [FramesTest](j2k/FramesTest.java) covers selection, stale delivery, arrivals and sampling | Native JPEG 2000 decode coverage needs the native checks below. Rendering/processing changes also need `opengl`. |
| `io/Load`, `layers/ImageLayerLoader`, `layers/ImageLayer`, `io/ImageRequestSettings` | `j2k`: [FramesLoadTest](uri/FramesLoadTest.java), [LayerLoadTest](uri/LayerLoadTest.java), [ImageRequestSettingsTest](j2k/ImageRequestSettingsTest.java) | These loader tests live under `uri/` but run in `j2k`. They use local FITS/ZIP/directory inputs; remote service behavior is separate. |
| `source/jpip/` and `source/J2KReader` | `j2k`: [JPIPCacheManagerTest](j2k/JPIPCacheManagerTest.java), [HTTPStreamTest](j2k/HTTPStreamTest.java), [JPIPSocketTest](j2k/JPIPSocketTest.java), [J2KReaderTest](j2k/J2KReaderTest.java) | Offline `J2KReaderTest` checks missing-native-library behavior. Real retrieval/decode requires the opt-in live runner. |
| `source/J2KSource`, Java/native decode interface, repository-root `native/jpeg2000/` | [check_native_build.py](j2k/check_native_build.py) runs [J2KNativeTest](j2k/J2KNativeTest.java) against supplied native libraries and esajpip fixtures | Follow the [native build/validation guide](../jpeg2000/README.md) on each affected platform. `python3 extra/test/j2k/run_j2k_tests.py --live` adds `FrameDecoderTest` and `CallistoTest`; offline runs compile those classes but do not execute them. |
| `source/FITSData`, `source/FITSSource`, `source/FastRice*` | `uri`: [FastRiceVerifier](uri/FastRiceVerifier.java) checks provider selection, decoder boundaries and compressed/uncompressed loading; `j2k` exercises local FITS frame loading | `uri` needs external nom-tam fixtures. Add `wcs` for metadata interpretation and `opengl` for displayed pixel/geometry changes. |
| `metadata/WcsInterpreter`, `metadata/FitsMetaData`, `wcs/`, image projection/sampling shaders | `wcs`: [validation runner](wcs/run_jhv_wcs_hpc_validation_suite.py), including Java metadata comparisons; `opengl`: [ImageRenderingTest](opengl/ImageRenderingTest.java), [GridRenderingTest](opengl/GridRenderingTest.java); `shaders`: GLSL syntax/linking | Select checks matching the changed transform. Electron checks are opt-in. Syntax, numerical reference checks and production ANGLE rendering establish different things. |
| `opengl/`, `layers/grid/`, `display/` | `shaders` and `opengl`: image/grid/model/colored-vertex rendering, [GLGrabRenderingTest](opengl/GLGrabRenderingTest.java), [ContextRecoveryTest](opengl/ContextRecoveryTest.java) | Context recovery injects loss into a pbuffer renderer and explicitly recreates it. It does not exercise `AngleCanvas`'s automatic recovery or native window lifecycle. Verify affected platforms and GUI resize/show/hide behavior. |
| `opengl/model/`, `layers/ModelLayer`, model detection in `io/DataUri` | `model`: [ModelDataUriTest](model/ModelDataUriTest.java), [AssimpMetaDataTest](model/AssimpMetaDataTest.java), [AssimpModelLoaderTest](model/AssimpModelLoaderTest.java); `opengl`: [ModelRenderingTest](opengl/ModelRenderingTest.java) | Loader/metadata assertions do not replace rendered-output checks. |
| `timelines/band/`, `timelines/TimelineLayers`, `time/RequestCache`, `timelines/draw/`, `timelines/chart/` | `timelines`: [HapiCatalogTest](timelines/HapiCatalogTest.java) and [TimelineDataTest](timelines/TimelineDataTest.java), including request reuse, coverage, band restoration, data export/import and chart painting | Verify mouse interaction, chart show/hide/re-attach behavior, display scales and recorded chart output for relevant UI changes. Use the explicit benchmarks below for performance claims. |
| `event/`, `plugins/swek/`, repository-root `resources/settings/SWEK.json` | `event`: [runner](event/run_event_tests.py) executes HEK parsing, database, cache, filter-request, integration and SWEK layer tests | Live HEK availability is a separate probe. Interactive event selection and overlay appearance need GUI verification when affected. |
| `movie/`, `app/Commands`, `app/state/`, `io/samp/`, Swing controls | `opengl` checks capture/readback and export view-state restoration; `timelines` checks band serialization/restoration and chart painting | These are partial checks. The listed suites do not provide an end-to-end SAMP, whole-session restoration or encoded-movie regression. Exercise the changed command/state/recording flow, including completion/failure and output. |
| Repository-root `extra/licenses/`, `extra/ffmpeg/` | `maintenance`: license and FFmpeg updater unit tests | Downloads are mocked. These checks do not run an installed FFmpeg encoder. ANGLE updates have their own [validation workflow](../angle/README.md). |

The execution lists in [run_tests.py](run_tests.py) and its delegated runners are
authoritative for what a command runs. Update this table in the same patch when
production paths move, suites change, or coverage is added/removed. Inspect test
assertions before treating a mapped suite as evidence for a particular change.

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
