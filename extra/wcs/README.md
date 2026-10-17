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

Build with Python 3 and Zig 0.17.0:

```sh
python3 extra/wcs/build.py /path/to/build-output
```

This builds macOS arm64/x64, Linux x64 and Windows x64. Add
`--platform macos-arm64` to build one target. `--install` atomically installs the
libraries into the corresponding `lib/jhv/` resource directories. Build records
and checksums are saved in the output directory. Debug information is stripped;
the four adapter entry points remain exported. Both macOS builds target macOS 13.0
and use the installed Apple SDK. No Python scientific packages, CFITSIO or Fortran
runtime are required to build or run the native library.

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
The fragment tests obtain prepared uniforms from the actual Java/native adapter
and compare rendered coordinates with independent geometry and Astropy.
They also check tilted references, pole parameters, native fiducials, origin
offsets and the accuracy of angular-input sine/cosine on both graphics backends.

Each frame's metadata prepares and retains a native solution. Decoded grids share
that owner. Its automatic FFM arena frees it after all Java references become
unreachable. Each source solution caches its last nonzero CRVAL adjustment;
changing that offset prepares a new native copy, while zero reuses the original.
Replaced copies become eligible for cleanup when no longer referenced.

Image shader draws use prepared values. HPC and warped-view scale preparation
also calls the batched native conversion through `ImageBounds` on each redraw.

A warmed local probe on 2026-10-17 measured about 27 microseconds per new
`WcsSolution`, 7.4 microseconds per changed CRVAL adjustment and 0.7 microseconds
per eight-point `ImageBounds.hpc()` calculation, including Java allocations and
FFM. These are median operation timings on an Apple M3 Pro using the normalized
TAN header from `extra/test/data/sample.171.fits`. Construction starts from an
in-memory header; FITS/XML reading, metadata normalization and native-library/JIT
startup are outside these measurements. They do not measure the incremental
movie-loading cost over the previous implementation.

Native construction is separate from platform execution: cross-compilation and
the ABI's compile-time assertions do not replace running the Java checks on each
target platform. WebAssembly is not included in this desktop build. The generated
scanners' setjmp/longjmp runtime support remains a browser toolchain requirement.

After rebuilding installed libraries, regenerate dependency notices:

```sh
python3 extra/licenses/sync_licenses.py
```
