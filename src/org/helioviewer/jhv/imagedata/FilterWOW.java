package org.helioviewer.jhv.imagedata;

import java.util.OptionalDouble;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

import org.helioviewer.jhv.math.MathUtils;

@SuppressWarnings("serial")
class FilterWOW implements ImageFilter.Algorithm {

    private interface ArrayOp {

        void accept(float[] arg1, float[] arg2, float[] arg3, int start, int end);

        int THRESHOLD = 64; // Adjust based on image size and system

        class Task3 extends RecursiveAction {

            private final float[] arg1;
            private final float[] arg2;
            private final float[] arg3;
            private final int start;
            private final int end;
            private final ArrayOp op;

            Task3(float[] arg1, float[] arg2, float[] arg3, int start, int end, ArrayOp op) {
                this.arg1 = arg1;
                this.arg2 = arg2;
                this.arg3 = arg3;
                this.start = start;
                this.end = end;
                this.op = op;
            }

            @Override
            protected void compute() {
                if (end - start <= THRESHOLD) {
                    op.accept(arg1, arg2, arg3, start, end);
                } else {
                    int mid = (start + end) / 2;
                    invokeAll(
                            new Task3(arg1, arg2, arg3, start, mid, op),
                            new Task3(arg1, arg2, arg3, mid, end, op));
                }
            }

        }

    }

    private static final int SCALES = 6;
    private static final float MIX_FACTOR = 0.99f;
    private static final float ONE_MINUS_MIX_FACTOR = 1f - MIX_FACTOR;
    private static final float[] FILTER = {1f / 16, 4f / 16, 6f / 16, 4f / 16, 1f / 16};
    private static final float SIGMA_E0 = 8.907e-1f;
    private static final float SIGMA_E1 = 2.0072e-1f;
    private static final float BOOST = 1.5f; // restore some crispness after denoising
    private static final float NOISE_THRESH = 1e-10f; // avoid denoising very low noise or blank images

    private static final ArrayOp opSubtract = (op1, op2, dest, start, end) -> {
        for (int i = start; i < end; i++) {
            dest[i] = op1[i] - op2[i];
        }
    };

    private static final ArrayOp opDenoise = (op1, op2, dest, start, end) -> {
        float factor = op1[0];
        for (int i = start; i < end; i++) {
            float x = Math.abs(dest[i]) * factor;
            dest[i] *= BOOST * x / (1 + x);
        }
    };

    private static final ArrayOp opSynthesis = (op1, op2, dest, start, end) -> {
        for (int i = start; i < end; i++) {
            dest[i] += MathUtils.invSqrt(op1[i]) * op2[i];
        }
    };

    private static final ArrayOp opBlend = (op1, op2, dest, start, end) -> {
        for (int i = start; i < end; i++) {
            dest[i] = (dest[i] + op1[i]) * ONE_MINUS_MIX_FACTOR + op2[i] * MIX_FACTOR;
        }
    };

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
        ForkJoinPool pool = ForkJoinPool.commonPool();
        for (int scale = 0; scale < SCALES; scale++) {
            int step = 1 << scale;
            // A trous transform
            convolveHorizontal(image, coeff, width, height, step); // Horizontal pass
            convolveVertical(coeff, temp1, width, height, step); // Vertical pass
            pool.invoke(new ArrayOp.Task3(image, temp1, coeff, 0, length, opSubtract)); // Coefficients
            System.arraycopy(temp1, 0, image, 0, length); // Update image for next scale
            // Whiten coefficients
            convolveHorizontalSquared(coeff, temp1, width, height, step); // Squared src horizontal pass
            convolveVertical(temp1, temp2, width, height, step); // Vertical pass
            // Denoise stage
            if (scale == 0) {
                noise = (1.48260221850560f / SIGMA_E0) * medianStream(coeff, length);
                if (noise > NOISE_THRESH) { // avoid division by 0
                    denoiseFactor[0] = 1 / (3 * SIGMA_E0 * noise);
                    pool.invoke(new ArrayOp.Task3(denoiseFactor, null, coeff, 0, length, opDenoise));
                }
            } else if (scale == 1) {
                if (noise > NOISE_THRESH) { // avoid division by 0
                    denoiseFactor[0] = 1 / (SIGMA_E1 * noise);
                    pool.invoke(new ArrayOp.Task3(denoiseFactor, null, coeff, 0, length, opDenoise));
                }
            }
            // Whitened synthesis
            pool.invoke(new ArrayOp.Task3(temp2, coeff, synth, 0, length, opSynthesis));
        }
        pool.invoke(new ArrayOp.Task3(image, data, synth, 0, length, opBlend));
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

    private static void convolveHorizontal(float[] src, float[] dest, int width, int height, int step) {
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowBase = y * width;
            for (int x = 0; x < width; x++) {
                float sum = 0;
                int idx_m2 = mirroredIdx(x - 2 * step, width);
                sum += src[rowBase + idx_m2] * FILTER[0];
                int idx_m1 = mirroredIdx(x - step, width);
                sum += src[rowBase + idx_m1] * FILTER[1];
                sum += src[rowBase + x] * FILTER[2];
                int idx_p1 = mirroredIdx(x + step, width);
                sum += src[rowBase + idx_p1] * FILTER[3];
                int idx_p2 = mirroredIdx(x + 2 * step, width);
                sum += src[rowBase + idx_p2] * FILTER[4];
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
                sum += v_m2 * v_m2 * FILTER[0];
                int idx_m1 = mirroredIdx(x - step, width);
                float v_m1 = src[rowBase + idx_m1];
                sum += v_m1 * v_m1 * FILTER[1];
                float v0 = src[rowBase + x];
                sum += v0 * v0 * FILTER[2];
                int idx_p1 = mirroredIdx(x + step, width);
                float v_p1 = src[rowBase + idx_p1];
                sum += v_p1 * v_p1 * FILTER[3];
                int idx_p2 = mirroredIdx(x + 2 * step, width);
                float v_p2 = src[rowBase + idx_p2];
                sum += v_p2 * v_p2 * FILTER[4];
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
                sum += src[rowM2 + x] * FILTER[0];
                sum += src[rowM1 + x] * FILTER[1];
                sum += src[rowBase + x] * FILTER[2];
                sum += src[rowP1 + x] * FILTER[3];
                sum += src[rowP2 + x] * FILTER[4];
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
