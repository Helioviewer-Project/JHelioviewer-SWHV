package org.helioviewer.jhv.imagedata;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinTask;
import java.util.ArrayList;

import org.helioviewer.jhv.math.MathUtils;

class ImageFilter {

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
    private static final float H = 0.8f;
    private static final float[] sigmas = {1, 4, 16, 64};
    private static final float[] weights = {0.25f, 0.5f, 0.75f, 1f};

    private record ScaleTask(float[] data, int width, int height, float sigma, float weight)
            implements Callable<float[]> {
        @Override
        public float[] call() {
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
                conv[i] = conv2[i] == 0 ? 0 : weight * MathUtils.clip(conv[i] * MathUtils.invSqrt(conv2[i]), -1, 1);

            return conv;
        }
    }

    private static float[] multiScale(float[] data, int width, int height) {
        ArrayList<ForkJoinTask<float[]>> tasks = new ArrayList<>(sigmas.length);
        for (int i = 0; i < sigmas.length; ++i)
            tasks.add(ForkJoinTask.adapt(new ScaleTask(data, width, height, sigmas[i], weights[i])).fork());

        int size = width * height;
        float[] image = new float[size];
        for (ForkJoinTask<float[]> task : tasks) {
            float[] res = task.join();
            for (int i = 0; i < size; ++i)
                image[i] += res[i];
        }

        float min = 1e6f, max = -1e6f;
        for (int i = 0; i < size; ++i) {
            float v = image[i];
            if (v > max)
                max = v;
            if (v < min)
                min = v;
        }

        if (min == max)
            return data;

        float k = (1 - H) / (max - min);
        for (int i = 0; i < size; ++i)
            image[i] = k * (image[i] - min) + H * data[i];
        return image;
    }

    private static ByteBuffer mgn(ByteBuffer buf, int width, int height) {
        int size = width * height;
        float[] data = new float[size];

        byte[] array = buf.array(); // always backed by array
        for (int i = 0; i < size; ++i)
            data[i] = ((array[i] + 256) & 0xFF) / 255f;

        float[] image = multiScale(data, width, height);

        byte[] out = new byte[size];
        for (int i = 0; i < size; ++i)
            out[i] = (byte) MathUtils.clip(image[i] * 255 + .5f, 0, 255);
        return ByteBuffer.wrap(out);
    }

    private static ShortBuffer mgn(ShortBuffer buf, int width, int height) {
        int size = width * height;
        float[] data = new float[size];

        short[] array = buf.array(); // always backed by array
        for (int i = 0; i < size; ++i)
            data[i] = ((array[i] + 65536) & 0xFFFF) / 65535f;

        float[] image = multiScale(data, width, height);

        short[] out = new short[size];
        for (int i = 0; i < size; ++i)
            out[i] = (short) MathUtils.clip(image[i] * 65535 + .5f, 0, 65535);
        return ShortBuffer.wrap(out);
    }

    static Buffer mgn(Buffer buf, int width, int height) throws Exception {
        if (buf instanceof ByteBuffer)
            return mgn((ByteBuffer) buf, width, height);
        else if (buf instanceof ShortBuffer)
            return mgn((ShortBuffer) buf, width, height);
        else
            throw new Exception("Unimplemented MGN filter");
    }

}
