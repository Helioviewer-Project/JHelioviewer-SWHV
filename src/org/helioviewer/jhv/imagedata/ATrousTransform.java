package org.helioviewer.jhv.imagedata;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;

import org.helioviewer.jhv.math.MathUtils;

@SuppressWarnings("serial")
class ATrousTransform {

    private static final float[] FILTER = {1f / 16, 4f / 16, 6f / 16, 4f / 16, 1f / 16};
    private static final int THRESHOLD = 64; // Adjust based on image size and system

    static float[] decompose(float[] image, int width, int height, int levels) {
        float[] result = new float[image.length];
        float[] temp = image.clone();

        float[] wtemp1 = new float[image.length];
        float[] wtemp2 = new float[image.length];

        ForkJoinPool pool = ForkJoinPool.commonPool();
        for (int level = 0; level < levels; level++) {
            int step = (int) Math.pow(2, level);
            // Calculate wavelet coefficients
            pool.invoke(new ConvolutionTask(temp, result, width, height, true, step)); // Horizontal pass
            pool.invoke(new ConvolutionTask(result, temp, width, height, false, step)); // Vertical pass
            pool.invoke(new WaveletCoefficientTask(image, temp, result, 0, image.length));
            // Whiten coefficients
            pool.invoke(new SquareTask(result, wtemp1, 0, image.length));
            pool.invoke(new ConvolutionTask(wtemp1, wtemp2, width, height, true, step)); // Horizontal pass
            pool.invoke(new ConvolutionTask(wtemp2, wtemp1, width, height, false, step)); // Vertical pass
            pool.invoke(new ScaleTask(wtemp1, result, 0, image.length));
            // Update image for next level
            System.arraycopy(temp, 0, image, 0, image.length);
        }
        return result;
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

        private final float[] image;
        private final float[] temp;
        private final float[] result;
        private final int start;
        private final int end;

        WaveletCoefficientTask(float[] image, float[] temp, float[] result, int start, int end) {
            this.image = image;
            this.temp = temp;
            this.result = result;
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
                        new WaveletCoefficientTask(image, temp, result, start, mid),
                        new WaveletCoefficientTask(image, temp, result, mid, end));
            }
        }

        private void computeCoefficients() {
            for (int i = start; i < end; i++) {
                result[i] = image[i] - temp[i]; // Store wavelet coefficients
            }
        }

    }

    private static class SquareTask extends RecursiveAction {

        private final float[] src;
        private final float[] dest;
        private final int start;
        private final int end;

        SquareTask(float[] src, float[] dest, int start, int end) {
            this.src = src;
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
                        new SquareTask(src, dest, start, mid),
                        new SquareTask(src, dest, mid, end));
            }
        }

        private void computeSquare() {
            for (int i = start; i < end; i++) {
                dest[i] = src[i] * src[i];
            }
        }

    }

    private static class ScaleTask extends RecursiveAction {

        private final float[] src;
        private final float[] dest;
        private final int start;
        private final int end;

        ScaleTask(float[] src, float[] dest, int start, int end) {
            this.src = src;
            this.dest = dest;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (end - start <= THRESHOLD) {
                computeScale();
            } else {
                int mid = (start + end) / 2;
                invokeAll(
                        new ScaleTask(src, dest, start, mid),
                        new ScaleTask(src, dest, mid, end));
            }
        }

        private void computeScale() {
            for (int i = start; i < end; i++) {
                dest[i] *= MathUtils.invSqrt(src[i]);
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
