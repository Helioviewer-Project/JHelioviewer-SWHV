/* JHV's JPEG 2000 engine: the esajpip C client reads local files and JPIP data,
 * Kakadu decodes. Lengths are bytes. Errors are text in a 256-byte buffer;
 * negative results or NULL mean failure. No exception crosses this boundary.
 * The host serializes calls on one source, including close and begin_decode.
 * A decode job retains its source and may run while the source is used or closed. */
#ifndef JHV_J2K_H
#define JHV_J2K_H
#include <stdint.h>
#ifdef _WIN32
#define JHV_J2K_API __declspec(dllexport)
#else
#define JHV_J2K_API __attribute__((visibility("default")))
#endif
#ifdef __cplusplus
extern "C" {
#endif
typedef struct jhv_j2k jhv_j2k;
typedef struct jhv_j2k_job jhv_j2k_job;
typedef struct {
    uint64_t stream;               /* JPIP stream of the frame */
    int32_t channels;              /* bytes per decoded pixel, 1 or 4 */
    int32_t levels, ready;         /* resolution levels; how many, from the coarsest, are ready */
    int32_t width[33], height[33]; /* per level, finest first */
} jhv_j2k_frame_info;
/* A local JP2/JPX file (UTF-8 path), or an empty JPIP source for NULL. */
JHV_J2K_API jhv_j2k *jhv_j2k_open(const char *path, char *error);
JHV_J2K_API void jhv_j2k_close(jhv_j2k *source);
/* Begin delivery on a replacement channel without discarding cached data. */
JHV_J2K_API void jhv_j2k_new_channel(jhv_j2k *source);
/* One whole JPIP response body. Returns its end-of-response reason and reports
 * delivery of new data-bin ranges or final flags in progress. Complete windows
 * clear only their own delivery history. window is the requested frame, or -1
 * for metadata. A failure is a refusal: accept no further responses. */
JHV_J2K_API int jhv_j2k_response(jhv_j2k *source, const uint8_t *body, uint64_t size, int window,
                               int *progress, char *error);
JHV_J2K_API int jhv_j2k_frames(jhv_j2k *source, char *error);
/* Without the frame's header only stream is set. Fails if the frame cannot be decoded. */
JHV_J2K_API int jhv_j2k_frame(jhv_j2k *source, int frame, jhv_j2k_frame_info *info, char *error);
/* Size query with NULL output, then a copy. Palette is a 256-entry RGBA8
 * display table in color order, or absent. Indexed frames decode to raw bytes;
 * indices beyond the palette's entries select its last entry. */
JHV_J2K_API int64_t jhv_j2k_xml(jhv_j2k *source, int frame, uint8_t *out, uint64_t capacity, char *error);
JHV_J2K_API int jhv_j2k_palette_rgba(jhv_j2k *source, int frame, uint8_t *out, uint64_t capacity, char *error);
/* Disk cache entry of a JPIP frame. A refused import leaves the source unchanged. */
JHV_J2K_API int64_t jhv_j2k_export(jhv_j2k *source, int frame, uint8_t *out, uint64_t capacity, char *error);
JHV_J2K_API int jhv_j2k_import(jhv_j2k *source, int frame, const uint8_t *entry, uint64_t size, char *error);
/* Fails unless the level (0 finest) is ready. Decode writes Gray8 or RGBA rows
 * of a region of that level and returns the byte count; on success error holds
 * any Kakadu warning. Run and end a job on one thread. */
JHV_J2K_API jhv_j2k_job *jhv_j2k_begin_decode(jhv_j2k *source, int frame, int level, char *error);
JHV_J2K_API int64_t jhv_j2k_decode(jhv_j2k_job *job, int x, int y, int width, int height,
                                uint8_t *out, uint64_t capacity, char *error);
JHV_J2K_API void jhv_j2k_end_decode(jhv_j2k_job *job);
#ifdef __cplusplus
}
#endif
#endif
