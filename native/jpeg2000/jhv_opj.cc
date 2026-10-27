#include "jhv_j2k_decoder.h"
#include "hvc_openjpeg.h"
#include "decoder_header.h"
#include "openjpeg.h"
#include "jpeg2000/hv_reader.h"
#include <algorithm>
#include <climits>
#include <mutex>
#include <new>
#include <string>
#include <thread>
#include <vector>

// OpenJPEG's view of one client input, for header reads.
struct opj_input {
    decoder_input &input;
    size_t position;
};

static OPJ_SIZE_T read_input(void *out, OPJ_SIZE_T size, void *user) {
    opj_input *in = static_cast<opj_input *>(user);
    size_t count = 0;
    try { count = in->input.read(in->position, static_cast<uint8_t *>(out), size); }
    catch (...) { return static_cast<OPJ_SIZE_T>(-1); }
    if (!count) return static_cast<OPJ_SIZE_T>(-1);
    in->position += count;
    return count;
}

static OPJ_OFF_T skip_input(OPJ_OFF_T count, void *user) {
    opj_input *in = static_cast<opj_input *>(user);
    if (count < 0) return -1;
    count = std::min(count, static_cast<OPJ_OFF_T>(in->input.size() - in->position));
    in->position += static_cast<size_t>(count);
    return count;
}

static OPJ_BOOL seek_input(OPJ_OFF_T position, void *user) {
    opj_input *in = static_cast<opj_input *>(user);
    if (position < 0 || static_cast<uint64_t>(position) > in->input.size()) return OPJ_FALSE;
    in->position = static_cast<size_t>(position);
    return OPJ_TRUE;
}

static uint32_t ceil_shift(uint32_t value, int r) {
    return static_cast<uint32_t>((static_cast<uint64_t>(value) + (UINT64_C(1) << r) - 1) >> r);
}

static void keep_error(const char *text, void *user) {
    std::string &error = *static_cast<std::string *>(user);
    error = text;
    while (!error.empty() && error.back() == '\n') error.pop_back();
}

static void read_header_bytes(decoder_input &input, size_t at, size_t end, uint8_t *bytes, size_t size) {
    if (at > end || size > end - at)
        throw std::runtime_error("truncated JPEG 2000 tile header");
    while (size) {
        size_t count = input.read(at, bytes, size);
        if (!count) throw std::runtime_error("truncated JPEG 2000 tile header");
        at += count;
        bytes += count;
        size -= count;
    }
}

static unsigned header_word(const uint8_t *bytes) {
    return (static_cast<unsigned>(bytes[0]) << 8) | bytes[1];
}

static unsigned header_marker(decoder_input &input, size_t at, size_t end, size_t &next) {
    if (at > end) throw std::runtime_error("truncated JPEG 2000 tile header");
    uint8_t bytes[4];
    read_header_bytes(input, at, end, bytes, std::min<size_t>(sizeof bytes, end - at));
    hv_marker marker;
    if (const char *error = hv_marker_read(bytes, 0, end - at, &marker))
        throw std::runtime_error(error);
    next = at + marker.end;
    return marker.code;
}

// OpenJPEG checks the reduction while reading every tile COD/COC, before a
// later override can replace it. Bound it by all such markers, not only the
// final effective coding. Read only tile-part headers; Psot skips packet data.
static unsigned local_resolutions(decoder_input &input, unsigned count, unsigned components) {
    size_t at = 2, next;
    unsigned code;
    while ((code = header_marker(input, at, input.size(), next)) != HV_SOT) {
        if (code == HV_SOD || code == HV_EOC)
            throw std::runtime_error("missing JPEG 2000 tile header");
        at = next;
    }
    for (;;) {
        uint8_t sot[8];
        if (next - at != 12) throw std::runtime_error("invalid JPEG 2000 SOT length");
        read_header_bytes(input, at + 4, next, sot, sizeof sot);
        uint32_t length = (static_cast<uint32_t>(header_word(sot + 2)) << 16) | header_word(sot + 4);
        if (length && ((length < 14 && length != 12) || length > input.size() - at))
            throw std::runtime_error("invalid JPEG 2000 tile-part length");
        size_t end = length ? at + length : input.size();
        at = next;
        if (length != 12) {
            while ((code = header_marker(input, at, end, next)) != HV_SOD) {
                if (code == HV_SOT || code == HV_EOC)
                    throw std::runtime_error("JPEG 2000 tile header without SOD");
                if (code == HV_COD) {
                    uint8_t levels;
                    if (next - at < 14) throw std::runtime_error("invalid JPEG 2000 COD length");
                    read_header_bytes(input, at + 9, next, &levels, 1);
                    if (levels >= OPJ_J2K_MAXRLVLS) throw std::runtime_error("invalid tile resolution count");
                    count = std::min(count, static_cast<unsigned>(levels) + 1);
                } else if (code == HV_COC) {
                    unsigned index_bytes = components <= 256 ? 1 : 2;
                    uint8_t bytes[4];
                    if (next - at < 10 + index_bytes) throw std::runtime_error("invalid JPEG 2000 COC length");
                    read_header_bytes(input, at + 4, next, bytes, index_bytes + 2);
                    unsigned component = index_bytes == 1 ? bytes[0] : header_word(bytes);
                    unsigned levels = bytes[index_bytes + 1];
                    if (component >= components || levels >= OPJ_J2K_MAXRLVLS)
                        throw std::runtime_error("invalid JPEG 2000 component coding");
                    count = std::min(count, levels + 1);
                }
                at = next;
            }
        }
        if (!length || end == input.size()) return count; // Final part, including OpenJPEG's tolerated EOF.
        at = end;
        code = header_marker(input, at, input.size(), next);
        if (code == HV_EOC) return count;
        if (code != HV_SOT) throw std::runtime_error("expected JPEG 2000 tile-part header");
    }
}

// The main header of a codestream: its image and coding parameters.
class header {
    opj_input source;
    opj_stream_t *stream = nullptr;
    opj_codec_t *codec = nullptr;
    std::string error;
public:
    opj_image_t *image = nullptr;
    unsigned resolution_limit = OPJ_J2K_MAXRLVLS;
    unsigned layers = 0;

    explicit header(decoder_input &input) : source{input, 0} {
        stream = opj_stream_create(1 << 16, OPJ_TRUE);
        codec = opj_create_decompress(OPJ_CODEC_J2K);
        if (!stream || !codec) {
            release();
            throw std::bad_alloc();
        }
        opj_stream_set_read_function(stream, read_input);
        opj_stream_set_skip_function(stream, skip_input);
        opj_stream_set_seek_function(stream, seek_input);
        opj_stream_set_user_data(stream, &source, nullptr);
        opj_stream_set_user_data_length(stream, input.size());
        opj_set_error_handler(codec, keep_error, &error);
        opj_dparameters_t parameters;
        opj_set_default_decoder_parameters(&parameters);
        if (!opj_setup_decoder(codec, &parameters) || !opj_read_header(stream, codec, &image)) {
            std::string reason = error.empty() ? "cannot read the JPEG 2000 codestream header" : error;
            release();
            throw std::runtime_error(reason);
        }
        try {
            // OpenJPEG's reduction setter checks every main-header component.
            for (OPJ_UINT32 c = 0; c < image->numcomps; c++) {
                OPJ_UINT32 resolutions;
                if (!opj_decoder_get_coding_parameters(codec, c, &resolutions, &layers))
                    throw std::runtime_error("cannot read JPEG 2000 coding parameters");
                resolution_limit = std::min(resolution_limit, resolutions);
            }
            if (!input.data()) resolution_limit = local_resolutions(input, resolution_limit, image->numcomps);
        } catch (...) {
            release();
            throw;
        }
    }
    ~header() { release(); }
    header(const header &) = delete;
    header &operator=(const header &) = delete;
private:
    void release() {
        opj_image_destroy(image);
        if (codec) opj_destroy_codec(codec);
        if (stream) opj_stream_destroy(stream);
        image = nullptr;
        codec = nullptr;
        stream = nullptr;
    }
};

// Idle decoder workspaces, reused across decodes; at most one per concurrent decode.
static std::mutex idle_lock;
static std::vector<hvc_decoder *> idle;

class lease {
    hvc_decoder *held = nullptr;
public:
    lease() {
        {
            std::lock_guard<std::mutex> lock(idle_lock);
            if (!idle.empty()) {
                held = idle.back();
                idle.pop_back();
                return;
            }
        }
        // The decoding thread waits for its workers; limit each decode to eight.
        int threads = static_cast<int>(std::min(8u, std::thread::hardware_concurrency()));
        held = hvc_decoder_create(threads);
        if (!held) held = hvc_decoder_create(0);
        if (!held) throw std::bad_alloc();
    }
    ~lease() {
        try {
            std::lock_guard<std::mutex> lock(idle_lock);
            idle.push_back(held);
        } catch (...) {
            hvc_decoder_destroy(held);
        }
    }
    lease(const lease &) = delete;
    lease &operator=(const lease &) = delete;
    hvc_decoder *get() const { return held; }
};

void jhv_decoder_init() {}

static const char *read_geometry(const header &h, const hv_render &render, jhv_j2k_geometry &out) {
    jhv_j2k_geometry value = jhv_j2k_geometry();
    // Original decoder output indices, in decode order.
    OPJ_UINT32 components[3] = {};
    for (size_t c = 0; c < render.channel_count; c++) {
        OPJ_UINT32 component = static_cast<OPJ_UINT32>(render.channel[c].component);
        if (component >= h.image->numcomps) return "selected component does not exist";
        int plane = 0;
        while (plane < value.plane_count && components[plane] != component)
            plane++;
        if (plane == value.plane_count)
            components[value.plane_count++] = component;
    }
    // The host applies a palette: its channels share one decoded index plane.
    bool indexed = render.channel[0].palette_column >= 0;
    for (size_t c = 1; c < render.channel_count; c++)
        if ((render.channel[c].palette_column >= 0) != indexed)
            return "unsupported mix of palette and direct channels";
    if (indexed && value.plane_count != 1)
        return "unsupported palette on several components";
    const opj_image_comp_t &first = h.image->comps[components[0]];
    if (indexed && (first.sgnd || first.prec > 8))
        return "palette indices require one unsigned component of at most 8 bits";
    value.channels = indexed ? 1 : static_cast<int>(render.channel_count);

    // Include local tile overrides and the decoder's component-wide limit.
    value.resolutions = static_cast<int>(h.resolution_limit);
    value.layers = static_cast<int>(h.layers);
    for (int plane = 0; plane < value.plane_count; plane++) {
        const opj_image_comp_t &component = h.image->comps[components[plane]];
        if (component.dx != 1 || component.dy != 1)
            return "selected output components require resampling";
        if (component.prec < 1 || component.prec > 31)
            return "unsupported decoder sample precision";
    }
    if (value.resolutions > 31)
        return "unsupported decoder sampling range";
    // Unit sampling: every plane covers the image area.
    for (int r = 0; r < value.resolutions; r++) {
        uint32_t x0 = ceil_shift(h.image->x0, r), y0 = ceil_shift(h.image->y0, r);
        uint32_t x1 = ceil_shift(h.image->x1, r), y1 = ceil_shift(h.image->y1, r);
        if (x1 > INT_MAX || y1 > INT_MAX)
            return "JPEG 2000 image exceeds decoder coordinate range";
        value.level[r] = {static_cast<int>(x0), static_cast<int>(y0),
                          static_cast<int>(x1 - x0), static_cast<int>(y1 - y0)};
    }
    out = value;
    return NULL;
}

void jhv_decoder_describe(decoder_input &input, hvc *client, size_t frame,
                          hv_render &render, jhv_j2k_geometry &out) {
    header h(input);
    jhv_decoder_select(client, frame, h.image->numcomps,
        [&](const hv_render &selected, jhv_j2k_geometry &geometry) {
            return read_geometry(h, selected, geometry);
        }, render, out);
}

const char *jhv_decoder_decode(decoder_input &input, const hv_render &render, const jhv_j2k_geometry &geometry,
                               int reduce, const jhv_j2k_level &region, uint8_t *output, size_t capacity) {
    if (reduce < 0 || reduce >= geometry.resolutions)
        return "JPEG 2000 reduction out of range";
    const jhv_j2k_level &level = geometry.level[reduce], &full = geometry.level[0];
    if (region.x < 0 || region.y < 0 || region.width <= 0 || region.height <= 0 ||
        static_cast<int64_t>(region.x) + region.width > level.width ||
        static_cast<int64_t>(region.y) + region.height > level.height)
        return "JPEG 2000 region out of range";
    size_t bytes = geometry.channels == 1 ? 1 : 4;
    uint64_t required = static_cast<uint64_t>(region.width) * region.height * bytes;
    if (!output || required > capacity) return "JPEG 2000 output buffer too small";

    // The full-resolution area whose reduction is the region, relative to the image.
    uint64_t x0 = static_cast<uint64_t>(level.x) + region.x, x1 = x0 + region.width;
    uint64_t y0 = static_cast<uint64_t>(level.y) + region.y, y1 = y0 + region.height;
    uint64_t right = static_cast<uint64_t>(full.x) + full.width, bottom = static_cast<uint64_t>(full.y) + full.height;
    uint32_t area[4] = {
        static_cast<uint32_t>(std::max<uint64_t>(x0 << reduce, full.x) - full.x),
        static_cast<uint32_t>(std::max<uint64_t>(y0 << reduce, full.y) - full.y),
        static_cast<uint32_t>(std::min<uint64_t>(x1 << reduce, right) - full.x),
        static_cast<uint32_t>(std::min<uint64_t>(y1 << reduce, bottom) - full.y)};

    lease decoder;
    const uint8_t *data = input.data();
    size_t size = input.size();
    if (!data) {
        uint8_t *copy = static_cast<uint8_t *>(hvc_decoder_input(decoder.get(), size));
        if (!copy && size) throw std::bad_alloc();
        for (size_t at = 0; at < size;) {
            size_t count = input.read(at, copy + at, size - at);
            if (!count) throw std::runtime_error("JPEG 2000 codestream ended early");
            at += count;
        }
        data = copy;
    }
    char error[256];
    hvc_image image;
    // OpenJPEG decodes a whole level faster without a decode area.
    bool whole = region.width == level.width && region.height == level.height;
    hvc_decoder_output(decoder.get(), output, capacity, bytes == 4);
    if (whole ? hvc_openjpeg_decode_context(decoder.get(), data, size, reduce, &render, &image, error, sizeof error)
              : hvc_openjpeg_decode_region(decoder.get(), data, size, reduce, &render, area, &image, error, sizeof error))
        throw std::runtime_error(error);
    if (image.width != static_cast<uint32_t>(region.width) || image.height != static_cast<uint32_t>(region.height) ||
        image.components != (bytes == 1 ? 1 : 3))
        return "JPEG 2000 decode does not match the region";
    return NULL;
}

void jhv_decoder_warnings(char *) {}
