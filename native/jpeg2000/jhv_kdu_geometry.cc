#include "jhv_kdu_geometry.h"
#include <cstdint>
#include <climits>

const char *jhv_kdu_read_geometry(kdu_core::kdu_codestream &stream,
                                  const hv_render &render, jhv_j2k_geometry &out) {
    using namespace kdu_core;
    jhv_j2k_geometry value = {};
    for (size_t c = 0; c < render.channel_count; c++) {
        int component = static_cast<int>(render.channel[c].component);
        int plane = 0;
        while (plane < value.plane_count && value.component[plane] != component)
            plane++;
        if (plane == value.plane_count)
            value.component[value.plane_count++] = component;
        value.channel_plane[c] = plane;
    }
    // The host applies a palette: its channels share one decoded index plane.
    bool indexed = render.channel[0].palette_column >= 0;
    for (size_t c = 1; c < render.channel_count; c++)
        if ((render.channel[c].palette_column >= 0) != indexed)
            return "unsupported mix of palette and direct channels";
    if (indexed && value.plane_count != 1)
        return "unsupported palette on several components";
    value.channels = indexed ? 1 : static_cast<int>(render.channel_count);

    stream.set_persistent();
    stream.apply_input_restrictions(value.plane_count, value.component, 0, 0,
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
        stream.apply_input_restrictions(value.plane_count, value.component, r, 0,
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
    stream.apply_input_restrictions(value.plane_count, value.component, 0, 0,
                                    NULL, KDU_WANT_OUTPUT_COMPONENTS);
    out = value;
    return NULL;
}

const char *jhv_kdu_set_region(kdu_core::kdu_codestream &stream,
                              const jhv_j2k_geometry &geometry, int reduce, int layers,
                              const jhv_j2k_level &region) {
    using namespace kdu_core;
    if (reduce < 0 || reduce >= geometry.resolutions)
        return "JPEG 2000 reduction out of range";
    if (layers < 0 || layers > geometry.layers)
        return "JPEG 2000 quality limit out of range";
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
    int components[3];
    for (int p = 0; p < geometry.plane_count; p++) components[p] = geometry.component[p];
    stream.apply_input_restrictions(geometry.plane_count, components, reduce,
                                    layers, NULL, KDU_WANT_OUTPUT_COMPONENTS);
    kdu_dims component_region, canvas_region;
    component_region.pos = kdu_coords(static_cast<int>(x), static_cast<int>(y));
    component_region.size = kdu_coords(region.width, region.height);
    stream.map_region(0, component_region, canvas_region, true);
    stream.apply_input_restrictions(geometry.plane_count, components, reduce,
                                    layers, &canvas_region, KDU_WANT_OUTPUT_COMPONENTS);
    return NULL;
}
