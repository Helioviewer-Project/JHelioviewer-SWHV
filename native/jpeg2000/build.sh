#!/bin/bash
# Build the JPEG 2000 bridge for this host into $BUILD, against JHV's bundled Kakadu library
# or, with JHV_J2K_DECODER=openjpeg, with the esajpip client's OpenJPEG.
set -eu
cd "$(dirname "$0")/../.."
root=$PWD
: "${ESAJPIP:=$root/../esajpip-SWHV}"
: "${JHV_J2K_DECODER:=kdu}"
: "${BUILD:=$root/tmp/j2k-native}"
case "$(uname -s)-$(uname -m)" in
    Darwin-arm64) resource=macos-arm64; suffix=dylib ;;
    Darwin-x86_64) resource=macos-amd64; suffix=dylib ;;
    Linux-x86_64) resource=linux-amd64; suffix=so ;;
    *) echo "Unsupported host: build native/jpeg2000 with CMake." >&2; exit 1 ;;
esac
mkdir -p "$BUILD"
options=(-DJHV_J2K_DECODER="$JHV_J2K_DECODER")
bridge_name=jhvj2k_opj
if [ "$JHV_J2K_DECODER" = kdu ]; then
    bridge_name=jhvj2k_kdu
    : "${KDU_VENDOR:?Set KDU_VENDOR to the Kakadu SDK directory}"
    cp "lib/jhv/$resource/libkdu_jni.$suffix" "$BUILD/"
    options+=(-DKDU_VENDOR="$KDU_VENDOR" -DKDU_LIBRARY="$BUILD/libkdu_jni.$suffix")
fi
if [ "$suffix" = dylib ]; then options+=(-DCMAKE_OSX_DEPLOYMENT_TARGET=13.0); fi
cmake -S native/jpeg2000 -B "$BUILD/$bridge_name" -DCMAKE_BUILD_TYPE=Release \
    -DESAJPIP="$ESAJPIP" -DESAJPIP_CLIENT_ONLY="${ESAJPIP_CLIENT_ONLY:-ON}" \
    "${options[@]}"
cmake --build "$BUILD/$bridge_name" --target jhvj2k --parallel
cp "$BUILD/$bridge_name/lib$bridge_name.$suffix" "$BUILD/"
echo "$BUILD/lib$bridge_name.$suffix"
