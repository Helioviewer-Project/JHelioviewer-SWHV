#!/bin/sh
set -eu

MSDF_ATLAS_GEN=${MSDF_ATLAS_GEN:-/tmp/jhv-msdf-atlas-gen-build/bin/msdf-atlas-gen}

"$MSDF_ATLAS_GEN" \
    -font extra/DejaVuSansCondensed.ttf \
    -charset resources/msdf/charset \
    -type sdf \
    -format png \
    -size 128 \
    -pxrange 4 \
    -pxpadding 12 \
    -outerpxpadding 12 \
    -angle 3.0 \
    -coloringstrategy simple \
    -imageout resources/msdf/atlas-128.png \
    -json resources/msdf/atlas-128.json \
    -threads 0
