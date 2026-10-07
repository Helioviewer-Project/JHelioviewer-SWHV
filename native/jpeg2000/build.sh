#!/bin/bash
# Build the JPEG 2000 bridge for this host into $BUILD, against JHV's bundled Kakadu library.
set -eu
cd "$(dirname "$0")/../.."
root=$PWD
: "${ESAJPIP:=$root/../esajpip-SWHV}"
: "${KDU_VENDOR:=$root/../kdu/v7_A_3-00781N}"
: "${BUILD:=$root/tmp/j2k-native}"
case "$(uname -s)-$(uname -m)" in
    Darwin-arm64) target=macos-arm64; resource=macos-arm64; suffix=dylib ;;
    Darwin-x86_64) target=macos; resource=macos-amd64; suffix=dylib ;;
    Linux-x86_64) target=linux; resource=linux-amd64; suffix=so ;;
    *) echo "Unsupported host: build native/jpeg2000 with CMake." >&2; exit 1 ;;
esac
mkdir -p "$BUILD"
unzip -p "lib/jhv/jhv-natives-$target.jar" "jhv/$resource/libkdu_jni.$suffix" > "$BUILD/libkdu_jni.$suffix"
options=()
if [ "$suffix" = dylib ]; then options+=(-DCMAKE_OSX_DEPLOYMENT_TARGET=13.0); fi
cmake -S "$ESAJPIP" -B "$BUILD/client" -DCMAKE_BUILD_TYPE=Release \
    -DESAJPIP_CLIENT_ONLY="${ESAJPIP_CLIENT_ONLY:-ON}" \
    -DCMAKE_POSITION_INDEPENDENT_CODE=ON "${options[@]}"
cmake --build "$BUILD/client" --target esajpip_client --parallel
cmake -S native/jpeg2000 -B "$BUILD/bridge" -DCMAKE_BUILD_TYPE=Release \
    -DESAJPIP="$ESAJPIP" -DESAJPIP_BUILD="$BUILD/client" \
    -DKDU_VENDOR="$KDU_VENDOR" -DKDU_LIBRARY="$BUILD/libkdu_jni.$suffix" "${options[@]}"
cmake --build "$BUILD/bridge" --parallel
cp "$BUILD/bridge/libjhvj2k.$suffix" "$BUILD/"
echo "$BUILD/libjhvj2k.$suffix"
