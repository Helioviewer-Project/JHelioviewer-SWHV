# Preparing JPEG 2000 native libraries

The `jhvj2k` bridge builds the static esajpip client and JPEG 2000 reader as
part of the same CMake project. It requires a Kakadu SDK and compatible Kakadu
libraries, supplied separately. OpenJPEG, GLib, llhttp and zlib are not required.

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
MinGW produces `libjhvj2k.dll`; copy it as `jhvj2k.dll` before validation and packaging.
The SDK and libraries must match, and all libraries must target the same architecture.
The Windows JNI/support DLL must also export the Kakadu support functions used
by the bridge.

On macOS arm64, macOS x86-64 or Linux x86-64, the host script builds against
JHV's bundled Kakadu library:

```sh
KDU_VENDOR=/path/to/kakadu-sdk bash native/jpeg2000/build.sh
```

It writes the extracted Kakadu library and built bridge to `tmp/j2k-native`.
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

The Python 3 script verifies build checksums, replaces the Kakadu and bridge
entries in copies of the current native JARs, and verifies that unrelated entries retain
their contents. Review the staged JARs and build records before copying the JARs
into `lib/jhv`. `--platform macos-arm64`, for example, stages a single platform.

To check a local build without rebuilding it:

```sh
python3 extra/test/j2k/check_native_build.py /path/to/builds/macos-arm64 --esajpip ../esajpip-SWHV
```

The check runs `J2KNativeTest` through JHV's Java binding. It compares every
frame and resolution of the existing RGB, grayscale and JPX fixtures in the
esajpip checkout against Kakadu's compositor. It also checks concurrent decode
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
