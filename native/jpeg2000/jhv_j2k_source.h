#ifndef JHV_J2K_SOURCE_H
#define JHV_J2K_SOURCE_H
#include <climits>
#include <cstdio>
#include <exception>
#include <stdexcept>
#include <vector>
#include "jhv_kdu_decode.h"
#include "client/hvc_decode.h"
using namespace kdu_core;

// Kakadu's view of one immutable client input.
class decoder_input : public kdu_compressed_source {
    hvc_input *input;
    size_t length, position = 0;
public:
    decoder_input(hvc *_client, size_t _frame, int _reduce) : input(hvc_input_open(_client, _frame, _reduce)) {
        if (!input) throw std::runtime_error(hvc_error(_client));
        length = hvc_input_size(input);
    }
    ~decoder_input() { hvc_input_close(input); }
    decoder_input(const decoder_input &) = delete;
    decoder_input &operator=(const decoder_input &) = delete;
    int get_capabilities() override { return KDU_SOURCE_CAP_SEQUENTIAL | KDU_SOURCE_CAP_SEEKABLE; }
    int read(kdu_byte *out, int size) override {
        if (size < 0) throw std::runtime_error("negative codestream read");
        size_t count = hvc_input_read(input, position, out, static_cast<size_t>(size));
        if (count == SIZE_MAX) throw std::runtime_error("codestream read failed");
        position += count;
        return static_cast<int>(count);
    }
    bool seek(kdu_long offset) override {
        if (offset < 0 || static_cast<uint64_t>(offset) > length) return false;
        position = static_cast<size_t>(offset);
        return true;
    }
    kdu_long get_pos() override { return static_cast<kdu_long>(position); }
    // Run use on a fresh codestream over this input, destroying it on every path.
    template<class F> const char *with_stream(F use) {
        kdu_codestream stream;
        try {
            seek(0);
            stream.create(this);
            const char *result = use(stream);
            stream.destroy();
            return result;
        } catch (...) {
            if (stream.exists()) stream.destroy();
            throw;
        }
    }
};

// One client and the decoder's description of its frames. The host serializes calls.
class jhv_j2k_source {
public:
    struct description {
        hv_render render;
        jhv_j2k_geometry geometry;
    };
private:
    hvc *client = nullptr;
    std::vector<description> descriptions;
    std::exception_ptr inspection_failure;

    // What JHV shows of a file whose color description is not supported.
    static void first_component(hv_render &render) {
        render = hv_render();
        render.colour_space = 17;
        render.channel_count = 1;
        render.channel[0].palette_column = -1;
    }

    static int inspect(hvc *source, size_t frame, void *context,
                       hvc_info *info, char *error, size_t error_size) noexcept {
        jhv_j2k_source &owner = *static_cast<jhv_j2k_source *>(context);
        try {
            size_t count = hvc_frames(source);
            if (count > INT_MAX) throw std::runtime_error("too many JPEG 2000 frames");
            owner.descriptions.resize(count);
            // The coarsest level suffices: every header is kept at any reduction.
            decoder_input input(source, frame, INT_MAX);
            description result = description();
            const char *reason = input.with_stream([&](kdu_codestream &stream) {
                if (hvc_render_read(source, frame, stream.get_num_components(true), &result.render))
                    first_component(result.render);
                return jhv_kdu_read_geometry(stream, result.render, result.geometry);
            });
            if (reason && result.render.channel_count > 1) {
                first_component(result.render);
                reason = input.with_stream([&](kdu_codestream &stream) {
                    return jhv_kdu_read_geometry(stream, result.render, result.geometry);
                });
            }
            if (reason) throw std::runtime_error(reason);
            info->components = result.geometry.plane_count;
            info->resolutions = result.geometry.resolutions;
            info->layers = result.geometry.layers;
            for (int r = 0; r < info->resolutions; r++) {
                info->width[r] = result.geometry.level[r].width;
                info->height[r] = result.geometry.level[r].height;
            }
            owner.descriptions[frame] = result;
            return 0;
        } catch (...) {
            // Keep the typed failure; nothing may unwind through the C client.
            owner.inspection_failure = std::current_exception();
            std::snprintf(error, error_size, "decoder inspection failed");
            return -1;
        }
    }

    [[noreturn]] void fail() {
        if (inspection_failure) {
            std::exception_ptr failure = inspection_failure;
            inspection_failure = nullptr;
            std::rethrow_exception(failure);
        }
        throw std::runtime_error(hvc_error(client));
    }

public:
    explicit jhv_j2k_source(const char *path) {
        char error[256];
        client = hvc_open_local(path, inspect, this, error, sizeof error);
        if (!client) throw std::runtime_error(error);
    }
    jhv_j2k_source() : client(hvc_create(inspect, this)) {
        if (!client) throw std::bad_alloc();
    }
    ~jhv_j2k_source() { hvc_destroy(client); }
    jhv_j2k_source(const jhv_j2k_source &) = delete;
    jhv_j2k_source &operator=(const jhv_j2k_source &) = delete;
    hvc *raw() { return client; }

    int checked(int result) {
        if (result < 0) fail();
        return result;
    }
    int frames() {
        size_t count = hvc_frames(client);
        if (!count) fail();
        if (count > INT_MAX) throw std::runtime_error("too many JPEG 2000 frames");
        return static_cast<int>(count);
    }
    // Readiness at a level; zero geometry while the frame's header is missing.
    hvc_view status(int frame, int level) {
        if (frame < 0 || level < 0) throw std::runtime_error("JPEG 2000 frame or level out of range");
        hvc_options options = {level, 0, 0, 0};
        hvc_view view;
        if (hvc_status(client, static_cast<uint64_t>(frame), &options, &view)) fail();
        return view;
    }
    // Valid once status has reported the frame's geometry.
    const description &described(int frame) const { return descriptions.at(frame); }
};

#endif
