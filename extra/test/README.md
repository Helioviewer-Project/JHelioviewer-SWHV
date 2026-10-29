# JHV tests

Run the routine offline regression suites from the repository root:

```sh
python3 extra/test/run_tests.py
```

Requires Python 3, JDK 25, and Ant on `PATH`. Native resources are read from
`lib/jhv/<platform>/`. FFmpeg binaries use Git LFS; run `git lfs pull` if they
were not downloaded during checkout. The runner compiles JHV first,
then runs maintenance, command/state, model, astronomy, JPIP, timeline, and event tests. Java tests use
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
| `commands` | State/SAMP parameter parsing, partial updates, warnings, playback validation and recording start/refusal behavior | Yes |
| `sidebar` | Filtered layer actions and editor retention, plugin registration/removal, Viewpoint mode transitions and time synchronization, Movie construction without a MainFrame sidebar, saved recording formats, stable timestamp and slider geometry, image/FITS disclosure sizing, and Grid label state round trips | No |
| `model` | Model URI recognition, Assimp metadata, generated glTF loading, geometry, materials, and layer metadata | Yes |
| `astronomy` | Position/state interpolation and coordinate conventions, UTC to ET against `deltet`, bundled star catalogue, apparent star directions against SOFA, barycentric state samples, and Location planet/star placement and solar-disk occultation against independent geometry, using the bundled SPICE kernels | Yes |
| `j2k` | Image ownership/cache, worker requests, frame selection/loading, layer load completion, image request settings, disk cache failures, HTTP boundaries and JPIP socket cleanup | Yes |
| `timelines` | HAPI server configuration and catalog loading, partial failures and cancellation, timeline data, request coverage, cache, export, and Java2D drawing | Yes |
| `event` | HEK fixture parsing, event database persistence, filtering, cache/download coordination, and SWEK configuration | Yes |
| `shaders` (`opengl/`) | GLSL syntax and program linking using `glslangValidator`, including both hardware and SwiftShader image-shader variants | No |
| `opengl` | Production ANGLE grids, image shaders, colored vertices and models, framebuffer readback, export geometry/state, and context destruction/recreation after injected loss | No |
| `uri` | FITS Rice decoding, provider selection, buffer boundaries, and compressed/uncompressed loader comparisons using nom-tam fixtures | No |
| `wcs` | FITS coordinate comparisons against Astropy, Java metadata, validator failure checks, and GLSL syntax | No |

`data/` holds shared FITS and model fixtures. `event/hek-events.json.gz` is the
captured HEK fixture for offline tests. Test classes retain their application
packages so they can exercise package-private code without widening its API.

`python3 extra/test/run_tests.py sidebar` requires a graphical desktop. It
constructs the production Swing sidebar and dispatches table and mode-selection
events without starting the main application or an OpenGL canvas. It uses the
Java module exports declared by the application JAR for the JIDE controls.
On Windows, the suite restores a native canvas displaced from nested Swing
panels without changing its logical bounds.
Live DPI changes still require an application-window check.
The plain sidebar host in `MainFrame` needs an application-window check for
top alignment, whole-sidebar scrolling, and the frame indicator and program
clock sharing the row below the time slider.

## Production areas to tests

Use this mapping to select checks for changed production code.
Production paths below are relative to `src/org/helioviewer/jhv/` unless stated
otherwise. Run a suite with `python3 extra/test/run_tests.py <suite>`, combining
suites when a change crosses areas. This is a guide to existing coverage, not a
claim that a suite validates every behavior in those files.

| Changed production area | Existing checks | Additional verification or limit |
| --- | --- | --- |
| `image/ImageBuffer*`, `image/DecodedImage`, `layers/FrameDecoder`, `layers/Frames`, `thread/LatestWorker` | `j2k`: [ImageBufferCacheTest](j2k/ImageBufferCacheTest.java) covers reference ownership, eviction and keyed worker requests; [FramesTest](j2k/FramesTest.java) covers selection, stale delivery, arrivals and sampling | Native JPEG 2000 decode coverage needs the native checks below. Rendering/processing changes also need `opengl`. |
| `io/Load`, `layers/ImageLayerLoader`, `layers/ImageLayer`, `io/ImageRequestSettings` | `j2k`: [FramesLoadTest](uri/FramesLoadTest.java), [LayerLoadTest](uri/LayerLoadTest.java), [ImageRequestSettingsTest](j2k/ImageRequestSettingsTest.java) | These loader tests live under `uri/` but run in `j2k`. `opengl`: [ImageRenderingTest.layerPose](opengl/ImageRenderingTest.java) covers actual layer pose selection, rendering and picking with independent geometry in all modes and mixed source types. Loader inputs are local; remote service behavior is separate. |
| `source/jpip/`, `source/J2KReader`, `io/NetClientRemote`, `io/ProxySettings` | `j2k`: [JPIPCacheManagerTest](j2k/JPIPCacheManagerTest.java), [HTTPStreamTest](j2k/HTTPStreamTest.java), [JPIPSocketTest](j2k/JPIPSocketTest.java), [J2KReaderTest](j2k/J2KReaderTest.java) | Loopback checks cover encoded and Unicode request paths, direct connections, HTTP/SOCKS proxies, exclusions, failover/failure reports, socket options and cleanup after read timeout or 407 refusal, stored/system credentials, origin-authentication refusal and explicit CONNECT restrictions. Offline coverage uses HTTP and plain JPIP; it does not exercise JPIPS/TLS. Offline `J2KReaderTest` checks missing-native-library behavior. Real retrieval/decode requires the opt-in live runner. |
| `source/J2KSource`, Java/native decode interface, repository-root `native/jpeg2000/`, `app/AppInit` decoder loading | [check_native_build.py](j2k/check_native_build.py) runs [J2KNativeTest](j2k/J2KNativeTest.java) against supplied native libraries and esajpip fixtures, selecting `--decoder kdu` or `--decoder openjpeg` | Run against both decoder backends when changing shared header inspection, channel selection or geometry. Geometry coverage includes local tile COD/COC overrides, multiple tiles and tile-parts, and decoding every advertised level. These checks load the selected bridge directly; startup loading through `AppInit` needs a separate check with each `jhv.j2k.decoder` value. Follow the [native build/validation guide](../jpeg2000/README.md) on each affected platform. `python3 extra/test/j2k/run_j2k_tests.py --live` adds `FrameDecoderTest` and `CallistoTest`; offline runs compile those classes but do not execute them. |
| `source/FITSData`, `source/FITSSource`, `source/FastRice*` | `uri`: [FastRiceVerifier](uri/FastRiceVerifier.java) checks provider selection, decoder boundaries and compressed/uncompressed loading; `j2k` exercises local FITS frame loading | `uri` needs external nom-tam fixtures. Add `wcs` for metadata interpretation and `opengl` for displayed pixel/geometry changes. |
| `metadata/MetaData`, `metadata/WcsInterpreter`, `metadata/FitsMetaData`, `wcs/` (`WcsSolution` owns WCSLIB, `ObserverGeometry` handles observer rays), `native/wcs/`, `resources/glsl/image*.frag` (shared declarations/color, geometry and mode shaders) | `wcs`: [validation runner](wcs/run_jhv_wcs_hpc_validation_suite.py), including [WcsLibraryTest](wcs/WcsLibraryTest.java) and Java metadata comparisons; `opengl`: [ImageRenderingTest](opengl/ImageRenderingTest.java), [GridRenderingTest](opengl/GridRenderingTest.java); `shaders`: GLSL syntax/linking | Independent analytic geometry, shared plain-image WCS ownership and tracing-viewpoint selection, FITS-to-buffer conversion, legacy CEA row scales with missing or Sine Latitude units, synoptic latitude coverage and PC/CD precedence, effective PV families, inverse domains, coefficient rejection, concurrent ownership, adjusted native frames and packaged-JAR loading. The WCS adapter exposes named projection axes, coefficients and pole directions; the WebGL runner verifies named std140 member offsets and both 192-byte image records, including projection matrices and per-image PV coefficients. Electron full-fragment checks upload image records packed by the production Java binder, including binder-composed projection scales, direct Orthographic TAN sampling, observer/surface rays with time correction and enhancement on/off, mixed direct-ray/sphere sampling in difference mode, and aligned Ortho/HPC comparisons with formal-TAN controls and precise source sampling across the nominal limb. Java checks also cover directed ray scaling/reversal, both warped-view picking paths, and mixed Orthographic surface/observer comparisons with distinct rotations, coefficients and decoded grids in both image orders. Native cross-build instructions are in [WCSLIB](../wcs/README.md). |
| `opengl/`, `layers/grid/`, `display/`, `layers/ImageLayers` fitting | `shaders` and `opengl`: image/grid/model/colored-vertex rendering, [GLGrabRenderingTest](opengl/GLGrabRenderingTest.java), [ContextRecoveryTest](opengl/ContextRecoveryTest.java) | `ImageRenderingTest.layerScales()` checks actual HPC/warp scales, surface/observer one-to-one zoom and the rendered apparent limb against independent tangent geometry. `layerFit()` checks public Orthographic zoom-fit/reset, mixed-layer heights and fitted surface pixels against sphere geometry. Context recovery injects loss into a pbuffer renderer and explicitly recreates it. The Windows `sidebar` test checks native canvas position restoration through [WindowsCanvasTest](sidebar/WindowsCanvasTest.java). Offscreen tests do not exercise `AngleCanvas`'s automatic recovery or native window lifecycle. Verify affected platforms and GUI resize/show/hide and live display-scaling changes. |
| `annotation/`, `display/ProjectedView`, `layers/ConnectionLayer` | `opengl`: [AnnotationRenderingTest](opengl/AnnotationRenderingTest.java) | Actual cross/loop and connectivity rendering checks finite-distance occlusion, tangent points, hidden strip breaks, seams and whole-surface Latitudinal behavior against independent geometry. |
| `opengl/text/SdfTextRenderer`, `opengl/GLText` | `opengl`: [SdfTextRenderingTest](opengl/SdfTextRenderingTest.java), [GridRenderingTest](opengl/GridRenderingTest.java) | All atlas glyph pairs: widths and both draw paths against JSON advances/kerning; spaces, missing-glyph fallback and supplementary code points; framebuffer equality with independently positioned glyphs. Grid checks exercise production labels across all modes and display scales. |
| `opengl/model/`, `layers/ModelLayer`, model detection in `io/DataUri` | `model`: [ModelDataUriTest](model/ModelDataUriTest.java), [AssimpMetaDataTest](model/AssimpMetaDataTest.java), [AssimpModelLoaderTest](model/AssimpModelLoaderTest.java); `opengl`: [ModelRenderingTest](opengl/ModelRenderingTest.java) | Loader/metadata assertions do not replace rendered-output checks. |
| `timelines/band/`, `timelines/TimelineLayers`, `time/RequestCache`, `timelines/draw/`, `timelines/chart/` | `timelines`: [HapiCatalogTest](timelines/HapiCatalogTest.java) and [TimelineDataTest](timelines/TimelineDataTest.java), including request reuse, coverage, band restoration, data export/import, chart painting, and independent line-equation and crop-invariance checks at integer/fractional scales and translations, including multicolor transitions and extreme-zoom coordinate limits | Verify mouse interaction, chart show/hide/re-attach behavior, display scales and recorded chart output for relevant UI changes. The opt-in [rendering benchmark](timelines/run_rendering_benchmark.py) checks the rendered cache, device resolution, stroke width and exact placement, multicolor runs and gaps, and resize, and measures pan/zoom and movie export. Use the explicit benchmarks below for performance claims. |
| `event/`, `plugins/swek/`, repository-root `resources/settings/SWEK.json` | `event`: [runner](event/run_event_tests.py) executes HEK parsing, database, cache, filter-request, integration and SWEK layer tests | Live HEK availability is a separate probe. Interactive event selection and overlay appearance need GUI verification when affected. |
| `astronomy/Stars`, `astronomy/Spice`, `astronomy/PositionResponse`, `astronomy/PositionLoad`, `layers/ViewpointLayer` Location mode, repository-root `resources/data/stars.bin` | `astronomy`: [PositionResponseTest](astronomy/PositionResponseTest.java) checks position/state interpolation, playback mapping, spherical conventions and JSON units; [SpiceTest](astronomy/SpiceTest.java) checks `Spice.milli2et` against `deltet` hourly 1960-2060 and around every leap second; [StarsTest](astronomy/StarsTest.java) checks the catalogue, `Stars.apparent` against `jauApcs` + `jauAtciq` and local state ranges against `spkezr`; [LocationProjectionTest](astronomy/LocationProjectionTest.java) checks `projectLocation` against eqs. (3), (6) of Skokić on Carrington LT+S directions, occultation, and the solar pole constant against the `SOLO_IAU_SUN_2009` frame | Local fixtures and SPICE only; live position-server requests are not exercised. Drawing, hover and the Planets/Stars checkboxes need GUI verification. Regenerate the catalogue with `extra/stars/update_stars.py`. |
| `movie/`, `app/Commands`, `app/state/`, `io/samp/`, Swing controls | `commands`: [CommandStateTest](commands/CommandStateTest.java) checks state/SAMP parsing, warnings, partial updates, configured export formats and fallback, and recording start/refusal, state-restore completion and recording failure payloads; `opengl` checks capture/readback and export view-state restoration; `timelines` checks band serialization/restoration and chart painting | These are partial checks. The listed suites do not provide an end-to-end SAMP, whole-session restoration or encoded-movie regression. Exercise the changed command/state/recording flow, including completion/failure and output. |
| `layers/selector/`, `layers/filters/`, `layers/GridLayer`, `layers/fov/FOVTreePane`, `gui/component/TableValue`, `gui/component/SliderValueLabel`, `gui/component/JHVSlider`, `gui/component/WheelSupport`, `gui/component/ToolBar`, `gui/component/MainContentPanel`, `gui/component/CollapsiblePane*`, `gui/time/CalendarPicker`, sidebar integration in `gui/MainFrame`, `gui/component/MoviePanel`, `gui/dialog/ImageDialog`, `gui/dialog/SettingsDialog`, `timelines/gui/TimelinesGUI`, `timelines/gui/TimelinePanel`, `timelines/gui/DrawControllerOptions` and `plugins/swek/swing/SWEKSwingUI`, `plugins/swek/swing/SWEKTreePane`, `layers/ViewpointLayerOptions`, `layers/ViewpointLayerOptionsExpert`, viewpoint lifecycle in `layers/Layers` | `sidebar`: [SidebarTest](sidebar/SidebarTest.java) checks filtered layer selection, visibility, reorder/removal, plugin registration/removal, editor retention and selected-tab sizing, stable image/FITS columns and slider bounds across value changes and disclosures, full transport readouts and timestamp caret stability at multiple font sizes, Movie construction without a MainFrame sidebar, Viewpoint mode transitions and reselection, fractional movie-time synchronization and manual edits, Spiral enablement and restored speed, saved video format retention, and Grid label state round trips; `opengl`: [GridRenderingTest](opengl/GridRenderingTest.java) renders grids with zero/full label opacity across map modes and display scales; `commands`: [ViewpointStateTest](commands/ViewpointStateTest.java) checks session restoration, camera transforms, Spiral and Planets/Stars persistence, defaults, bounds and detached viewpoint listeners | The sidebar suite needs a desktop but no graphics context. Verify themes, tab colors, hover, borders, typography, control placement, whole-sidebar scrolling, plugin installation/removal, Timelines plot installation/removal, collapse/maximize and canvas restoration, calendar navigation across month ends and leap years, proxy credential editing and saving, FOV tree expansion, image dataset add/replace dialogs and catalog-driven default intervals, image-request interval edits and updates from shared settings, the Timelines controls above the New Layer/Lock movie row, timeline interactions and canvas behavior in the application. Location drawing also needs the astronomy/rendering checks above. |
| `gui/dialog/SoarDialog`, `gui/dialog/PunchDialog`, `io/SoarClient`, `io/PunchClient` | No automated archive-dialog coverage | Verify overlapping searches and query edits, PUNCH level/product changes with responses arriving out of order, coverage defaults versus manually edited time ranges, and Add/Cancel. Remote archive availability is separate. |
| Repository-root `extra/licenses/`, `extra/ffmpeg/` | `maintenance`: license and FFmpeg updater unit tests | Downloads are mocked. These checks do not run an installed FFmpeg encoder. ANGLE updates have their own [validation workflow](../angle/README.md). |

The execution lists in [run_tests.py](run_tests.py) and its delegated runners are
authoritative for what a command runs. Update this table in the same patch when
production paths move, suites change, or coverage is added/removed. Inspect test
assertions before treating a mapped suite as evidence for a particular change.

For `extra/jpeg2000/build_openjpeg.py` and
`.github/workflows/build-openjpeg-natives.yml`, run `actionlint` on the workflow,
build on each affected platform, then run `check_native_build.py --decoder openjpeg
--tolerance 8`. The builder checks architecture and the macOS, glibc or Windows
subsystem baseline. Windows cross-compilation does not verify DLL loading;
the Windows runner must execute the Java check. The KDU workflow in the separate
native-build repository runs the same validator with `--decoder kdu`.

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
[WCSLIB guide](../wcs/README.md) for the native dependency and focused checks.
The full production-fragment cases run separately on both backends:

```sh
python3 extra/test/wcs/test_wcs_rendering.py --backend default
python3 extra/test/wcs/test_wcs_rendering.py --backend swiftshader
```

Java, the syntax validator and the Electron harness concatenate `imageCommon.frag`,
`imageGeometry.frag` and the mode fragment in that order, without separators.

`ImageRenderingTest` checks actual layer bindings, rendering and picking, HPC/warp scales,
one-to-one zoom and Orthographic fit/reset against independent geometry. It covers observer and
surface images, mixed comparisons, decoded grids, masks, slits and image processing.
Regressions cover surface-map texture lookup with rotated display-frame masks, and wide-field HPC rays
on both sides of 90°,
including infinite/finite outer radii, sector direction, square cut-off, enhancement disabled
at and beyond the Sun-plane horizon, and difference mode.
`WcsLibraryTest` checks native geometry, domains, parameters and ownership. `WcsInterpreterTest`
checks the [captured instrument fixtures](data/wcs-instruments/README.md) and metadata conventions.

Full-fragment checks cover native frames, surface geometry, depth and Orthographic TAN Orthographic
sampling. Coordinate diagnostics reuse the production geometry helpers. The differential-rotation check validates rates against the published magnetic model at 0°, 30°, 60° and 90° latitude. Local cases also check finite-distance source horizons, differential rotation across the limb,
and both image slots. Direct Ortho/HPC comparisons include an independent formal-TAN control. Use
`--ortho-hpc-only` for that subset and `--output-dir` to retain coordinate buffers and
`ortho-hpc-comparison.json`. Trigonometry probes check the selected hardware/SwiftShader helper
and independent controls with a maximum error of `2e-7`; raw SwiftShader intrinsics are diagnostic.

ANGLE updates and old/new image comparisons are documented in
[the ANGLE updater guide](../angle/README.md).

## Live services and benchmarks

These remain explicit and are never part of the default run:

```sh
python3 extra/test/j2k/run_j2k_tests.py --live
python3 extra/test/event/check_hek_server.py
python3 extra/test/timelines/run_timelines_tests.py --benchmark
python3 extra/test/timelines/run_timelines_tests.py --hapi-benchmark /path/to/capture
python3 extra/test/timelines/run_rendering_benchmark.py --output /path/to/new-run --scale 2
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

The timeline raster regressions define aliased band traces on the nearest device-pixel
grid. They evaluate the line equation with rational arithmetic, accept either
nearest pixel at an exact halfway tie, and check endpoints, joins, colors and gaps.
Clipped output must equal the full trace masked by the viewport at pixel centers.

The headful timeline rendering benchmark requires a desktop and an accelerated
Java2D graphics configuration. It exercises the production chart cache with
five-layer synthetic reduced, full-resolution, and multicolor line workloads,
overlay and stacked layouts, and pan/zoom interval sequences. Graph preparation
runs synchronously for deterministic timing. An accelerated offscreen destination
and a final readback include completed GPU work without window-manager timing.
These measurements are not interactive frame rates or live HAPI workloads.

Run each device scale in a separate JVM with `--scale 1` or `--scale 2`.
`--workload` selects one fixture. Each output directory must be new and contains
logs, frames, an isolated JHV home, source snapshots, and the exact Java command.

Export capture measurements include the frame-source callback and native image
copy, with both reused and redrawn chart caches. At 2x, the runner also writes
60-frame timeline-only movies through the production export writer and bundled
FFmpeg, reporting capture, scaling/raw-file writing, and final encoding separately.
They exclude the application's playback scheduling, encoder queue, and main-canvas
capture. `--ffmpeg` can override the bundled encoder path.

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
