# Native FITS reader

The reader uses unmodified CFITSIO 4.7.0 and zlib 1.3.1. `cfitsio.json` pins
upstream revisions and source-archive SHA-256 values. The archives contain full
upstream source snapshots, matching the existing WCSLIB vendoring policy. This
allows builds without downloading dependency sources; it is not a CFITSIO
requirement. Builds use each dependency's CMake project, followed
by the adapter in `native/fits`, without a manually maintained source-file list.

## Build

```sh
python3 extra/cfitsio/build.py macos-arm64 --output /path/to/output
```

Choose `macos-arm64`, `macos-x64`, `linux-x64` or `windows-x64`. As with the native
JPIP client, builds use CMake Release with Apple Clang on macOS, GCC on Linux and
MinGW-w64 on Windows (`MINGW_ROOT`, default `C:\mingw64`). The platform baselines
match JPIP: macOS 13.0, Linux glibc 2.17 and Windows PE subsystem 5.2. Linux release
builds use the same digest-pinned manylinux2014 container. Windows links its GNU
runtimes statically. CFITSIO and zlib are linked statically on every platform.
No Fortran compiler or CFITSIO networking is used.

The builder verifies both archives and the resulting binary's architecture,
platform baseline and dependencies. `builds.json` records commands, compiler
versions, dependency pins and output hashes. Floating-point contraction is
disabled for consistent quantized reconstruction. Optional `--install` atomically
copies the result into `lib/jhv`; validate the candidate before installing it.

## Independent validation

```sh
python3 extra/cfitsio/check_build.py /path/to/output --platform macos-arm64
```

Validation uses Python's standard library and loads the candidate by its absolute
path through `ctypes`. It requires no application classes, Java installation, NTF
JAR, WCS library or SPICE library. A missing entrypoint or broken candidate fails;
no bundled reader substitutes for it.

The checks compare all planes, subsets and row bands against 13 committed
CFITSIO/Astropy fixtures. They cover integer types, Rice, gzip, HCOMPRESS, PLIO, quantized float
and double cubes, zero/null markers, tile edges, concurrent first-use quantized
reads, malformed input, invalid HDUs and insufficient output capacity. Reference
pixels come from the released CFITSIO C API, independently of this adapter.
See [fixture provenance](../test/data/fits/README.md). These are native-library
checks; application ownership, metadata, display and layer-selection tests run
separately when integrating the reader.

## GitHub Actions artifacts

The manually triggered [build-native-libraries.yml](../../.github/workflows/build-native-libraries.yml)
workflow builds the libraries independently of application integration. It takes
a full source commit SHA available on GitHub and a target (`all` by default).
The workflow itself must also be available on GitHub before dispatch.

```sh
gh workflow run build-native-libraries.yml -f library=cfitsio -f target=all -f jhv_commit=FULL_COMMIT_SHA
```

Each job builds with the same script, then runs the independent validation on
macOS ARM64, macOS Intel, Linux x64 or Windows x64. A separate step compiles fresh
Java classes and checks the application's FITS reader against only that candidate
library. The workflow requires Python 3.13 and JDK 25. CI requires a clean checkout
matching the requested source SHA.

Successful checks produce `jhv-cfitsio-TARGET` artifacts containing the library,
`builds.json`, `validation.json`, notices, test logs and `SHA256SUMS`. Validation
records the source revision, host architecture, workflow revision and Actions
run URL. Failed checks upload diagnostics. Artifacts are retained for seven days.
Verify all four artifacts' checksums, matching source revisions and passed
validation records before copying the libraries into `lib/jhv` and regenerating
licenses. The workflow does not commit or publish a release.

The Java reader checks appear in the workflow's `Validate the Java FITS reader`
step. To run them locally against a build output directory:

```sh
python3 extra/test/uri/check_fits_reader.py /path/to/output --platform macos-arm64
```

Local validation marks dirty source explicitly. Rechecking clears the previous
artifact first, so a failed retry leaves no stale success package.

## Native interface

`jhv_fits_init()` checks reentrancy and initializes CFITSIO and its dither table.
Call it before starting reads. It returns zero on success, nonzero on failure.
There is no adapter-version negotiation.

`jhv_fits_read()` accepts caller-owned input memory and output storage. HDUs are
one-based; planes and rows are zero-based. It reads the requested plane, row band
or strided subset. The caller supplies dimensions, BITPIX, output capacity in
bytes and a 128-byte error buffer. Input must remain stable until the call returns.

Each read opens CFITSIO's read-only memory-file driver and closes its handle before
returning. No filesystem path or persistent native handle crosses the interface.
Integer BLANK and BZERO/BSCALE conversion remains the caller's responsibility.
CFITSIO reconstructs quantized floating-point pixels and substitutes NaN for
floating nulls. The adapter reports status text without consuming CFITSIO's shared
error-message stack.

## Application input ownership

`FITSSource` uses released NTF 1.23.1 for headers and CFITSIO for image pixels.
Header scans are file-backed and leave payloads deferred. The selected HDU,
dimensions, pixel type, scaling and tile height are captured once. Decodes map
the input through Java's file API, join all workers, and unmap before returning.
Workers split at tile boundaries so a tile is not decompressed by multiple bands.

Input files must remain stable during reads. Outer gzip input is inflated once
into Java's file cache and deleted when the source closes, after active reads
finish. A long gzip sequence therefore retains its uncompressed files until
its sources close. Packaged detector masks use Java-owned memory rather than
filesystem paths. CFITSIO receives no paths or persistent handles.
