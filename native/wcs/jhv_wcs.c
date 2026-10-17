#include "jhv_wcs.h"

#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "wcs.h"
#include "wcshdr.h"
#include "sph.h"
#include "wcstrig.h"

_Static_assert(sizeof(jhv_wcs_info) == 264 && offsetof(jhv_wcs_info, native_basis) == 72,
               "Java/native WCS layout");

struct jhv_wcs {
    struct wcsprm value;
    double lonpole, latpole; // Requested values, before wcsset resolves defaults.
};

static const double radians = 0.017453292519943295769;
static const double degrees = 57.295779513082320877;

void jhv_wcs_destroy(jhv_wcs *wcs) {
    if (wcs) {
        wcsfree(&wcs->value);
        free(wcs);
    }
}

static jhv_wcs *failure(jhv_wcs *wcs, const char *message, char *error, size_t capacity) {
    if (capacity) snprintf(error, capacity, "%s", message);
    jhv_wcs_destroy(wcs);
    return NULL;
}

static int uses_pv(int projection, int coefficient) {
    return (projection == JHV_WCS_ZPN && coefficient < 6) ||
           (projection == JHV_WCS_AZP && (coefficient == 1 || coefficient == 2)) ||
           (projection == JHV_WCS_CEA && coefficient == 1);
}

static jhv_wcs *prepare(jhv_wcs *handle, jhv_wcs_info *info, size_t info_size,
                        char *error, size_t capacity) {
    if (info_size != sizeof(*info)) return failure(handle, "WCS ABI mismatch", error, capacity);
    struct wcsprm *w = &handle->value;
    static const char *codes[] = {"TAN", "ARC", "AZP", "ZPN", "CAR", "CEA"};
    int projection;
    for (projection = 0; projection < JHV_WCS_PROJECTION_COUNT; projection++)
        if (strlen(w->ctype[0]) == 8 && !strcmp(w->ctype[0]+5, codes[projection])) break;
    if (projection == JHV_WCS_PROJECTION_COUNT) return failure(handle, "Unsupported WCS projection", error, capacity);

    // The CPU and GPU evaluate the same effective coefficients. Reject float underflow
    // or overflow where it changes the allowed parameter family, rather than clamping.
    for (int i = 0; i < w->npv; i++) {
        struct pvcard *p = w->pv+i;
        if (p->i != 2) continue;
        if (projection == JHV_WCS_ZPN && p->m >= 6 && p->value != 0)
            return failure(handle, "ZPN supports PV2_0 through PV2_5", error, capacity);
        if (uses_pv(projection, p->m)) {
            p->value = (float)p->value;
            if (!isfinite(p->value)) return failure(handle, "Non-finite WCS coefficient", error, capacity);
            if (projection == JHV_WCS_CEA && !(p->value > 0 && p->value <= 1))
                return failure(handle, "CEA requires 0 < PV2_1 <= 1 after float encoding", error, capacity);
        }
    }
    w->flag = 0;
    int status = wcsset(w);
    if (status) return failure(handle, wcs_errmsg[status], error, capacity);
    if (w->naxis != 2 || w->lng != 0 || w->lat != 1 || !w->lin.affine)
        return failure(handle, "WCS requires two celestial axes and an affine pixel transform", error, capacity);

    memset(info, 0, sizeof(*info));
    info->version = 1;
    info->projection = projection;
    memcpy(info->reference_pixel, w->crpix, sizeof(info->reference_pixel));
    double pixels[6] = {w->crpix[0], w->crpix[1], w->crpix[0]+1, w->crpix[1], w->crpix[0], w->crpix[1]+1};
    double plane[6];
    status = linp2x(&w->lin, 3, 2, pixels, plane);
    if (status) return failure(handle, "Invalid WCS affine transform", error, capacity);
    info->pixel_to_plane[0] = (plane[2]-plane[0])*radians;
    info->pixel_to_plane[1] = (plane[4]-plane[0])*radians;
    info->pixel_to_plane[2] = (plane[3]-plane[1])*radians;
    info->pixel_to_plane[3] = (plane[5]-plane[1])*radians;
    info->reference[0] = w->crval[0]*radians;
    info->reference[1] = w->crval[1]*radians;

    // Ask WCSLIB for the rotated Cartesian axes. Its degree trig functions preserve
    // exact cardinal values. Input JHV (x,y,z) corresponds to celestial (z,x,y).
    double lng[3] = {0,90,0}, lat[3] = {0,0,90}, phi[3], theta[3];
    sphs2x(w->cel.euler, 3, 0, 1, 1, lng, lat, phi, theta);
    for (int j = 0; j < 3; j++) {
        double sp, cp, st, ct;
        sincosd(phi[j], &sp, &cp);
        sincosd(theta[j], &st, &ct);
        int column = (j+2)%3;
        info->native_basis[column] = ct*sp;
        info->native_basis[4+column] = -ct*cp;
        info->native_basis[8+column] = st;
    }
    info->offset[0] = w->cel.prj.x0*radians;
    info->offset[1] = w->cel.prj.y0*radians;
    for (int i = 0; i < 6; i++)
        if (uses_pv(projection, i))
            info->pv[i] = w->cel.prj.pv[i];
    if (projection == JHV_WCS_AZP) {
        info->native_basis[3] = w->cel.prj.w[1];
        info->native_basis[7] = w->cel.prj.w[2];
    }
    if (projection == JHV_WCS_ZPN) info->native_basis[11] = w->cel.prj.w[0];
    // A Cartesian point at a native pole carries no native longitude. Preserve
    // WCSLIB's convention, using longitude zero at a celestial Cartesian pole.
    for (int i = 0; i < 2; i++) {
        double p = 0, t = i == 0 ? 90 : -90, longitude, latitude;
        sphx2s(w->cel.euler, 1, 0, 1, 1, &p, &t, &longitude, &latitude);
        if (fabs(latitude) == 90) longitude = 0;
        sphs2x(w->cel.euler, 1, 0, 1, 1, &longitude, &latitude, &p, &t);
        double sp, cp;
        sincosd(p, &sp, &cp);
        info->native_poles[2*i] = sp;
        info->native_poles[2*i+1] = -cp;
    }
    return handle;
}

jhv_wcs *jhv_wcs_create(const char *cards, int count, jhv_wcs_info *info,
                         size_t info_size, char *error, size_t capacity) {
    struct wcsprm *parsed = NULL;
    int total = 0, rejected = 0;
    int status = wcspih((char *)cards, count, WCSHDR_all, 0, &rejected, &total, &parsed);
    if (status || rejected || !total) {
        wcsvfree(&total, &parsed);
        return failure(NULL, "Invalid FITS WCS header", error, capacity);
    }
    jhv_wcs *handle = calloc(1, sizeof(*handle));
    if (!handle) {
        wcsvfree(&total, &parsed);
        return failure(NULL, "WCS allocation failed", error, capacity);
    }
    handle->value.flag = -1;
    struct wcsprm *primary = NULL;
    for (int i = 0; i < total; i++)
        if (parsed[i].alt[0] == ' ' || parsed[i].alt[0] == 0) primary = parsed+i;
    status = primary ? wcscopy(1, primary, &handle->value) : 1;
    wcsvfree(&total, &parsed);
    if (status) return failure(handle, "Missing primary WCS", error, capacity);
    handle->lonpole = handle->value.lonpole;
    handle->latpole = handle->value.latpole;
    return prepare(handle, info, info_size, error, capacity);
}

jhv_wcs *jhv_wcs_adjust(const jhv_wcs *source, double longitude, double latitude,
                         jhv_wcs_info *info, size_t info_size, char *error, size_t capacity) {
    jhv_wcs *handle = calloc(1, sizeof(*handle));
    if (!handle) return failure(NULL, "WCS allocation failed", error, capacity);
    handle->value.flag = -1;
    if (wcscopy(1, &source->value, &handle->value))
        return failure(handle, "WCS copy failed", error, capacity);
    handle->value.crval[0] += longitude*degrees;
    handle->value.crval[1] += latitude*degrees;
    handle->lonpole = handle->value.lonpole = source->lonpole;
    handle->latpole = handle->value.latpole = source->latpole;
    return prepare(handle, info, info_size, error, capacity);
}

int jhv_wcs_transform(jhv_wcs *handle, int to_plane, int count, double *coordinates) {
    struct celprm *cel = &handle->value.cel;
    int wrap_longitude = !to_plane && (!strcmp(cel->prj.code,"CAR") || !strcmp(cel->prj.code,"CEA"));
    int invalid = 0;
    for (int i = 0; i < count; i++) {
        double a = coordinates[2*i]*degrees, b = coordinates[2*i+1]*degrees;
        double x, y, phi, theta;
        int stat = 0, status;
        if (to_plane) {
            status = cels2x(cel, 1, 0, 1, 1, &a, &b, &phi, &theta, &x, &y, &stat);
        } else {
            // Cylinders may store a different whole-turn copy. Reduce only longitude;
            // latitude and the other projection-domain checks remain enabled.
            if (wrap_longitude)
                a = remainder(a+cel->prj.x0, 360)-cel->prj.x0;
            status = celx2s(cel, 1, 0, 1, 1, &a, &b, &phi, &theta, &x, &y, &stat);
        }
        if (status || stat || !isfinite(x) || !isfinite(y)) {
            coordinates[2*i] = coordinates[2*i+1] = NAN;
            invalid++;
        } else {
            if (!to_plane)
                x = handle->value.crval[0] + remainder(x-handle->value.crval[0], 360);
            coordinates[2*i] = x*radians;
            coordinates[2*i+1] = y*radians;
        }
    }
    return invalid;
}
