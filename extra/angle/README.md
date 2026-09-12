# Updating ANGLE

Use Python 3.12+ with NumPy and Pillow,
JDK 25, Ant, `glslangValidator`, and a working local graphics backend.
Run the commands below in a Python environment containing these dependencies.
The optional Electron reference tests also require Astropy.

## Stage and compare

With the four extracted build artifacts in Downloads:

```sh
python3 extra/angle/update_angle.py 44.3.0 \
  --artifacts ~/Downloads --output /tmp/angle-44-review
```

Alternatively, `--release TAG` downloads the four archives from a published
`bogdanni/angle-builds` GitHub release using `gh`. This does not start builds or
publish releases. Run the build workflow separately, selecting all platforms.
Individual-platform artifacts can also be collected under the same directory;
the updater requires their source/build pins to match.

The updater verifies artifact file checksums and platform/version metadata, then
replaces only EGL/GLES entries in staged copies of the current jars. Other native
libraries and jar entries are preserved. The checksummed ANGLE license must agree
across all four builds. Its text and source revision are staged as `ANGLE.txt`. `d3dcompiler_47.dll` is not added to jars:
JHV's native loader extracts the EGL/GLES pair; packaging another DLL alone would
not make it available to the Windows loader.

Previous EGL/GLES libraries come from `HEAD` by default. Both staged JAR sets
preserve every other entry from the current JARs. Use `--baseline-ref COMMIT` to
compare another committed version. The baseline and candidate run with identical
current JHV code in separate JVMs and fresh native caches. This isolates the
ANGLE update from Java/shader changes. The resolved baseline commit, library
hashes, test logs, and staged jars remain in the output directory.

`report.html` shows previous/candidate/difference images. `comparison.json`
contains per-image maximum channel difference, mean difference, and pixel count
above tolerance. Difference images are amplified eight times. The default
`--pixel-tolerance 2` permits two 8-bit levels per channel; any pixel above that
threshold blocks installation and returns a failing exit status. Missing cases
also fail. References are never silently regenerated or accepted.

## Coverage

- Production grids: all five map modes; 128×128, 256×192, 192×384, 512×128,
  and 512×512 logical canvases; 1x/2x pixel scale; labels on/off (100 images).
- Production image shaders: half-float streaming texture re-upload, all five map
  modes at 128, 257 and 512 pixels; gray/inverted LUT, levels, gamma, sharpening,
  difference, radial mask and blending (120 images).
- Existing colored-vertex and model tests: line joins, disconnected segments,
  antialiased points, transparency, depth, face culling and masks (two images,
  plus numerical assertions).
- Existing framebuffer/export tests: capture and state restoration across map
  modes and square/portrait/landscape sizes, including multisampled export.
- GLSL compilation/linking: all production shader programs.

These are offscreen tests on the current machine's default production backend.
They do not validate the native window/compositor path, interactive resizing,
or runtime behavior on other operating systems. Passing images are not a
performance measurement.

## Optional Electron reference

Add `--electron-reference` on an ARM Mac to run the existing production-fragment
and all-mode color/difference WCS tests with the matching Electron version.
If missing, Electron is downloaded from its GitHub release, checked against
`SHASUMS256.txt`, and extracted with `ditto` under `~/electron-vVERSION-darwin-arm64`.
Existing installations are version-checked and left unchanged. A failed launch
or reference test blocks installation. No quarantine attributes or signatures
are changed by the updater.

Electron is a separate reference, not a replacement for testing JHV's actual
bundled libraries. The WCS validator's default version follows `angle.json`;
`JHV_ELECTRON` still overrides its executable.

## Install after validation

Repeat with a new output directory and `--install` to install only after all
selected tests and image comparisons pass. Original files are retained under
`original-files` with repository-relative paths. If copying or license
synchronization fails, the jars, version manifest, and notices are restored.
`angle.json` records the installed version, pins, and binary hashes. Installation
also updates `extra/licenses/ANGLE.txt` from the builds and runs the existing
license synchronizer to refresh distributed notices and the JAR inventory. Review the
jar/manifest changes and commit them yourself. The updater never commits or pushes.

Run updater unit tests with:

```sh
python3 -m unittest discover -s extra/angle -v
```

Run graphics tests independently and retain images with:

```sh
python3 extra/test/run_tests.py opengl --render-output /tmp/jhv-rendering
```
