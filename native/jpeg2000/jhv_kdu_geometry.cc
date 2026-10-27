#include "jhv_kdu_geometry.h"
#include <cstdint>

const char *jhv_kdu_read_geometry(kdu_core::kdu_codestream &stream,
                                  const hv_render &render, jhv_j2k_geometry &out) {
    using namespace kdu_core;
    jhv_j2k_geometry value = jhv_j2k_geometry();
    int components[3] = {};
    if (const char *reason = jhv_decoder_layout(render, static_cast<size_t>(stream.get_num_components(true)), components, value))
        return reason;
    bool indexed = render.channel[0].palette_column >= 0;
    if (indexed && (stream.get_signed(components[0], true) || stream.get_bit_depth(components[0], true) > 8))
        return "palette indices require one unsigned component of at most 8 bits";

    stream.apply_input_restrictions(value.plane_count, components, 0, 0,
                                    NULL, KDU_WANT_OUTPUT_COMPONENTS);
    kdu_dims tiles;
    stream.get_valid_tiles(tiles);
    for (int y = 0; y < tiles.size.y; y++)
        for (int x = 0; x < tiles.size.x; x++)
            stream.create_tile(kdu_coords(tiles.pos.x + x, tiles.pos.y + y));
    value.resolutions = stream.get_min_dwt_levels() + 1;
    value.layers = stream.get_max_tile_layers();
    // KDU's signed canvas coordinates cannot represent a sampling step of 2^31.
    if (value.resolutions > 31)
        return "unsupported decoder sampling range";

    for (int r = 0; r < value.resolutions; r++) {
        stream.apply_input_restrictions(value.plane_count, components, r, 0,
                                        NULL, KDU_WANT_OUTPUT_COMPONENTS);
        kdu_dims first;
        for (int plane = 0; plane < value.plane_count; plane++) {
            kdu_dims dims;
            kdu_coords sampling, registration;
            stream.get_dims(plane, dims, true);
            stream.get_subsampling(plane, sampling, true);
            stream.get_registration(plane, kdu_coords(65536, 65536), registration, true);
            if (sampling.x != (INT64_C(1) << r) || sampling.y != (INT64_C(1) << r) ||
                registration.x || registration.y)
                return "selected output components require resampling";
            if (plane == 0)
                first = dims;
            else if (dims.pos != first.pos || dims.size != first.size)
                return "selected output components require resampling";
        }
        value.level[r] = {first.pos.x, first.pos.y, first.size.x, first.size.y};
    }
    out = value;
    return NULL;
}
