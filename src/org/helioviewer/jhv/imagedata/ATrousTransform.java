package org.helioviewer.jhv.imagedata;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;

import org.helioviewer.jhv.math.MathUtils;

@SuppressWarnings("serial")
class ATrousTransform {

    private static final float[] FILTER = {1f / 16, 4f / 16, 6f / 16, 4f / 16, 1f / 16};
    private static final float H = 0.99f;
    private static final int THRESHOLD = 64; // Adjust based on image size and system

    static float[] decompose(float[] in, int width, int height, int levels) {
        float[] image = in.clone();
        float[] temp = in.clone();

        int length = in.length;
        float[] result = new float[length];
        float[] wtemp1 = new float[length];
        float[] wtemp2 = new float[length];
        float[] recon = new float[length];

        ForkJoinPool pool = ForkJoinPool.commonPool();
        for (int level = 0; level < levels; level++) {
            int step = 1 << level;
            // Calculate wavelet coefficients
            pool.invoke(new ConvolutionTask(temp, result, width, height, true, step)); // Horizontal pass
            pool.invoke(new ConvolutionTask(result, temp, width, height, false, step)); // Vertical pass
            pool.invoke(new WaveletCoefficientTask(image, temp, result, 0, length));
            // Update image for next level
            System.arraycopy(temp, 0, image, 0, length);
            // Whiten coefficients
            pool.invoke(new SquareTask(result, wtemp1, 0, length));
            pool.invoke(new ConvolutionTask(wtemp1, wtemp2, width, height, true, step)); // Horizontal pass
            pool.invoke(new ConvolutionTask(wtemp2, wtemp1, width, height, false, step)); // Vertical pass
            pool.invoke(new SynthesisTask(wtemp1, result, recon, 0, length)); // Weighted synthesis
        }
        pool.invoke(new MixTask(image, in, recon, 0, length));
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
            if (end - start <= THRESHOLD) {
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
                        int idx = getMirroredIndex(x + i * step, width);
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
                        int idx = getMirroredIndex(y + i * step, height);
                        sum += src[idx * width + x] * FILTER[i + 2];
                    }
                    dest[y * width + x] = sum;
                }
            }
        }

    }

    private static class WaveletCoefficientTask extends RecursiveAction {

        private final float[] op1;
        private final float[] op2;
        private final float[] dest;
        private final int start;
        private final int end;

        WaveletCoefficientTask(float[] op1, float[] op2, float[] dest, int start, int end) {
            this.op1 = op1;
            this.op2 = op2;
            this.dest = dest;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (end - start <= THRESHOLD) {
                computeCoefficients();
            } else {
                int mid = (start + end) / 2;
                invokeAll(
                        new WaveletCoefficientTask(op1, op2, dest, start, mid),
                        new WaveletCoefficientTask(op1, op2, dest, mid, end));
            }
        }

        private void computeCoefficients() {
            for (int i = start; i < end; i++) {
                dest[i] = op1[i] - op2[i]; // Store wavelet coefficients
            }
        }

    }

    private static class SquareTask extends RecursiveAction {

        private final float[] op;
        private final float[] dest;
        private final int start;
        private final int end;

        SquareTask(float[] op, float[] dest, int start, int end) {
            this.op = op;
            this.dest = dest;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (end - start <= THRESHOLD) {
                computeSquare();
            } else {
                int mid = (start + end) / 2;
                invokeAll(
                        new SquareTask(op, dest, start, mid),
                        new SquareTask(op, dest, mid, end));
            }
        }

        private void computeSquare() {
            for (int i = start; i < end; i++) {
                dest[i] = op[i] * op[i];
            }
        }

    }

    private static class SynthesisTask extends RecursiveAction {

        private final float[] op1;
        private final float[] op2;
        private final float[] dest;
        private final int start;
        private final int end;

        SynthesisTask(float[] op1, float[] op2, float[] dest, int start, int end) {
            this.op1 = op1;
            this.op2 = op2;
            this.dest = dest;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (end - start <= THRESHOLD) {
                computeSynthesis();
            } else {
                int mid = (start + end) / 2;
                invokeAll(
                        new SynthesisTask(op1, op2, dest, start, mid),
                        new SynthesisTask(op1, op2, dest, mid, end));
            }
        }

        private void computeSynthesis() {
            for (int i = start; i < end; i++) {
                dest[i] += MathUtils.invSqrt(op1[i]) * op2[i];
            }
        }

    }

    private static class MixTask extends RecursiveAction {

        private final float[] op1;
        private final float[] op2;
        private final float[] dest;
        private final int start;
        private final int end;

        MixTask(float[] op1, float[] op2, float[] dest, int start, int end) {
            this.op1 = op1;
            this.op2 = op2;
            this.dest = dest;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (end - start <= THRESHOLD) {
                computeMix();
            } else {
                int mid = (start + end) / 2;
                invokeAll(
                        new MixTask(op1, op2, dest, start, mid),
                        new MixTask(op1, op2, dest, mid, end));
            }
        }

        private void computeMix() {
            for (int i = start; i < end; i++) {
                dest[i] = (1 - H) * (dest[i] + op1[i]) + H * op2[i];
            }
        }

    }

    private static int getMirroredIndex(int index, int size) {
        if (index < 0) {
            return Math.abs(index);
        } else if (index >= size) {
            return 2 * size - index - 2;
        }
        return index;
    }

}
