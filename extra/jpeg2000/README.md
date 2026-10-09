# Preparing JPEG 2000 native libraries

The `jhvj2k` bridge builds the static esajpip client and JPEG 2000 reader as
part of the same CMake project. It requires a Kakadu SDK and compatible Kakadu
libraries, supplied separately. OpenJPEG, GLib, llhttp and zlib are not required.

With CMake 3.20 or later, build against supplied libraries:

```sh
cmake -S native/jpeg2000 -B /path/to/build -DCMAKE_BUILD_TYPE=Release \
    -DESAJPIP=/path/to/esajpip -DKDU_VENDOR=/path/to/kakadu-sdk \
    -DKDU_LIBRARY=/path/to/libkdu_jni.so
cmake --build /path/to/build --target jhvj2k --parallel
```

Use `.dylib` on macOS, with `-DCMAKE_OSX_DEPLOYMENT_TARGET=13.0` and the
appropriate `-DCMAKE_OSX_ARCHITECTURES`. Windows requires MinGW and also
`-DKDU_CORE_LIBRARY=/path/to/kdu_v7AR.dll`; supply `kdu_jni.dll` as `KDU_LIBRARY`.
The SDK and libraries must match, and all libraries must target the same architecture.
The Windows JNI/support DLL must also export the Kakadu support functions used
by the bridge.

For a host build against JHV's bundled Kakadu library:

```sh
KDU_VENDOR=/path/to/kakadu-sdk bash native/jpeg2000/build.sh
```

Validate each platform's libraries with `extra/test/j2k/check_native_build.py`
before staging. Each platform directory must contain `build-info.txt` with
SHA-256 checksums in `sha256sum` format for its libraries, for example:

```sh
cd /path/to/builds/linux-x64
sha256sum libkdu_jni.so libjhvj2k.so > build-info.txt
```

On macOS use `shasum -a 256`. For Windows include `kdu_v7AR.dll`, `kdu_jni.dll`
and `jhvj2k.dll`. Source revisions and compiler versions can also be recorded
in this file.

Collect the validated libraries into a directory named for each platform:

```text
builds/macos-arm64/
builds/macos-x64/
builds/linux-x64/
builds/windows-x64/
```

From the JHV root, stage the updated JARs:

```sh
python3 extra/jpeg2000/prepare_natives.py /path/to/builds /path/to/staged-jars
```

The script verifies build checksums, replaces the KDU and bridge entries in
copies of the current native JARs, and verifies that unrelated entries retain
their contents. Review the staged JARs and build records before copying the JARs
into `lib/jhv`. `--platform macos-arm64`, for example, stages a single platform.

To check a local build without rebuilding it:

```sh
python3 extra/test/j2k/check_native_build.py /path/to/builds/macos-arm64 --esajpip ../esajpip-SWHV
```

This uses the existing native test and committed RGB, grayscale and JPX fixtures.
Small generated palette fixtures check raw indices and fixed expected RGBA table
bytes, including channel ordering, signed sample conversion and last-entry padding.
They also check short buffers and unsupported index depths. Indexed decode comparisons
use the compositor's raw-component path, as JHV does on `master`.
The Java binding consumes a 256-entry RGBA8 table and raw unsigned index bytes.
It checks every frame and resolution against Kakadu's compositor, jobs that
outlive their source, failure recovery, and repeated open/decode/close cycles.
File-descriptor counts are checked on Unix. The full JPIP test still uses
`extra/test/j2k/run_native_test.sh` and requires the server build dependencies.
