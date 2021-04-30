package org.helioviewer.jhv.imagedata;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.ArrayList;

import org.helioviewer.jhv.math.MathUtils;

// derived from http://dev.ipol.im/~getreuer/code/
public class ImageFilter {

    private static final int SII_MIN_K = 3;
    private static final int SII_MAX_K = 5;

    private static final double sigma0 = 100.0 / Math.PI;
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

    public ImageFilter(double sigma, int _K, int N) {
        K = _K;

        int i = K - SII_MIN_K;
        double sum = 0;

        for (int k = 0; k < K; ++k) {
            radii[k] = (int) (radii0[i][k] * (sigma / sigma0) + 0.5);
            sum += weights0[i][k] * (2 * radii[k] + 1);
        }

        for (int k = 0; k < K; ++k)
            weights[k] = (float) (weights0[i][k] / sum);

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
        for (int n = -pad; n < N + pad; ++n) {
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

    private void gaussianConvImage(float[] dst, float[] src, int width, int height) {
        // Filter each row
        for (int y = 0; y < height; ++y)
            gaussianConv(dst, src, width, 1, width * y);
        // Filter each column
        for (int x = 0; x < width; ++x)
            gaussianConv(dst, dst, height, width, x);
    }

    private static final int _K = 3;
    private static final float H = 0.7f;
    private static final double KA = 0.7;
    private static final double[] sigmas = {1.25, 2.5, 5, 10, 20, 40};

    @SuppressWarnings("serial")
    private static class ScaleTask extends RecursiveTask<float[]> {

        private final float[] data;
        private final int width;
        private final int height;
        private final int size;
        private final int N;
        private final double sigma;

        ScaleTask(float[] _data, int _width, int _height, double _sigma) {
            data = _data;
            width = _width;
            height = _height;
            size = width * height;
            N = Math.max(width, height);
            sigma = _sigma;
        }

        @Override
        protected float[] compute() {
            ImageFilter filter = new ImageFilter(sigma, _K, N);

            float[] conv = new float[size];
            float[] conv2 = new float[size];

            filter.gaussianConvImage(conv, data, width, height);
            for (int i = 0; i < size; ++i) {
                float v = data[i] - conv[i];
                conv[i] = v;
                conv2[i] = v * v;
            }
            filter.gaussianConvImage(conv2, conv2, width, height);

            for (int i = 0; i < size; ++i) {
                double v = Math.sqrt(conv2[i]);
                if (v == 0)
                    v = 1;
                conv[i] = (float) Math.atan(KA * conv[i] / v) * (1 - H) / sigmas.length;
            }

            return conv;
        }

    }

    private static float[] multiScale2(float[] data, int width, int height) {
        ArrayList<ForkJoinTask<float[]>> tasks = new ArrayList<>(sigmas.length);
        for (double sigma : sigmas)
            tasks.add(new ScaleTask(data, width, height, sigma).fork());

        int size = width * height;
        float[] image = new float[size];
        for (ForkJoinTask<float[]> task : tasks) {
            float[] res = task.join();
            for (int i = 0; i < size; ++i)
                image[i] += res[i];
        }

        for (int i = 0; i < size; ++i) {
            image[i] += H * data[i];
        }
        return image;
    }

    private static float[] multiScale(float[] data, int width, int height) {
        int N = Math.max(width, height);
        int size = width * height;
        float[] conv = new float[size];
        float[] conv2 = new float[size];
        float[] image = new float[size];

        for (double sigma : sigmas) {
            ImageFilter filter = new ImageFilter(sigma, _K, N);

            filter.gaussianConvImage(conv, data, width, height);
            for (int i = 0; i < size; ++i) {
                float v = data[i] - conv[i];
                conv[i] = v;
                conv2[i] = v * v;
            }
            filter.gaussianConvImage(conv2, conv2, width, height);

            for (int i = 0; i < size; ++i) {
                double v = Math.sqrt(conv2[i]);
                if (v == 0)
                    v = 1;
                image[i] += (float) Math.atan(KA * conv[i] / v);
            }
        }

        for (int i = 0; i < size; ++i) {
            image[i] = (1 - H) * (image[i] / sigmas.length) + H * data[i];
        }

        return image;
    }

    public static ByteBuffer mgn(ByteBuffer buf, int width, int height) {
        int size = width * height;
        float[] data = new float[size];

        byte[] array = buf.array(); // always backed by array
        for (int i = 0; i < size; ++i)
            data[i] = ((array[i] + 256) & 0xFF) / 255f;

        float[] image = multiScale(data, width, height);

        ByteBuffer ret = ByteBuffer.allocate(size);
        for (int i = 0; i < size; ++i)
            ret.put((byte) MathUtils.clip(image[i] * 255 + .5f, 0, 255));
        return ret.rewind();
    }

    public static ShortBuffer mgn(ShortBuffer buf, int width, int height) {
        int size = width * height;
        float[] data = new float[size];

        short[] array = buf.array(); // always backed by array
        for (int i = 0; i < size; ++i)
            data[i] = ((array[i] + 65536) & 0xFFFF) / 65535f;

        float[] image = multiScale2(data, width, height);

        ShortBuffer ret = ShortBuffer.allocate(size);
        for (int i = 0; i < size; ++i) {
            ret.put((short) MathUtils.clip(image[i] * 65535 + .5f, 0, 65535));
        }
        return ret.rewind();
    }

}
