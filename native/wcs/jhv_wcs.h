#ifndef JHV_WCS_H
#define JHV_WCS_H

#include <stddef.h>
#include <stdint.h>

#ifdef _WIN32
#define JHV_WCS_API __declspec(dllexport)
#else
#define JHV_WCS_API __attribute__((visibility("default")))
#endif

typedef struct jhv_wcs jhv_wcs;

enum jhv_wcs_projection {
    JHV_WCS_TAN, JHV_WCS_ARC, JHV_WCS_AZP, JHV_WCS_ZPN, JHV_WCS_CAR, JHV_WCS_CEA,
    JHV_WCS_PROJECTION_COUNT
};

// Version 1: 264 bytes on every packaged platform. Angles and plane coordinates are radians.
typedef struct {
    uint32_t version, projection; // Projection codes from jhv_wcs_projection.
    double reference_pixel[2];   // FITS centers, one based, Y up.
    double pixel_to_plane[4];    // Row major.
    double reference[2];        // Celestial longitude and latitude.
    double native_basis[12];    // Three vec4 rows: JHV direction -> (east, north, zenith).
                               // Row W values: AZP tan(gamma), sec(gamma), ZPN upper eta.
    double offset[2];           // Subtract from the raw native projection plane.
    double pv[6];               // Effective float coefficients, also used by WCSLIB.
    double native_poles[4];     // Projection directions at native north and south poles.
} jhv_wcs_info;

JHV_WCS_API jhv_wcs *jhv_wcs_create(const char *cards, int count, jhv_wcs_info *info,
                                  size_t info_size, char *error, size_t capacity);
JHV_WCS_API jhv_wcs *jhv_wcs_adjust(const jhv_wcs *source, double longitude, double latitude,
                                  jhv_wcs_info *info, size_t info_size, char *error, size_t capacity);
JHV_WCS_API void jhv_wcs_destroy(jhv_wcs *wcs);
// In-place pairs: plane -> celestial, or celestial -> plane when to_plane != 0.
// Invalid points become NaN pairs; the return value is the number of invalid points.
JHV_WCS_API int jhv_wcs_transform(jhv_wcs *wcs, int to_plane, int count, double *coordinates);

#endif
