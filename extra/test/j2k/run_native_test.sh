#!/bin/bash
# Build the JPEG 2000 bridge into $BUILD and check it, through J2KNative, against
# Kakadu's compositor. Arguments: JP2/JPX files.
set -euo pipefail
if [ "$#" -eq 0 ]; then
    echo "Usage: $0 JP2_OR_JPX [JP2_OR_JPX ...]" >&2
    exit 2
fi
cd "$(dirname "$0")/../../.."
root=$PWD
: "${ESAJPIP:=$root/../esajpip-SWHV}"
: "${BUILD:=$root/tmp/j2k-native}"
export ESAJPIP BUILD
# The response generator below needs the full esajpip configuration.
library=$(ESAJPIP_CLIENT_ONLY=OFF bash native/jpeg2000/build.sh | tail -1)
kdu=$BUILD/libkdu_jni.${library##*.}
test=$BUILD/test
mkdir -p "$test/responses" "$test/classes"

# JPIP responses from esajpip's own server code, for its RGB fixture.
image=$ESAJPIP/tests/transcode/fixtures/kakadu/synthetic_rgb_129x129_CPRL_SOP_EPH.jp2
cmake --build "$BUILD/bridge" --target test_client_source --parallel > "$test/server.log"
"$BUILD/bridge/client/tests/client/test_client_source" --write-responses "$test/responses"

# That fixture declared as sYCC, and a container around an invalid codestream.
python3 - "$image" "$test" <<'EOF'
import struct, sys
source, folder = open(sys.argv[1], 'rb').read(), sys.argv[2]
def boxes(data):
    at = 0
    while at < len(data):
        size, kind = struct.unpack_from('>I4s', data, at)
        head = 8
        if size == 1: size, head = struct.unpack_from('>Q', data, at + 8)[0], 16
        if size == 0: size = len(data) - at
        yield kind, data[at + head:at + size]
        at += size
def box(kind, payload): return struct.pack('>I4s', 8 + len(payload), kind) + payload
def rebuilt(change): return b''.join(box(kind, change(kind, payload)) for kind, payload in boxes(source))
def sycc(kind, payload):
    if kind != b'jp2h': return payload
    return b''.join(box(k, bytes([1, 0, 0]) + struct.pack('>I', 18) if k == b'colr' else v) for k, v in boxes(payload))
open(folder + '/sycc.jp2', 'wb').write(rebuilt(sycc))
open(folder + '/malformed.jp2', 'wb').write(rebuilt(lambda kind, payload: b'\xffO\xffQ\x00\x02' if kind == b'jp2c' else payload))
EOF

classpath=$(find lib extra/test/j2k/lib -name '*.jar' | tr '\n' ':')
javac -nowarn -cp "$classpath" -d "$test/classes" \
    src/org/helioviewer/jhv/view/j2k/J2KNative.java extra/test/j2k/J2KNativeTest.java
python3 - "$classpath$test/classes" "$kdu" "$library" "$test" "$image" "$@" <<'EOF'
import os, subprocess, sys
subprocess.run(["java", "--enable-native-access=ALL-UNNAMED", "-cp", sys.argv[1],
                "org.helioviewer.jhv.view.j2k.J2KNativeTest", *sys.argv[2:]],
               check=True, timeout=int(os.environ.get("JHV_NATIVE_TEST_TIMEOUT", "600")))
EOF
