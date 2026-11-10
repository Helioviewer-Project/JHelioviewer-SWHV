# WCSLIB native dependency

JHV uses WCSLIB 8.10 for FITS linear transforms, native celestial frames and CPU
projection conversions. The shared library is named `jhvwcs`; adapter source files,
types and exported C symbols retain the `jhv_wcs` prefix.
`native/wcs/jhv_wcs.h` defines the opaque adapter ABI.
The Java owner and GPU bindings are described in [image coordinates](../../docs/image-coordinates.md).

`wcslib-8.10.tar.xz` contains the unchanged WCSLIB source and generated scanners
vendored by Astropy at the revision recorded in `wcslib.json`, excluding generated
`wcsconfig.h`. The archive retains WCSLIB's GPL and LGPL license texts.
The manifest records its SHA-256. The adapter is separate from the JPEG 2000 bridge.

Build with Python 3, CMake and the platform compiler, as for FITS and JPIP:

```sh
python3 extra/wcs/build.py macos-arm64 --output /path/to/build-output
```

Choose `macos-arm64`, `macos-x64`, `linux-x64` or `windows-x64`. Builds use CMake
Release with Apple Clang on macOS, GCC on Linux and MinGW-w64 on Windows
(`MINGW_ROOT`, default `C:\mingw64`). The platform baselines match JPIP and FITS:
macOS 13.0, Linux glibc 2.17 and Windows PE subsystem 5.2. Linux release builds
require the same pinned manylinux2014 environment used by JPIP.

`native/wcs/CMakeLists.txt` owns the source list, generated configuration, compiler
flags and linking. The Python driver verifies and unpacks the pinned archive,
invokes CMake and checks the binary's architecture, baseline and dependencies.
The vendored Astropy source subset contains no upstream build system.

`--install` atomically installs the result into its `lib/jhv/` resource directory.
Build records contain the commands, source pins, compiler versions and checksums.
The four adapter entrypoints remain exported. GNU runtimes are linked statically.
No Python scientific packages, CFITSIO or Fortran runtime are required to build
or run the native library.


The same [native-library workflow](../../.github/workflows/build-native-libraries.yml)
builds FITS and WCS on the four platform runners:

```sh
gh workflow run build-native-libraries.yml -f library=wcs -f target=all -f jhv_commit=FULL_COMMIT_SHA
```

After building, `check_build.py` verifies the candidate hash and source pins,
compiles fresh classes for the existing `WcsLibraryTest`, and runs its independent
geometry, inverse-domain, parameter and concurrent-ownership checks. Only the
candidate WCS library is staged for loading. Python 3.13 and JDK 25 are the test
runtimes; the FITS application integration is not required.

Successful jobs upload `jhv-wcs-TARGET` artifacts with the library, notices, build
and validation records, logs and checksums. CI requires a clean checkout matching
the requested source SHA. Validate all four platforms before ingesting binaries.

The C11 build enables WCSLIB's thread-local diagnostics and atomic error handling.
The adapter retains only the primary two-axis affine celestial WCS, supports
TAN/ARC/AZP/ZPN/CAR/CEA, and rejects nonzero ZPN coefficients above PV2_5.
Projection coefficients are encoded to float once before setup, so native CPU
evaluation and the GPU use the same effective coefficients. CEA lambda must
remain in `(0,1]` after that encoding. Invalid transforms return NaN pairs.
The native projection codes retain their ABI values, with one shared rule selecting
the PV coefficients to encode and export. Native `to_plane` selects celestial-to-plane
conversion; zero selects plane-to-celestial conversion, including longitude wrapping for CAR/CEA.

Run the focused checks with a Python environment containing NumPy and Astropy:

```sh
python3 extra/test/wcs/compare_java_metadata_to_validator.py
python3 extra/test/run_tests.py shaders opengl
python3 extra/test/wcs/test_wcs_rendering.py --backend default
python3 extra/test/wcs/test_wcs_rendering.py --backend swiftshader
```

The first command includes independent Java geometry/domain tests, concurrency,
adjustment-cache checks and captured instrument metadata regressions, followed
by real and constructed metadata cases. The instrument fixtures cover legacy
axis/unit spellings, retained cube-axis declarations in SPICE/PUNCH JP2 images,
and missing pixel scales. See the
[fixture provenance and reference equations](../test/data/wcs-instruments/README.md).
The adapter's exact input conventions are documented in
[image coordinates](../../docs/image-coordinates.md).
The fragment tests obtain image records packed by the production Java binder using the native adapter,
upload their bytes in WebGL and compare rendered coordinates with independent geometry and Astropy.
They also check tilted references, pole parameters, native fiducials, origin
offsets and the accuracy of angular-input sine/cosine on both graphics backends.

Each frame's metadata prepares and retains a native solution. Decoded grids share
that owner. Its automatic FFM arena frees it after all Java references become
unreachable. Each original image geometry caches its last nonzero CRVAL adjustment;
changing that offset prepares a new native copy, while zero reuses the original.
Replaced copies become eligible for cleanup when no longer referenced.

Image shader draws use prepared values. HPC and warped-view scale preparation
also calls the batched native conversion through `ImageBounds` on each redraw.

Native construction is separate from platform execution: cross-compilation and
the ABI's compile-time assertions do not replace running the Java checks on each
target platform. WebAssembly is not included in this desktop build. The generated
scanners' setjmp/longjmp runtime support remains a browser toolchain requirement.

After rebuilding installed libraries, regenerate dependency notices:

```sh
python3 extra/licenses/sync_licenses.py
```
