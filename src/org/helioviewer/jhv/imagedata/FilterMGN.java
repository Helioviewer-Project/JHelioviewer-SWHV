package org.helioviewer.jhv.imagedata;

import java.util.stream.IntStream;

import org.helioviewer.jhv.math.MathUtils;

class FilterMGN implements ImageFilter.Algorithm {

    // derived from https://dev.ipol.im/~getreuer/code/
    private static class GaussFilter {

        private static final int SII_MIN_K = 3;
        private static final int SII_MAX_K = 5;

        private static final double sigma0 = 100 / Math.PI;
        private static final short[][] radii0 = {
                {76, 46, 23, 0, 0},
                {82, 56, 37, 19, 0},
                {85, 61, 44, 30, 16}};
        private static final float[][] weights0 = {
                {0.1618f, 0.5502f, 0.9495f, 0, 0},
                {0.0976f, 0.3376f, 0.6700f, 0.9649f, 0},
                {0.0739f, 0.2534f, 0.5031f, 0.7596f, 0.9738f}};

        // Box weights
        private final float[] weights = new float[SII_MAX_K];
        // Box radii
        private final int[] radii = new int[SII_MAX_K];
        // Number of boxes
        private final int K;

        private final ThreadLocal<float[]> buffer;

        GaussFilter(float sigma, int _K, int N) {
            K = _K;

            int i = K - SII_MIN_K;
            float sum = 0;

            for (int k = 0; k < K; ++k) {
                radii[k] = (int) (radii0[i][k] * (sigma / sigma0) + 0.5);
                sum += weights0[i][k] * (2 * radii[k] + 1);
            }

            for (int k = 0; k < K; ++k)
                weights[k] = weights0[i][k] / sum;

            int pad = radii[0] + 1;
            int bufferLength = N + 2 * pad;
            buffer = ThreadLocal.withInitial(() -> new float[bufferLength]);
        }

        private static int extension(int N, int n) {
            while (true) {
                if (n < 0)
                    n = -1 - n;         // Reflect over n = -1/2
                else if (n >= N)
                    n = 2 * N - 1 - n;  // Reflect over n = N - 1/2
                else
                    break;
            }
            return n;
        }

        private void gaussianConv(float[] dst, float[] src, int N, int stride, int offset, float[] scratch) {
            int pad = radii[0] + 1;
            float accum = 0;

            // Compute cumulative sum of src over n = -pad,..., N + pad - 1
            for (int n = -pad; n < 0; ++n) {
                accum += src[offset + stride * extension(N, n)];
                scratch[pad + n] = accum;
            }
            int srcPos = offset;
            for (int n = 0; n < N; ++n) {
                accum += src[srcPos];
                scratch[pad + n] = accum;
                srcPos += stride;
            }
            for (int n = N; n < N + pad; ++n) {
                accum += src[offset + stride * extension(N, n)];
                scratch[pad + n] = accum;
            }

            // Compute stacked box filters
            int dstPos = offset;
            for (int n = 0; n < N; ++n) {
                accum = weights[0] * (scratch[pad + n + radii[0]] - scratch[pad + n - radii[0] - 1]);
                for (int k = 1; k < K; ++k)
                    accum += weights[k] * (scratch[pad + n + radii[k]] - scratch[pad + n - radii[k] - 1]);
                dst[dstPos] = accum;
                dstPos += stride;
            }
        }

        void gaussianConvImage(float[] dst, float[] src, int width, int height) {
            IntStream.range(0, height).parallel().forEach(y ->
                    gaussianConv(dst, src, width, 1, width * y, buffer.get()));
            IntStream.range(0, width).parallel().forEach(x ->
                    gaussianConv(dst, dst, height, width, x, buffer.get()));
        }

    }

    private static final int K = 3;
    private static final float MIX_FACTOR = 0.97f;
    private static final float ONE_MINUS_MIX_FACTOR = 1f - MIX_FACTOR;
    private static final float[] sigmas = {1, 4, 16, 64};
    private static final float[] weights = {0.125f, 0.25f, 0.5f, 1f};

    private static void gaussNormAccumulate(float[] data, int width, int height, float weight, GaussFilter filter,
                                            float[] conv, float[] conv2, float[] accum) {
        filter.gaussianConvImage(conv, data, width, height);
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowBase = y * width;
            int rowEnd = rowBase + width;
            for (int i = rowBase; i < rowEnd; i++) {
                float v = data[i] - conv[i];
                conv[i] = v;
                conv2[i] = v * v;
            }
        });
        filter.gaussianConvImage(conv2, conv2, width, height);
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowBase = y * width;
            int rowEnd = rowBase + width;
            for (int i = rowBase; i < rowEnd; i++) {
                accum[i] += conv2[i] == 0 ? 0 : weight * conv[i] * MathUtils.invSqrt(conv2[i]);
            }
        });
    }

    @Override
    public float[] filter(float[] data, int width, int height) {
        if (width < 1 || height < 1)
            return data;

        int size = width * height;
        float[] accum = new float[size];
        float[] conv = new float[size];
        float[] conv2 = new float[size];

        int maxDim = Math.max(width, height);
        GaussFilter[] filters = new GaussFilter[sigmas.length];
        for (int i = 0; i < sigmas.length; i++) {
            filters[i] = new GaussFilter(sigmas[i], K, maxDim);
        }

        for (int i = 0; i < sigmas.length; i++) {
            gaussNormAccumulate(data, width, height, weights[i], filters[i], conv, conv2, accum);
        }

        float[] image = new float[size];
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowBase = y * width;
            int rowEnd = rowBase + width;
            for (int i = rowBase; i < rowEnd; i++) {
                image[i] = accum[i] * ONE_MINUS_MIX_FACTOR + data[i] * MIX_FACTOR;
            }
        });
        return image;
    }

}
