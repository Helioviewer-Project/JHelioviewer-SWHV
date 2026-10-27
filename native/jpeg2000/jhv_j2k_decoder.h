#ifndef JHV_J2K_DECODER_H
#define JHV_J2K_DECODER_H
#include <cstdint>
#include <stdexcept>
#include "client/hvc_decode.h"

struct jhv_j2k_level {
    int x, y, width, height;
};

struct jhv_j2k_geometry {
    int coded_components;      // SIZ count before transforms/channel selection
    int plane_count;
    int channels;               // decoded output: 1 gray or palette indices, 3 color
    int resolutions, layers;
    jhv_j2k_level level[33];    // exact decoder coordinates, full resolution first
};

// One immutable client input, a raw codestream.
class decoder_input {
    hvc_input *input;
    size_t length;
public:
    decoder_input(hvc *_client, size_t _frame, int _reduce) : input(hvc_input_open(_client, _frame, _reduce)) {
        if (!input) throw std::runtime_error(hvc_error(_client));
        length = hvc_input_size(input);
    }
    ~decoder_input() { hvc_input_close(input); }
    decoder_input(const decoder_input &) = delete;
    decoder_input &operator=(const decoder_input &) = delete;
    size_t size() const { return length; }
    // NULL for local files.
    const uint8_t *data() const { return hvc_input_data(input); }
    // At most size bytes from offset, 0 at the end.
    size_t read(size_t offset, uint8_t *out, size_t size) const {
        char error[256];
        size_t count = hvc_input_read(input, offset, out, size, error, sizeof error);
        if (count == SIZE_MAX) throw std::runtime_error(error);
        return count;
    }
};

// The decoder of this build, Kakadu or OpenJPEG. Failures throw std::exception.
// jhv_decoder_init runs once before any other call.
void jhv_decoder_init();
// Select rendering and geometry using one parsed header. A palette's channels
// share one unsigned index plane of at most 8 bits. Unsupported color layouts
// fall back to the first component, as for an unsupported color description.
void jhv_decoder_describe(decoder_input &input, hvc *client, size_t frame,
                          hv_render &render, jhv_j2k_geometry &out);

// Shared display layout; sampling, precision and geometry stay in each backend.
inline const char *jhv_decoder_layout(const hv_render &render, size_t output_components,
                                      int components[3], jhv_j2k_geometry &value) {
    bool indexed = render.channel[0].palette_column >= 0;
    for (size_t c = 0; c < render.channel_count; c++) {
        if ((render.channel[c].palette_column >= 0) != indexed)
            return "unsupported mix of palette and direct channels";
        if (render.channel[c].component >= output_components)
            return "selected component does not exist";
        int component = static_cast<int>(render.channel[c].component);
        int plane = 0;
        while (plane < value.plane_count && components[plane] != component) plane++;
        if (plane == value.plane_count) components[value.plane_count++] = component;
    }
    if (indexed && value.plane_count != 1)
        return "unsupported palette on several components";
    value.channels = indexed ? 1 : static_cast<int>(render.channel_count);
    return NULL;
}

// Shared selection policy for either backend's already-open header.
// read_geometry returns NULL, or a rendering limitation with out unchanged.
// It must support retrying with the first component on the same header.
template<class F> void jhv_decoder_select(hvc *client, size_t frame, size_t components,
                                         F read_geometry, hv_render &render, jhv_j2k_geometry &out) {
    auto first_component = [&] {
        render = hv_render();
        render.colour_space = 17;
        render.channel_count = 1;
        render.channel[0].palette_column = -1;
    };
    if (hvc_render_read(client, frame, components, &render)) first_component();
    const char *reason = read_geometry(render, out);
    if (reason && render.channel_count > 1 && render.channel[0].palette_column < 0) {
        first_component();
        reason = read_geometry(render, out);
    }
    if (reason) throw std::runtime_error(reason);
}
// Gray8 rows for grayscale and palette indices, RGBA rows with opaque alpha
// for sRGB, of a nonempty region of level reduce relative to its top-left
// pixel. The common native boundary validates reduction, region and capacity
// before calling. Returns NULL, or why the output is incomplete.
const char *jhv_decoder_decode(decoder_input &input, const hv_render &render, const jhv_j2k_geometry &geometry,
                               int reduce, const jhv_j2k_level &region, uint8_t *output, size_t capacity);
// Append pending decoder warnings to a 256-byte text.
void jhv_decoder_warnings(char *text);

#endif
