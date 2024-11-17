package org.helioviewer.jhv.imagedata;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;
import java.util.Arrays;

import org.helioviewer.jhv.math.MathUtils;

@SuppressWarnings("serial")
class FilterWOW implements ImageFilter.Algorithm {

    private static final int SCALES = 6;
    private static final float MIX_FACTOR = 0.99f;
    private static final float[] FILTER = {1f / 16, 4f / 16, 6f / 16, 4f / 16, 1f / 16};
    private static final float SIGMA_E0 = 8.907e-1f;
    private static final float SIGMA_E1 = 2.0072e-1f;
    private static final float BOOST = 1.5f; // restore some crispness after denoising

    private static final ArrayOp opSubtract = (op1, op2, dest, start, end) -> {
        for (int i = start; i < end; i++) {
            dest[i] = op1[i] - op2[i];
        }
    };

    private static final ArrayOp opDenoise = (op1, op2, dest, start, end) -> {
        for (int i = start; i < end; i++) {
            float x = Math.abs(dest[i]) * op1[0];
            dest[i] *= BOOST * x / (1 + x);
        }
    };

    private static final ArrayOp opSynthesis = (op1, op2, dest, start, end) -> {
        for (int i = start; i < end; i++) {
            dest[i] += MathUtils.invSqrt(op1[i]) * op2[i];
        }
    };

    private static final ArrayOp opMix = (op1, op2, dest, start, end) -> {
        for (int i = start; i < end; i++) {
            dest[i] = (1 - MIX_FACTOR) * (dest[i] + op1[i]) + MIX_FACTOR * op2[i];
        }
    };

    @Override
    public float[] filter(float[] data, int width, int height) {
        return filter(data, width, height, false);
    }

    protected float[] filter(float[] data, int width, int height, boolean denoise) {
        float[] image = data.clone();
        float[] temp = data.clone();

        int length = data.length;
        float[] coeff = new float[length];
        float[] wtemp1 = new float[length];
        float[] wtemp2 = new float[length];
        float[] synth = new float[length];

        float noise = 0; // computed for scale 0
        ForkJoinPool pool = ForkJoinPool.commonPool();
        for (int scale = 0; scale < SCALES; scale++) {
            int step = 1 << scale;
            // Calculate wavelet coefficients
            pool.invoke(new ConvolutionTask(temp, coeff, width, height, true, step)); // Horizontal pass
            pool.invoke(new ConvolutionTask(coeff, temp, width, height, false, step)); // Vertical pass
            pool.invoke(new ArrayOp.TaskTwo(image, temp, coeff, 0, length, opSubtract)); // Coefficients
            // Update image for next scale
            System.arraycopy(temp, 0, image, 0, length);
            // Whiten coefficients
            pool.invoke(new ConvolutionTask2(coeff, wtemp1, width, height, true, step)); // Squared src horizontal pass
            pool.invoke(new ConvolutionTask(wtemp1, wtemp2, width, height, false, step)); // Vertical pass

            if (denoise) {
                if (scale == 0) {
                    noise = computeNoise(coeff, length);
                    float[] div = new float[]{1 / (3 * SIGMA_E0 * noise)};
                    pool.invoke(new ArrayOp.TaskTwo(div, null, coeff, 0, length, opDenoise));
                } else if (scale == 1) {
                    float[] div = new float[]{1 / (1 * SIGMA_E1 * noise)};
                    pool.invoke(new ArrayOp.TaskTwo(div, null, coeff, 0, length, opDenoise));
                }
            }
            pool.invoke(new ArrayOp.TaskTwo(wtemp2, coeff, synth, 0, length, opSynthesis)); // Whitened synthesis
        }
        pool.invoke(new ArrayOp.TaskTwo(image, data, synth, 0, length, opMix));
        return synth;
    }

    private static float computeNoise(float[] c, int length) {
        float[] w = new float[length];
        for (int i = 0; i < length; i++)
            w[i] = Math.abs(c[i]);
        Arrays.parallelSort(w);
        return w[length / 2] * 1.48260221850560f / SIGMA_E0;
    }

    private static class ConvolutionTask extends RecursiveAction {

        protected final float[] src;
        protected final float[] dest;
        protected final int width;
        protected final int height;
        protected final boolean isHorizontal;
        protected final int step;
        protected final int start;
        protected final int end;

        ConvolutionTask(float[] src, float[] dest, int width, int height, boolean isHorizontal, int step) {
            this(src, dest, width, height, isHorizontal, step, 0, isHorizontal ? height : width);
        }

        protected ConvolutionTask(float[] src, float[] dest, int width, int height, boolean isHorizontal, int step, int start, int end) {
            this.src = src;
            this.dest = dest;
            this.width = width;
            this.height = height;
            this.isHorizontal = isHorizontal;
            this.step = step;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (end - start <= ArrayOp.THRESHOLD) {
                if (isHorizontal) {
                    computeHorizontal();
                } else {
                    computeVertical();
                }
            } else {
                int mid = (start + end) / 2;
                invokeAll(
                        new ConvolutionTask(src, dest, width, height, isHorizontal, step, start, mid),
                        new ConvolutionTask(src, dest, width, height, isHorizontal, step, mid, end));
            }
        }

        protected void computeHorizontal() {
            for (int y = start; y < end; y++) {
                for (int x = 0; x < width; x++) {
                    float sum = 0;
                    for (int i = -2; i <= 2; i++) {
                        int idx = mirroredIdx(x + i * step, width);
                        sum += src[y * width + idx] * FILTER[i + 2];
                    }
                    dest[y * width + x] = sum;
                }
            }
        }

        protected void computeVertical() {
            for (int x = start; x < end; x++) {
                for (int y = 0; y < height; y++) {
                    float sum = 0;
                    for (int i = -2; i <= 2; i++) {
                        int idx = mirroredIdx(y + i * step, height);
                        sum += src[idx * width + x] * FILTER[i + 2];
                    }
                    dest[y * width + x] = sum;
                }
            }
        }

    }

    private static class ConvolutionTask2 extends ConvolutionTask {

        ConvolutionTask2(float[] src, float[] dest, int width, int height, boolean isHorizontal, int step) {
            super(src, dest, width, height, isHorizontal, step);
        }

        protected ConvolutionTask2(float[] src, float[] dest, int width, int height, boolean isHorizontal, int step, int start, int end) {
            super(src, dest, width, height, isHorizontal, step, start, end);
        }

        @Override
        protected void compute() {
            if (end - start <= ArrayOp.THRESHOLD) {
                if (isHorizontal) {
                    computeHorizontal();
                } else {
                    super.computeVertical();
                }
            } else {
                int mid = (start + end) / 2;
                invokeAll(
                        new ConvolutionTask2(src, dest, width, height, isHorizontal, step, start, mid),
                        new ConvolutionTask2(src, dest, width, height, isHorizontal, step, mid, end));
            }
        }

        @Override
        protected void computeHorizontal() {
            for (int y = start; y < end; y++) {
                for (int x = 0; x < width; x++) {
                    float sum = 0;
                    for (int i = -2; i <= 2; i++) {
                        int idx = mirroredIdx(x + i * step, width);
                        float v = src[y * width + idx];
                        sum += v * v * FILTER[i + 2];
                    }
                    dest[y * width + x] = sum;
                }
            }
        }

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
