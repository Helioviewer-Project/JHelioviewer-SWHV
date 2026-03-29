#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INPUT="${1:-$SCRIPT_DIR/jhv_wcs_hpc_validation_note.md}"
OUTPUT="${INPUT%.md}.pdf"
INPUT_DIR="$(cd "$(dirname "$INPUT")" && pwd)"
INPUT_BASENAME="$(basename "$INPUT")"
CCN4_ROOT="${CCN4_ROOT:-$HOME/jhv/ccn4.wiki}"
IMAGE_NAME="ccn4.doc"

if [[ ! -f "$INPUT" ]]; then
    echo "Input file not found: $INPUT" >&2
    exit 1
fi

if [[ ! -d "$CCN4_ROOT" ]]; then
    echo "CCN4 root not found: $CCN4_ROOT" >&2
    exit 1
fi

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/jhv_pdf_XXXXXX")"
trap 'rm -rf "$TMP_DIR"' EXIT

mkdir -p "$TMP_DIR/doc"
cp -R "$INPUT_DIR"/. "$TMP_DIR/doc/"

if command -v docker >/dev/null 2>&1 && docker image inspect "$IMAGE_NAME" >/dev/null 2>&1; then
    cat >"$TMP_DIR/wrapper.md" <<'EOF'
\include{/ccn4.wiki/templates/preamble.md}
\include{/work/doc/INPUT_BASENAME_PLACEHOLDER}
EOF
    sed -i.bak "s|INPUT_BASENAME_PLACEHOLDER|$INPUT_BASENAME|" "$TMP_DIR/wrapper.md"
    rm -f "$TMP_DIR/wrapper.md.bak"

    docker run --rm \
        -v "$CCN4_ROOT":/ccn4.wiki \
        -v "$TMP_DIR":/work \
        "$IMAGE_NAME" \
        /bin/sh -lc '
            export PATH=/root/bin:$PATH
            mkdir -p "$HOME/.pandoc"
            ln -sf /ccn4.wiki/templates/ "$HOME/.pandoc/"
            cd /ccn4.wiki/RP
            gpp -x -T /work/wrapper.md | pandoc - \
                --standalone \
                --wrap=none \
                --syntax-highlighting=idiomatic \
                --number-sections \
                --toc \
                --top-level-division=chapter \
                --resource-path=/work/doc:/ccn4.wiki/RP:/ccn4.wiki \
                -V classoption=oneside \
                --template /ccn4.wiki/templates/eisvogel.latex \
                --pdf-engine=xelatex \
                -o /work/out.pdf
        '
else
    cat >"$TMP_DIR/wrapper.md" <<EOF
\include{$CCN4_ROOT/templates/preamble.md}
\include{$TMP_DIR/doc/$INPUT_BASENAME}
EOF

    if ! command -v gpp >/dev/null 2>&1; then
        echo "gpp not found in PATH and no prebuilt docker image '$IMAGE_NAME' is available" >&2
        exit 1
    fi
    if ! command -v pandoc >/dev/null 2>&1; then
        echo "pandoc not found in PATH and no prebuilt docker image '$IMAGE_NAME' is available" >&2
        exit 1
    fi
    if ! command -v xelatex >/dev/null 2>&1; then
        echo "xelatex not found in PATH and no prebuilt docker image '$IMAGE_NAME' is available" >&2
        exit 1
    fi
    (
        cd "$CCN4_ROOT/RP"
        gpp -x -T "$TMP_DIR/wrapper.md" | pandoc - \
            --standalone \
            --wrap=none \
            --syntax-highlighting=idiomatic \
            --number-sections \
            --toc \
            --top-level-division=chapter \
            --resource-path="$TMP_DIR/doc:$CCN4_ROOT/RP:$CCN4_ROOT" \
            -V classoption=oneside \
            --template "$CCN4_ROOT/templates/eisvogel.latex" \
            --pdf-engine=xelatex \
            -o "$TMP_DIR/out.pdf"
    )
fi

mkdir -p "$(dirname "$OUTPUT")"
cp "$TMP_DIR/out.pdf" "$OUTPUT"
echo "Wrote $OUTPUT"
