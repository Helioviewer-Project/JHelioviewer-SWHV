#ifndef JHV_J2K_SOURCE_H
#define JHV_J2K_SOURCE_H
#include <climits>
#include <new>
#include <stdexcept>
#include <vector>
#include "jhv_j2k_decoder.h"

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

    static int inspect(hvc *source, size_t frame, void *context,
                       hvc_info *info, char *error, size_t error_size) noexcept;
    const description &describe(size_t frame);

    [[noreturn]] void fail() { throw std::runtime_error(hvc_error(client)); }

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
    // Immutable and stable once the client has inspected the frame's geometry.
    const description &described(int frame) const { return descriptions.at(frame); }
};

#endif
