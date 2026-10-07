#ifndef JHV_KDU_DECODE_H
#define JHV_KDU_DECODE_H

#include "jhv_kdu_geometry.h"

// Decode an admitted layer's reduced ROI into caller-owned storage: Gray8 for
// grayscale and for a palette's index plane, RGBA bytes with opaque alpha for
// sRGB. The host applies a palette's color table. No row padding or scaling.
// region is a nonempty rectangle inside the reduced level, relative to its
// top-left pixel. The codestream is fresh: unrestricted, no decoder tiles open.
// Returns NULL on success; KDU and allocation exceptions propagate. Output is
// valid only on success. Uses JHV's display quality limiter, up to eight KDU
// threads, and direct byte output. No KDU JP2/JPX parsing or intermediate
// decoded planes are used.
const char *jhv_kdu_decode(kdu_core::kdu_codestream &stream, const hv_render &render,
                          const jhv_j2k_geometry &geometry, int reduce,
                          const jhv_j2k_level &region, uint8_t *output, size_t capacity);

#endif
