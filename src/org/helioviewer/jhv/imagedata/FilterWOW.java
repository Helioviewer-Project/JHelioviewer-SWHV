package org.helioviewer.jhv.imagedata;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;

import org.helioviewer.jhv.math.MathUtils;

@SuppressWarnings("serial")
class FilterWOW implements ImageFilter.Algorithm {

    private static final int LEVELS = 6;
    private static final float MIX_FACTOR = 0.99f;
    private static final float[] FILTER = {1f / 16, 4f / 16, 6f / 16, 4f / 16, 1f / 16};

    private static final ArrayOp.Two opMix = (op1, op2, dest, start, end) -> {
        for (int i = start; i < end; i++) {
            dest[i] = (1 - MIX_FACTOR) * (dest[i] + op1[i]) + MIX_FACTOR * op2[i];
        }
    };

    private static final ArrayOp.Two opSynthesis = (op1, op2, dest, start, end) -> {
        for (int i = start; i < end; i++) {
            dest[i] += MathUtils.invSqrt(op1[i]) * op2[i];
        }
    };

    private static final ArrayOp.Two opCoefficients = (op1, op2, dest, start, end) -> {
        for (int i = start; i < end; i++) {
            dest[i] = op1[i] - op2[i];
        }
    };

    private static final ArrayOp.One opSquare = (op1, dest, start, end) -> {
        for (int i = start; i < end; i++) {
            dest[i] = op1[i] * op1[i];
        }
    };

    @Override
    public float[] filter(float[] data, int width, int height) {
        float[] image = data.clone();
        float[] temp = data.clone();

        int length = data.length;
        float[] result = new float[length];
        float[] wtemp1 = new float[length];
        float[] wtemp2 = new float[length];
        float[] recon = new float[length];

        ForkJoinPool pool = ForkJoinPool.commonPool();
        for (int level = 0; level < LEVELS; level++) {
            int step = 1 << level;
            // Calculate wavelet coefficients
            pool.invoke(new ConvolutionTask(temp, result, width, height, true, step)); // Horizontal pass
            pool.invoke(new ConvolutionTask(result, temp, width, height, false, step)); // Vertical pass
            pool.invoke(new ArrayOp.TaskTwo(image, temp, result, 0, length, opCoefficients));
            // Update image for next level
            System.arraycopy(temp, 0, image, 0, length);
            // Whiten coefficients
            pool.invoke(new ArrayOp.TaskOne(result, wtemp1, 0, length, opSquare));
            pool.invoke(new ConvolutionTask(wtemp1, wtemp2, width, height, true, step)); // Horizontal pass
            pool.invoke(new ConvolutionTask(wtemp2, wtemp1, width, height, false, step)); // Vertical pass
            pool.invoke(new ArrayOp.TaskTwo(wtemp1, result, recon, 0, length, opSynthesis)); // Weighted synthesis
        }
        pool.invoke(new ArrayOp.TaskTwo(image, data, recon, 0, length, opMix));
        return recon;
    }

    private static class ConvolutionTask extends RecursiveAction {

        private final float[] src;
        private final float[] dest;
        private final int width;
        private final int height;
        private final boolean isHorizontal;
        private final int step;
        private final int start;
        private final int end;

        ConvolutionTask(float[] src, float[] dest, int width, int height, boolean isHorizontal, int step) {
            this(src, dest, width, height, isHorizontal, step, 0, isHorizontal ? height : width);
        }

        private ConvolutionTask(float[] src, float[] dest, int width, int height, boolean isHorizontal, int step, int start, int end) {
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

        private void computeHorizontal() {
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

        private void computeVertical() {
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

    private static int mirroredIdx(int idx, int size) {
        if (idx < 0) {
            return -idx;
        } else if (idx >= size) {
            return 2 * size - idx - 2;
        }
        return idx;
    }

}
