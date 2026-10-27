# Preparing JPEG 2000 native libraries

The `jhvj2k` CMake target builds the static esajpip client and JPEG 2000 reader as
part of the same project. `JHV_J2K_DECODER=kdu` (the default) produces `jhvj2k_kdu`
and requires a Kakadu SDK and compatible Kakadu libraries, supplied separately.
`JHV_J2K_DECODER=openjpeg` produces `jhvj2k_opj` using the OpenJPEG copy in the
esajpip client and needs no Kakadu files. Both libraries can be packaged together.
GLib, llhttp and zlib are not required.

The manually triggered `build-openjpeg-natives.yml` GitHub Actions workflow in
this repository builds OPJ for macOS arm64, macOS x64, Linux x64 and Windows x64.
It takes full JHV and esajpip commit SHAs that must already be available on GitHub.
The workflow checks out those revisions, builds `jhvj2k_opj`, validates it with
Java 25 against the bundled Kakadu reference, and uploads `jhv-openjpeg-<platform>`
artifacts. The artifacts contain only the OPJ bridge and `build-info.txt`; Kakadu
is used for validation and is removed before upload.

The local counterpart is `extra/jpeg2000/build_openjpeg.py`, for example:

```sh
python3 extra/jpeg2000/build_openjpeg.py macos-arm64 \
    --esajpip ../esajpip-SWHV --output /path/to/openjpeg-builds
```

Linux release builds use the workflow's digest-pinned manylinux2014 container,
with a newer compiler on the glibc 2.17 baseline. The builder rejects newer glibc
requirements and links GNU runtimes statically. Windows uses the MinGW toolchain
in `C:\mingw64` by default, overridable through `MINGW_ROOT`. Mac builds target
macOS 13.0. Each artifact records source revisions, local changes, CMake/compiler
versions and a SHA-256 checksum. Local builds do not trigger validation automatically.
KDU builds remain in the separate native-build repository's existing workflow.

JHV loads only the selected bridge: KDU by default, or OpenJPEG with the JVM option
`-Djhv.j2k.decoder=openjpeg`. The accepted values are `kdu` and `openjpeg`.
Kakadu's support libraries are loaded only when KDU is selected. There is no
automatic fallback if the selected bridge is missing.
Rebuild and package the named bridges before running this version of JHV;
the former `jhvj2k` library is no longer loaded.

Both decoders serve the same C interface. The OpenJPEG bridge keeps a decoder
workspace for each concurrent decode, with up to eight worker threads, and reuses
it for later decodes. It does not drop coding passes below display precision, as
Kakadu's quality limiter does, so reduced resolutions can differ from Kakadu's by
a few gray levels. Local inspection also checks tile-part COD/COC headers to bound
the advertised resolutions, skipping packet data. OpenJPEG can expose fewer levels
than Kakadu when it rejects a reduction before reading a later coding override.
JPIP reconstruction uses the served profile's main-header parameters.

Use CMake 3.20 or later and a C11/C++11 compiler. From the JHV root, build
against supplied libraries:

```sh
cmake -S native/jpeg2000 -B /path/to/build -DCMAKE_BUILD_TYPE=Release \
    -DESAJPIP=/path/to/esajpip -DKDU_VENDOR=/path/to/kakadu-sdk \
    -DKDU_LIBRARY=/path/to/libkdu_jni.so
cmake --build /path/to/build --target jhvj2k --parallel
```

Use `.dylib` on macOS, with `-DCMAKE_OSX_DEPLOYMENT_TARGET=13.0` and the
appropriate `-DCMAKE_OSX_ARCHITECTURES=arm64` or
`-DCMAKE_OSX_ARCHITECTURES=x86_64`. Windows requires MinGW and also
`-DKDU_CORE_LIBRARY=/path/to/kdu_v7AR.dll`; supply `kdu_jni.dll` as `KDU_LIBRARY`.
MinGW produces `jhvj2k_kdu.dll` or `jhvj2k_opj.dll`, without a `lib` prefix.
The SDK and libraries must match, and all libraries must target the same architecture.
The bridge and Java binding must also match: frame geometry includes the full
decoder origin and each level's dimensions. Rebuild every platform's bridge when changing
that interface.
The Windows JNI/support DLL must also export the Kakadu support functions used
by the bridge.

On macOS arm64, macOS x86-64 or Linux x86-64, the host script builds against
JHV's bundled Kakadu library:

```sh
KDU_VENDOR=/path/to/kakadu-sdk bash native/jpeg2000/build.sh
```

or with OpenJPEG:

```sh
JHV_J2K_DECODER=openjpeg bash native/jpeg2000/build.sh
```

It writes the built bridge (and the extracted Kakadu library for KDU) to
`tmp/j2k-native`. Backend-specific build directories inside it allow both commands
to use the same output directory without overwriting each other's library.
Set `BUILD` to change this directory and `ESAJPIP` to change the default
esajpip checkout, `../esajpip-SWHV`.

Validate each platform's libraries on that platform with
`extra/test/j2k/check_native_build.py` before staging. The check requires
Python 3, JDK 25 and an esajpip source checkout containing its existing test
fixtures; it does not build esajpip or require server dependencies.

Each platform directory must contain `build-info.txt` with SHA-256 checksums
in `sha256sum` format for its libraries, using filenames without directory
prefixes, for example:

```sh
cd /path/to/builds/linux-x64
sha256sum libkdu_jni.so libjhvj2k_kdu.so libjhvj2k_opj.so > build-info.txt
```

On macOS use `shasum -a 256`. For Windows include `kdu_v7AR.dll`, `kdu_jni.dll`
and `jhvj2k_kdu.dll`, and include `jhvj2k_opj.dll` for OpenJPEG.
Source revisions and compiler versions can also be recorded
in this file.

Collect the validated libraries into a directory named for each platform:

```text
builds/macos-arm64/
builds/macos-x64/
builds/linux-x64/
builds/windows-x64/
```

From the JHV root, stage the updated libraries:

```sh
python3 extra/jpeg2000/prepare_natives.py /path/to/builds /path/to/staged-natives
```

The Python 3 script verifies build checksums and stages the recorded decoder
libraries under `staged-natives/jhv/<platform>/`. A KDU build record must include
its bridge and all Kakadu support libraries. An OpenJPEG-only record lists only
its bridge; a combined record lists both sets. Review those files and the build
records before copying the staged `jhv` directory into `lib`. Other native files
are not replaced. `--platform macos-arm64`, for example, stages a single platform.
KDU and OPJ artifacts can be staged separately into the same staging directory;
their records are saved as `<platform>-kdu-build-info.txt` and
`<platform>-openjpeg-build-info.txt` so neither overwrites the other. A combined
artifact produces `<platform>-both-build-info.txt`.

To check a local build without rebuilding it:

```sh
python3 extra/test/j2k/check_native_build.py /path/to/builds/macos-arm64 --esajpip ../esajpip-SWHV
```

An OpenJPEG bridge is checked the same way, with JHV's bundled Kakadu library copied
beside it as the reference and a tolerance for reduced resolutions, where Kakadu's
quality limiter drops passes below display precision:

```sh
cp lib/jhv/macos-arm64/libkdu_jni.dylib /path/to/builds/macos-arm64/
python3 extra/test/j2k/check_native_build.py /path/to/builds/macos-arm64 --esajpip ../esajpip-SWHV --decoder openjpeg --tolerance 8
```

The validator defaults to `--decoder kdu`. Run it once for each backend when
packaging both. Kakadu remains the validator's reference even for an OpenJPEG-only
build, so its support library must be present in the validation directory.

The check runs `J2KNativeTest` through JHV's Java binding. It compares every
frame and resolution of the existing RGB, grayscale and JPX fixtures in the
esajpip checkout against Kakadu's compositor, and checks each level's origin and
dimensions against Kakadu's codestream geometry. It also checks concurrent decode
jobs that outlive their source, failure recovery, repeated open/decode/close
cycles, and malformed JPIP response parsing. File-descriptor counts are checked
on Unix.

Small palette files are generated in a temporary directory and removed afterward.
They check raw indices and fixed expected bytes for the 256-entry RGBA8 table,
including channel ordering, signed and unsigned sample conversion, opaque alpha,
last-entry padding, short buffers and unsupported indices. Every palette file must
decode to fixed expected raw indices. One eight-bit palette file also runs the
compositor comparison and job lifetime checks. Color tables are checked separately.

The additional `extra/test/j2k/run_native_test.sh` test builds the bridge and
generates JPIP responses using esajpip's server code. It requires the server
build dependencies, `KDU_VENDOR`, and at least one JP2/JPX file argument.
See [the test README](../test/j2k/README.md) for commands and test limits.
