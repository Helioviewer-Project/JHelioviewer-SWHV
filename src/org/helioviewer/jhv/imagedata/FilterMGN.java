package org.helioviewer.jhv.imagedata;

import java.util.stream.IntStream;
import java.util.List;

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

        private final float[] buffer;

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
            buffer = new float[N + 2 * pad];
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

        private void gaussianConv(float[] dst, float[] src, int N, int stride, int offset) {
            int pad = radii[0] + 1;
            float accum = 0;

            // Compute cumulative sum of src over n = -pad,..., N + pad - 1
            for (int n = -pad; n < 0; ++n) {
                accum += src[offset + stride * extension(N, n)];
                buffer[pad + n] = accum;
            }
            for (int n = 0; n < N; ++n) {
                accum += src[offset + stride * n];
                buffer[pad + n] = accum;
            }
            for (int n = N; n < N + pad; ++n) {
                accum += src[offset + stride * extension(N, n)];
                buffer[pad + n] = accum;
            }

            // Compute stacked box filters
            for (int n = 0; n < N; ++n) {
                accum = weights[0] * (buffer[pad + n + radii[0]] - buffer[pad + n - radii[0] - 1]);
                for (int k = 1; k < K; ++k)
                    accum += weights[k] * (buffer[pad + n + radii[k]] - buffer[pad + n - radii[k] - 1]);
                dst[offset + stride * n] = accum;
            }
        }

        void gaussianConvImage(float[] dst, float[] src, int width, int height) {
            // Filter each row
            for (int y = 0; y < height; ++y)
                gaussianConv(dst, src, width, 1, width * y);
            // Filter each column
            for (int x = 0; x < width; ++x)
                gaussianConv(dst, dst, height, width, x);
        }

    }

    private static final int K = 3;
    private static final float MIX_FACTOR = 0.97f;
    private static final float ONE_MINUS_MIX_FACTOR = 1f - MIX_FACTOR;
    private static final float[] sigmas = {1, 4, 16, 64};
    private static final float[] weights = {0.125f, 0.25f, 0.5f, 1f};

    private float[] gaussNorm(float[] data, int width, int height, float sigma, float weight) {
        GaussFilter filter = new GaussFilter(sigma, K, Math.max(width, height));

        int size = width * height;
        float[] conv = new float[size];
        float[] conv2 = new float[size];

        filter.gaussianConvImage(conv, data, width, height);
        for (int i = 0; i < size; ++i) {
            float v = data[i] - conv[i];
            conv[i] = v;
            conv2[i] = v * v;
        }
        filter.gaussianConvImage(conv2, conv2, width, height);

        for (int i = 0; i < size; ++i)
            conv[i] = conv2[i] == 0 ? 0 : weight * conv[i] * MathUtils.invSqrt(conv2[i]);

        return conv;
    }

    @Override
    public float[] filter(float[] data, int width, int height) {
        // Process each sigma in parallel and collect results
        List<float[]> results = IntStream.range(0, sigmas.length)
                .parallel()
                .mapToObj(i -> gaussNorm(data, width, height, sigmas[i], weights[i]))
                .toList();

        int size = width * height;
        float[] image = new float[size];
        // Combine accumulation and blending in a single parallel pass
        IntStream.range(0, size).parallel().forEach(i -> {
            float sum = 0;
            for (float[] res : results) {
                sum += res[i];
            }
            image[i] = sum * ONE_MINUS_MIX_FACTOR + data[i] * MIX_FACTOR;
        });
        return image;
    }

}
