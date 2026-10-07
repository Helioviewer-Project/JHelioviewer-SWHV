#include "jhv_kdu_decode.h"
#include "kdu_region_decompressor.h"
#include <algorithm>
#include <climits>
#include <new>

const char *jhv_kdu_decode(kdu_core::kdu_codestream &stream, const hv_render &render,
                          const jhv_j2k_geometry &geometry, int reduce,
                          const jhv_j2k_level &region, uint8_t *output, size_t capacity) {
    using namespace kdu_core;
    using namespace kdu_supp;
    if (reduce < 0 || reduce >= geometry.resolutions)
        return "JPEG 2000 reduction out of range";
    const jhv_j2k_level &level = geometry.level[reduce];
    if (region.x < 0 || region.y < 0 || region.width <= 0 || region.height <= 0 ||
        static_cast<int64_t>(region.x) + region.width > level.width ||
        static_cast<int64_t>(region.y) + region.height > level.height)
        return "JPEG 2000 region out of range";
    int64_t x = static_cast<int64_t>(level.x) + region.x;
    int64_t y = static_cast<int64_t>(level.y) + region.y;
    if (x < INT_MIN || y < INT_MIN || x + region.width > INT_MAX ||
        y + region.height > INT_MAX)
        return "JPEG 2000 region exceeds decoder coordinate range";
    size_t bytes = geometry.channels == 1 ? 1 : 4;
    uint64_t required = static_cast<uint64_t>(region.width) * region.height * bytes;
    if (!output || required > capacity) return "JPEG 2000 output buffer too small";
    kdu_channel_mapping mapping;
    // A palette's channels all name its index component: decode that plane once.
    mapping.set_num_channels(geometry.channels);
    mapping.num_colour_channels = mapping.num_channels;
    for (int c = 0; c < geometry.channels; c++) {
        int component = static_cast<int>(render.channel[c].component);
        int bits = stream.get_bit_depth(component, true);
        bool is_signed = stream.get_signed(component, true);
        if (bits < 1 || bits > 32) return "unsupported decoder sample precision";
        mapping.source_components[c] = component;
        mapping.default_rendering_precision[c] = bits;
        mapping.default_rendering_signed[c] = is_signed;
        mapping.channel_interp[c].init(bits, is_signed, 0.0f);
    }
    kdu_dims requested;
    requested.pos = kdu_coords(static_cast<int>(x), static_cast<int>(y));
    requested.size = kdu_coords(region.width, region.height);
    // Per-decode ownership keeps KDU thread affinity and failure cleanup local.
    kdu_thread_env environment;
    kdu_region_decompressor decoder;
    kdu_quality_limiter limiter(1.0f / 256);
    decoder.set_quality_limiting(&limiter, -1, -1);
    decoder.set_white_stretch(8);
    bool complete = false, finished = false;
    kdu_exception failure = 0;
    try {
        environment.create();
        int threads = std::min(8, kdu_get_num_processors());
        for (int t = 1; t < threads; t++) environment.add_thread();
        bool started = decoder.start(stream, &mapping, -1, reduce, geometry.layers,
                                     requested, kdu_coords(1, 1), kdu_coords(1, 1), false,
                                     KDU_WANT_OUTPUT_COMPONENTS, true, &environment);
        kdu_dims incomplete = requested, updated;
        int offsets[] = {0,1,2,3};
        if (started) {
            while (!incomplete.is_empty())
                if (!decoder.process(output, offsets, static_cast<int>(bytes), requested.pos,
                                     region.width, 256 * 1024, 0, incomplete, updated, 8, true,
                                     0, bytes == 4 ? 1 : 0, geometry.channels)) break;
        }
        complete = started && incomplete.is_empty();
        finished = decoder.finish(&failure);
        decoder.reset();
        environment.destroy();
    } catch (...) {
        if (environment.exists()) environment.handle_exception(KDU_ERROR_EXCEPTION);
        decoder.finish();
        decoder.reset();
        environment.destroy();
        throw;
    }
    if (!finished) {
        if (failure == KDU_MEMORY_EXCEPTION) throw std::bad_alloc();
        throw failure;
    }
    return complete ? NULL : "JPEG 2000 decode incomplete";
}
