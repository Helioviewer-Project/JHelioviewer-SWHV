#include "jhv_kdu_decode.h"
#include <cstdio>
#include <cstring>
#include <mutex>
#include <utility>
using namespace kdu_core;

// Kakadu's view of one client input.
class kdu_input : public kdu_compressed_source {
    decoder_input &input;
    size_t position = 0;
public:
    explicit kdu_input(decoder_input &_input) : input(_input) {}
    int get_capabilities() override { return KDU_SOURCE_CAP_SEQUENTIAL | KDU_SOURCE_CAP_SEEKABLE; }
    int read(kdu_byte *out, int size) override {
        if (size < 0) throw std::runtime_error("negative codestream read");
        size_t count = input.read(position, out, static_cast<size_t>(size));
        position += count;
        return static_cast<int>(count);
    }
    bool seek(kdu_long offset) override {
        if (offset < 0 || static_cast<uint64_t>(offset) > input.size()) return false;
        position = static_cast<size_t>(offset);
        return true;
    }
    kdu_long get_pos() override { return static_cast<kdu_long>(position); }
};

// Kakadu's messages are process-wide: with concurrent decodes the text handed
// to a call is the latest, not necessarily its own.
static kdu_message_queue &errors() { static kdu_message_queue queue; return queue; }
static kdu_message_queue &warnings() { static kdu_message_queue queue; return queue; }
static void take(kdu_message_queue &queue, char *text, size_t capacity = 256) {
    static std::mutex popping;
    std::lock_guard<std::mutex> lock(popping);
    size_t used = std::strlen(text);
    while (const char *message = queue.pop_message()) {
        if (used < capacity - 1) std::snprintf(text + used, capacity - used, "%s%s", used ? " " : "", message);
        used = std::strlen(text);
    }
}

// Run use on a fresh codestream over input, destroying it on every path.
// Kakadu errors become std::runtime_error with their queued text.
template<class F> static auto with_stream(decoder_input &input, F use) -> decltype(use(std::declval<kdu_codestream &>())) {
    kdu_input source(input);
    kdu_codestream stream;
    try {
        stream.create(&source);
        auto result = use(stream);
        stream.destroy();
        return result;
    } catch (kdu_exception code) {
        if (stream.exists()) stream.destroy();
        char text[256] = "";
        take(errors(), text);
        if (!text[0]) std::snprintf(text, sizeof text, "JPEG 2000 decoder error (%d)", code);
        throw std::runtime_error(text);
    } catch (...) {
        if (stream.exists()) stream.destroy();
        throw;
    }
}

void jhv_decoder_init() {
    errors().configure(32, false, true);
    warnings().configure(32, false, false);
    kdu_customize_errors(&errors());
    kdu_customize_warnings(&warnings());
}

void jhv_decoder_describe(decoder_input &input, hvc *client, size_t frame,
                          hv_render &render, jhv_j2k_geometry &out) {
    with_stream(input, [&](kdu_codestream &stream) {
        // Geometry can retry with the first component after creating tiles.
        stream.set_persistent();
        int coded_components = stream.get_num_components(false);
        jhv_decoder_select(client, frame, static_cast<size_t>(stream.get_num_components(true)),
            [&](const hv_render &selected, jhv_j2k_geometry &geometry) {
                return jhv_kdu_read_geometry(stream, selected, geometry);
            }, render, out);
        out.coded_components = coded_components;
        return 0;
    });
}

const char *jhv_decoder_decode(decoder_input &input, const hv_render &render, const jhv_j2k_geometry &geometry,
                               int reduce, const jhv_j2k_level &region, uint8_t *output, size_t) {
    return with_stream(input, [&](kdu_codestream &stream) {
        return jhv_kdu_decode(stream, render, geometry, reduce, region, output);
    });
}

void jhv_decoder_warnings(char *text) { take(warnings(), text); }
