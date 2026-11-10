#include <stdint.h>
#include <stddef.h>
#include <stdio.h>
#include <math.h>
#include <string.h>
#include <stdlib.h>
#include <float.h>
#include "fitsio2.h"

#ifdef _WIN32
#define JHV_EXPORT __declspec(dllexport)
#else
#define JHV_EXPORT __attribute__((visibility("default")))
#endif

_Static_assert(sizeof(float) == 4 && sizeof(double) == 8,
               "FITS floating-point types must match the Java layouts");

JHV_EXPORT int jhv_fits_init(void) {
    if (!fits_is_reentrant() || fits_init_cfitsio())
        return 1;
    /* CFITSIO publishes its dither table before filling it. Initialize before workers start. */
    return fits_init_randoms();
}

/* Caller owns input and output until return. No native handle escapes this call. */
static int read_pixels(const void *input, int64_t input_length,
        int32_t hdu, int32_t bitpix, int32_t width, int32_t height, int32_t plane,
        int32_t firstrow, int32_t rows, int32_t stepx, int32_t stepy,
        void *output, int64_t capacity, char *error, int datatype) {
    fitsfile *file = NULL;
    int status = 0, type = 0, axes = 0, actual = 0, anynull = 0;
    long dimensions[3] = {0, 0, 0};
    int size = datatype == TBYTE ? 1 : sizeof(float);
    if (!input || input_length <= 0 || !output || hdu < 1 || width <= 0 || height <= 0 ||
            plane < 0 || firstrow < 0 || rows <= 0 || firstrow > height - rows ||
            stepx <= 0 || stepy <= 0 || capacity < 0) {
        snprintf(error, 128, "Invalid FITS input or output shape");
        return -1;
    }
    int64_t count = (((int64_t)width + stepx - 1) / stepx) *
                    (((int64_t)rows + stepy - 1) / stepy);
    if (count > capacity / size) {
        snprintf(error, 128, "FITS output buffer is too small");
        return -1;
    }
    void *memory = (void *)input;
    size_t length = (size_t)input_length;
    fits_open_memfile(&file, "jhv-buffer", READONLY, &memory, &length, 0, NULL, &status);
    if (!status) fits_movabs_hdu(file, hdu, &type, &status);
    if (!status) fits_get_img_dim(file, &axes, &status);
    if (!status) fits_get_img_type(file, &actual, &status);
    if (!status) fits_get_img_size(file, 3, dimensions, &status);
    if (!status && (axes < 2 || axes > 3 || dimensions[0] != width ||
            dimensions[1] != height || actual != bitpix ||
            (axes == 2 && plane != 0) || (axes == 3 && plane >= dimensions[2])))
        status = BAD_DIMEN;
    // Byte images retain their existing raw display values, as do detector masks.
    if (!status && bitpix == 8) fits_set_bscale(file, 1.0, 0.0, &status);
    float fnan = NAN;
    void *nulval = datatype == TFLOAT && bitpix > 8 ? &fnan : NULL;
    if (!status && bitpix < 0 && fits_is_compressed_image(file, &status)) {
        int column, scale_status = 0, zero_status = 0, quantize_status = 0;
        char quantize[FLEN_VALUE];
        fits_get_colnum(file, CASEINSEN, "ZSCALE", &column, &scale_status);
        fits_get_colnum(file, CASEINSEN, "ZZERO", &column, &zero_status);
        fits_read_key(file, TSTRING, "ZQUANTIZ", quantize, NULL, &quantize_status);
        // Quantized pixels need null substitution; IEEE pixels must retain infinities.
        if ((scale_status == 0 || zero_status == 0) &&
                (quantize_status != 0 || strcmp(quantize, "NONE") != 0))
            nulval = &fnan;
    }
    // CFITSIO's double-to-float conversion reports IEEE infinities as overflow.
    // Read IEEE doubles unchanged, then narrow them without losing their meaning.
    double *wide = NULL;
    if (!status && bitpix == -64 && nulval == NULL) {
        if ((uint64_t)count > SIZE_MAX / sizeof(double) || !(wide = malloc((size_t)count * sizeof(double))))
            status = MEMORY_ALLOCATION;
    }
    void *pixels = wide ? (void *)wide : output;
    int read_type = wide ? TDOUBLE : datatype;
    long first[3] = {1, (long)firstrow + 1, (long)plane + 1};
    if (!status) {
        if (stepx == 1 && stepy == 1) {
            fits_read_pix(file, read_type, first, count, nulval, pixels, &anynull, &status);
        } else {
            long last[3] = {width, (long)firstrow + rows, (long)plane + 1};
            long inc[3] = {stepx, stepy, 1};
            fits_read_subset(file, read_type, first, last, inc, nulval, pixels, &anynull, &status);
        }
    }
    if (!status && wide) {
        for (int64_t i = 0; i < count; i++)
            ((float *)output)[i] = wide[i] > FLT_MAX ? INFINITY
                    : wide[i] < -FLT_MAX ? -INFINITY : (float)wide[i];
    }
    free(wide);
    int close_status = 0;
    if (file) fits_close_file(file, &close_status);
    if (!status) status = close_status;
    /* The process-wide CFITSIO message stack is deliberately not consumed. */
    if (status) fits_get_errstatus(status, error);
    return status;
}

JHV_EXPORT int jhv_fits_read_float(const void *input, int64_t input_length,
        int32_t hdu, int32_t bitpix, int32_t width, int32_t height, int32_t plane,
        int32_t firstrow, int32_t rows, int32_t stepx, int32_t stepy,
        void *output, int64_t capacity, char *error) {
    return read_pixels(input, input_length, hdu, bitpix, width, height, plane,
            firstrow, rows, stepx, stepy, output, capacity, error, TFLOAT);
}

JHV_EXPORT int jhv_fits_read_mask(const void *input, int64_t input_length,
        int32_t width, int32_t height, void *output, int64_t capacity, char *error) {
    return read_pixels(input, input_length, 1, 8, width, height, 0,
            0, height, 1, 1, output, capacity, error, TBYTE);
}
