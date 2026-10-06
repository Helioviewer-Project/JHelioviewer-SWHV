#include "jhv_kdu_decode.h"
#include "kdu_region_decompressor.h"
#include <algorithm>
#include <new>

const char *jhv_kdu_decode(kdu_core::kdu_codestream &stream, const hv_render &render,
                          const jhv_j2k_geometry &geometry, int reduce, int layers,
                          const jhv_j2k_level &region, uint8_t *output, size_t capacity) {
    using namespace kdu_core;
    using namespace kdu_supp;
    size_t bytes = geometry.channels == 1 ? 1 : 4;
    if (region.width <= 0 || region.height <= 0) return "JPEG 2000 region out of range";
    uint64_t required = static_cast<uint64_t>(region.width) * region.height * bytes;
    if (!output || required > capacity) return "JPEG 2000 output buffer too small";
    const char *reason = jhv_kdu_set_region(stream, geometry, reduce, layers, region);
    if (reason) return reason;
    stream.apply_input_restrictions(0, 0, 0, 0, NULL, KDU_WANT_OUTPUT_COMPONENTS);
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
    requested.pos = kdu_coords(geometry.level[reduce].x + region.x, geometry.level[reduce].y + region.y);
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
        bool started = decoder.start(stream, &mapping, -1, reduce, layers ? layers : geometry.layers,
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
