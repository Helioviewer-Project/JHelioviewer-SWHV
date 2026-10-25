#ifndef JHV_KDU_GEOMETRY_H
#define JHV_KDU_GEOMETRY_H

#include "kdu_compressed.h"
#include "jhv_j2k_decoder.h"

// Inspect a freshly created, seekable codestream, before opening decoder tiles.
// render must describe this codestream's supported grayscale/RGB channels.
// Palette-mapped channels select the palette's single index component, which
// decodes like a grayscale plane; the host applies the color table.
// Read all first tile-part headers before reporting reductions and quality.
// KDU's minimum DWT count is conservative across all tile-components.
// No resampling: selected planes must share their origin/extent and have unit
// base sampling and zero registration. Layer display coordinates start
// at zero; retain level.x/y for translation to the decoder's coordinates.
// Returns NULL, or a rendering limitation with out unchanged. KDU errors propagate;
// the caller owns and destroys the codestream, which is left restricted.
const char *jhv_kdu_read_geometry(kdu_core::kdu_codestream &stream,
                                  const hv_render &render, jhv_j2k_geometry &out);

#endif
