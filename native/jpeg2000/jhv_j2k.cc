#include "jhv_j2k.h"
#include "jhv_j2k_source.h"
#include <cstring>
#include <memory>
#include <mutex>

static_assert(sizeof(size_t) == sizeof(uint64_t), "JHV supports 64-bit native hosts");
static_assert(sizeof(jhv_j2k_frame_info) == 288 && offsetof(jhv_j2k_frame_info, width) == 20 &&
              offsetof(jhv_j2k_frame_info, height) == 152, "FFM frame layout");

struct jhv_j2k { std::shared_ptr<jhv_j2k_source> source; };
struct jhv_j2k_job {
    // Destruction closes the input before releasing its source.
    std::shared_ptr<jhv_j2k_source> owner;
    jhv_j2k_source::description description;
    decoder_input input;
    int level;
    jhv_j2k_job(const std::shared_ptr<jhv_j2k_source> &_source, int _frame, int _level)
        : owner(_source), description(_source->described(_frame)),
          input(_source->raw(), _frame, _level), level(_level) {}
};

// Kakadu's messages are process-wide: with concurrent decodes the text handed
// to a call is the latest, not necessarily its own.
static kdu_message_queue &errors() { static kdu_message_queue queue; return queue; }
static kdu_message_queue &warnings() { static kdu_message_queue queue; return queue; }
static void take(kdu_message_queue &queue, char *text) {
    static std::mutex popping;
    std::lock_guard<std::mutex> lock(popping);
    size_t used = std::strlen(text);
    while (const char *message = queue.pop_message()) {
        if (used < 255) std::snprintf(text + used, 256 - used, "%s%s", used ? " " : "", message);
        used = std::strlen(text);
    }
}

// Every exported fallible call contains all C++ and decoder exceptions.
template<class T, class F> static T guarded(char *error, T failed, F call) noexcept {
    error[0] = 0;
    try { return call(); }
    catch (kdu_exception code) {
        take(errors(), error);
        if (!error[0]) std::snprintf(error, 256, "JPEG 2000 decoder error (%d)", code);
        return failed;
    }
    catch (const std::bad_alloc &) { std::snprintf(error, 256, "JPEG 2000 native allocation failed"); }
    catch (const std::exception &e) { std::snprintf(error, 256, "%s", e.what()); }
    catch (...) { std::snprintf(error, 256, "Unexpected JPEG 2000 native failure"); }
    take(errors(), error);
    return failed;
}

int jhv_j2k_init(char *error) {
    return guarded(error, -1, [] {
        static std::once_flag once;
        std::call_once(once, [] {
            errors().configure(32, false, true);
            warnings().configure(32, false, false);
            kdu_customize_errors(&errors());
            kdu_customize_warnings(&warnings());
        });
        return 0;
    });
}
jhv_j2k *jhv_j2k_open(const char *path, char *error) {
    return guarded(error, static_cast<jhv_j2k *>(nullptr), [&] {
        std::unique_ptr<jhv_j2k> handle(new jhv_j2k);
        handle->source = path ? std::make_shared<jhv_j2k_source>(path) : std::make_shared<jhv_j2k_source>();
        return handle.release();
    });
}
void jhv_j2k_close(jhv_j2k *source) { delete source; }
int jhv_j2k_response(jhv_j2k *source, const uint8_t *body, uint64_t size, char *error) {
    return guarded(error, -1, [&] {
        return source->source->checked(hvc_response(source->source->raw(), body, size));
    });
}
int jhv_j2k_frames(jhv_j2k *source, char *error) {
    return guarded(error, -1, [&] { return source->source->frames(); });
}
int jhv_j2k_frame(jhv_j2k *source, int frame, jhv_j2k_frame_info *info, char *error) {
    return guarded(error, -1, [&] {
        hvc_view view = source->source->status(frame, 0);
        *info = jhv_j2k_frame_info();
        info->stream = view.codestream;
        if (!view.source.resolutions) return 0;
        const jhv_j2k_geometry &geometry = source->source->described(frame).geometry;
        info->channels = geometry.channels == 1 ? 1 : 4;
        info->levels = geometry.resolutions;
        info->ready = view.source.complete;
        for (int l = 0; l < info->levels; l++) {
            info->width[l] = geometry.level[l].width;
            info->height[l] = geometry.level[l].height;
        }
        return 0;
    });
}
int64_t jhv_j2k_xml(jhv_j2k *source, int frame, uint8_t *out, uint64_t capacity, char *error) {
    return guarded<int64_t>(error, -1, [&] {
        const uint8_t *xml; size_t size;
        source->source->checked(hvc_xml(source->source->raw(), frame, &xml, &size));
        if (size > INT64_MAX || (out && capacity < size)) throw std::runtime_error("XML output buffer too small");
        if (out && size) std::memcpy(out, xml, size);
        return static_cast<int64_t>(size);
    });
}
int jhv_j2k_palette(jhv_j2k *source, int frame, int32_t *channels, uint8_t *out, uint64_t capacity, char *error) {
    return guarded(error, -1, [&] {
        int count = 0;
        int entries = source->source->checked(hvc_palette(source->source->raw(), frame, &count, out, capacity));
        *channels = count;
        return entries;
    });
}
int64_t jhv_j2k_export(jhv_j2k *source, int frame, uint8_t *out, uint64_t capacity, char *error) {
    return guarded<int64_t>(error, -1, [&] {
        size_t size = hvc_export(source->source->raw(), frame, out, capacity);
        if (!size) throw std::runtime_error(hvc_error(source->source->raw()));
        if (size > INT64_MAX || (out && capacity < size)) throw std::runtime_error("cache entry buffer too small");
        return static_cast<int64_t>(size);
    });
}
int jhv_j2k_import(jhv_j2k *source, int frame, const uint8_t *entry, uint64_t size, char *error) {
    return guarded(error, -1, [&] {
        return source->source->checked(hvc_import(source->source->raw(), frame, entry, size));
    });
}
jhv_j2k_job *jhv_j2k_begin_decode(jhv_j2k *source, int frame, int level, char *error) {
    return guarded(error, static_cast<jhv_j2k_job *>(nullptr), [&] {
        hvc_view view = source->source->status(frame, level);
        if (!view.ready || view.reduce != level) throw std::runtime_error("JPEG 2000 level is not ready");
        return new jhv_j2k_job(source->source, frame, level);
    });
}
int64_t jhv_j2k_decode(jhv_j2k_job *job, int x, int y, int width, int height,
                     uint8_t *out, uint64_t capacity, char *error) {
    int64_t size = guarded<int64_t>(error, -1, [&] {
        jhv_j2k_level region = {x, y, width, height};
        const jhv_j2k_source::description &description = job->description;
        const char *reason = job->input.with_stream([&](kdu_codestream &stream) {
            return jhv_kdu_decode(stream, description.render, description.geometry,
                                  job->level, 0, region, out, capacity);
        });
        if (reason) throw std::runtime_error(reason);
        return int64_t(width) * height * (description.geometry.channels == 1 ? 1 : 4);
    });
    if (size >= 0) take(warnings(), error);
    return size;
}
void jhv_j2k_end_decode(jhv_j2k_job *job) { delete job; }
