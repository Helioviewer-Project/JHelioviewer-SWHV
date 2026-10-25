#ifndef JHV_J2K_DECODER_H
#define JHV_J2K_DECODER_H
#include <cstdint>
#include <stdexcept>
#include "client/hvc_decode.h"

struct jhv_j2k_level {
    int x, y, width, height;
};

struct jhv_j2k_geometry {
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
// The codestream's output component count, after its transforms.
size_t jhv_decoder_components(decoder_input &input);
// Geometry of the planes that render selects. render must describe the
// codestream's supported grayscale/RGB channels; a palette's channels share
// one unsigned index plane of at most 8 bits. Selected planes must not need
// resampling. Returns NULL, or a rendering limitation with out unchanged.
const char *jhv_decoder_geometry(decoder_input &input, const hv_render &render, jhv_j2k_geometry &out);
// Gray8 rows for grayscale and palette indices, RGBA rows with opaque alpha
// for sRGB, of a nonempty region of level reduce relative to its top-left
// pixel. Returns NULL, or why the output is incomplete.
const char *jhv_decoder_decode(decoder_input &input, const hv_render &render, const jhv_j2k_geometry &geometry,
                               int reduce, const jhv_j2k_level &region, uint8_t *output, size_t capacity);
// Append pending decoder warnings to a 256-byte text.
void jhv_decoder_warnings(char *text);

#endif
