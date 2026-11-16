#!/bin/sh
set -eu

# Writes the event icons from their SVG sources; run from the repository root
for svg in extra/icons/*.svg; do
    rsvg-convert -w 128 -h 128 "$svg" -o "resources/images/EventIcons/$(basename "$svg" .svg).png"
done
