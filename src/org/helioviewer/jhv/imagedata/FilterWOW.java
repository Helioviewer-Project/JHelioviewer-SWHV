package org.helioviewer.jhv.imagedata;

import java.util.OptionalDouble;
import java.util.stream.IntStream;

import org.helioviewer.jhv.math.MathUtils;

@SuppressWarnings("serial")
class FilterWOW implements ImageFilter.Algorithm {

    private static final int SCALES = 6;
    private static final float MIX_FACTOR = 0.99f;
    private static final float ONE_MINUS_MIX_FACTOR = 1f - MIX_FACTOR;
    private static final float SIGMA_E0 = 8.907e-1f;
    private static final float SIGMA_E1 = 2.0072e-1f;
    private static final float BOOST = 1.5f; // restore some crispness after denoising
    private static final float NOISE_THRESH = 1e-10f; // avoid denoising very low noise or blank images

    // private static final float[] FILTER = {1f / 16, 4f / 16, 6f / 16, 4f / 16, 1f / 16};
    private static final float FILTER0 = 1f / 16;
    private static final float FILTER1 = 4f / 16;
    private static final float FILTER2 = 6f / 16;
    private static final float FILTER3 = 4f / 16;
    private static final float FILTER4 = 1f / 16;

    @Override
    public float[] filter(float[] data, int width, int height) {
        if (width < 128 || height < 128)
            return data;

        float[] image = data.clone();

        int length = data.length;
        float[] coeff = new float[length];
        float[] temp1 = new float[length];
        float[] temp2 = new float[length];
        float[] synth = new float[length];
        float[] denoiseFactor = new float[1];

        float noise = 0; // computed for scale 0
        for (int scale = 0; scale < SCALES; scale++) {
            int step = 1 << scale;
            // A trous transform
            convolveHorizontal(image, coeff, width, height, step); // Horizontal pass
            convolveVertical(coeff, temp1, width, height, step); // Vertical pass
            subtractParallel(image, temp1, coeff, width, height); // Coefficients
            System.arraycopy(temp1, 0, image, 0, length); // Update image for next scale
            // Whiten coefficients
            convolveHorizontalSquared(coeff, temp1, width, height, step); // Squared src horizontal pass
            convolveVertical(temp1, temp2, width, height, step); // Vertical pass
            // Denoise stage
            if (scale == 0) {
                noise = (1.48260221850560f / SIGMA_E0) * medianStream(coeff, length);
                if (noise > NOISE_THRESH) { // avoid division by 0
                    denoiseFactor[0] = 1 / (3 * SIGMA_E0 * noise);
                    denoiseParallel(denoiseFactor[0], coeff, width, height);
                }
            } else if (scale == 1) {
                if (noise > NOISE_THRESH) { // avoid division by 0
                    denoiseFactor[0] = 1 / (SIGMA_E1 * noise);
                    denoiseParallel(denoiseFactor[0], coeff, width, height);
                }
            }
            // Whitened synthesis
            synthesisParallel(temp2, coeff, synth, width, height);
        }
        blendParallel(image, data, synth, width, height);
        return synth;
    }

/*
    private static float median(float[] c, int length) {
        float[] w = new float[length];
        IntStream.range(0, length).parallel().forEach(i -> w[i] = Math.abs(c[i]));
        Arrays.parallelSort(w); // can be faster than serial quickSelect
        return w[length / 2];
    }
*/

    private static float medianStream(float[] c, int length) {
        OptionalDouble od = IntStream.range(0, length)
                .parallel()
                .mapToDouble(i -> Math.abs(c[i]))
                .sorted()
                .skip(length / 2)
                .findFirst();
        return od.isEmpty() ? 0 : (float) od.getAsDouble();
    }

    private static void subtractParallel(float[] src1, float[] src2, float[] dest, int width, int height) {
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowBase = y * width;
            for (int x = 0; x < width; x++) {
                int idx = rowBase + x;
                dest[idx] = src1[idx] - src2[idx];
            }
        });
    }

    private static void denoiseParallel(float factor, float[] dest, int width, int height) {
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowBase = y * width;
            for (int x = 0; x < width; x++) {
                int idx = rowBase + x;
                float w = Math.abs(dest[idx]) * factor;
                dest[idx] *= BOOST * w / (1 + w);
            }
        });
    }

    private static void synthesisParallel(float[] weight, float[] detail, float[] dest, int width, int height) {
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowBase = y * width;
            for (int x = 0; x < width; x++) {
                int idx = rowBase + x;
                dest[idx] += MathUtils.invSqrt(weight[idx]) * detail[idx];
            }
        });
    }

    private static void blendParallel(float[] image, float[] data, float[] dest, int width, int height) {
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowBase = y * width;
            for (int x = 0; x < width; x++) {
                int idx = rowBase + x;
                dest[idx] = (dest[idx] + image[idx]) * ONE_MINUS_MIX_FACTOR + data[idx] * MIX_FACTOR;
            }
        });
    }

    private static void convolveHorizontal(float[] src, float[] dest, int width, int height, int step) {
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowBase = y * width;
            for (int x = 0; x < width; x++) {
                float sum = 0;
                int idx_m2 = mirroredIdx(x - 2 * step, width);
                sum += src[rowBase + idx_m2] * FILTER0;
                int idx_m1 = mirroredIdx(x - step, width);
                sum += src[rowBase + idx_m1] * FILTER1;
                sum += src[rowBase + x] * FILTER2;
                int idx_p1 = mirroredIdx(x + step, width);
                sum += src[rowBase + idx_p1] * FILTER3;
                int idx_p2 = mirroredIdx(x + 2 * step, width);
                sum += src[rowBase + idx_p2] * FILTER4;
                dest[rowBase + x] = sum;
            }
        });
    }

    private static void convolveHorizontalSquared(float[] src, float[] dest, int width, int height, int step) {
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowBase = y * width;
            for (int x = 0; x < width; x++) {
                float sum = 0;
                int idx_m2 = mirroredIdx(x - 2 * step, width);
                float v_m2 = src[rowBase + idx_m2];
                sum += v_m2 * v_m2 * FILTER0;
                int idx_m1 = mirroredIdx(x - step, width);
                float v_m1 = src[rowBase + idx_m1];
                sum += v_m1 * v_m1 * FILTER1;
                float v0 = src[rowBase + x];
                sum += v0 * v0 * FILTER2;
                int idx_p1 = mirroredIdx(x + step, width);
                float v_p1 = src[rowBase + idx_p1];
                sum += v_p1 * v_p1 * FILTER3;
                int idx_p2 = mirroredIdx(x + 2 * step, width);
                float v_p2 = src[rowBase + idx_p2];
                sum += v_p2 * v_p2 * FILTER4;
                dest[rowBase + x] = sum;
            }
        });
    }

    private static void convolveVertical(float[] src, float[] dest, int width, int height, int step) {
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowBase = y * width;
            int rowM2 = mirroredIdx(y - 2 * step, height) * width;
            int rowM1 = mirroredIdx(y - step, height) * width;
            int rowP1 = mirroredIdx(y + step, height) * width;
            int rowP2 = mirroredIdx(y + 2 * step, height) * width;
            for (int x = 0; x < width; x++) {
                float sum = 0;
                sum += src[rowM2 + x] * FILTER0;
                sum += src[rowM1 + x] * FILTER1;
                sum += src[rowBase + x] * FILTER2;
                sum += src[rowP1 + x] * FILTER3;
                sum += src[rowP2 + x] * FILTER4;
                dest[rowBase + x] = sum;
            }
        });
    }

    private static int mirroredIdx(int idx, int size) {
        if (idx < 0) {
            return -idx;
        } else if (idx >= size) {
            return 2 * size - idx - 2;
        }
        return idx;
    }

}
