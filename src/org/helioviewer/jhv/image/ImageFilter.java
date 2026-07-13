package org.helioviewer.jhv.image;

import javax.annotation.Nullable;

import org.helioviewer.jhv.thread.ParallelRange;

//import com.google.common.base.Stopwatch;

public class ImageFilter {

    public enum Type {
        None("No filter", null),
        MGN("Multi-scale Gaussian normalization", new FilterMGN()),
        WOW("Wavelet-optimized whitening", new FilterWOW()),
        RHEF("Radial histogram equalizing filter", new FilterRHEF());

        public final String description;
        final Algorithm algorithm;

        Type(String _description, Algorithm _algorithm) {
            description = _description;
            algorithm = _algorithm;
        }
    }

    interface Algorithm {
        float[] filter(float[] data, int width, int height);
    }

    interface RegionAlgorithm extends Algorithm {
        @Override
        default float[] filter(float[] data, int width, int height) {
            return filter(data, width, height, null);
        }

        float[] filter(float[] data, int width, int height, @Nullable FilterRegion region);
    }

    private static final float BDIV = 1 / 255f;

    private static byte[] filter(byte[] array, int width, int height, Algorithm algorithm, @Nullable FilterRegion region) {
        int length = width * height;

        float[] data = new float[length];
        ParallelRange.run(height, (from, to) -> {
            for (int y = from; y < to; y++) {
                int rowBase = y * width;
                int rowEnd = rowBase + width;
                for (int idx = rowBase; idx < rowEnd; idx++) {
                    data[idx] = ((array[idx] + 256) & 0xFF) * BDIV;
                }
            }
        });

        float[] image = filter(data, width, height, algorithm, region);

        byte[] out = new byte[length];
        ParallelRange.run(height, (from, to) -> {
            for (int y = from; y < to; y++) {
                int rowBase = y * width;
                int rowEnd = rowBase + width;
                for (int idx = rowBase; idx < rowEnd; idx++) {
                    out[idx] = (byte) Math.clamp(image[idx] * 255 + .5f, 0, 255);
                }
            }
        });

        return out;
    }

    private static short[] filterHalfFloat(short[] array, int width, int height, Algorithm algorithm, @Nullable FilterRegion region) {
        int length = width * height;

        float[] data = new float[length];
        ParallelRange.run(height, (from, to) -> {
            for (int y = from; y < to; y++) {
                int rowBase = y * width;
                int rowEnd = rowBase + width;
                for (int idx = rowBase; idx < rowEnd; idx++) {
                    data[idx] = Float.float16ToFloat(array[idx]);
                }
            }
        });

        float[] image = filter(data, width, height, algorithm, region);

        short[] out = new short[length];
        ParallelRange.run(height, (from, to) -> {
            for (int y = from; y < to; y++) {
                int rowBase = y * width;
                int rowEnd = rowBase + width;
                for (int idx = rowBase; idx < rowEnd; idx++) {
                    out[idx] = Float.floatToFloat16(Math.clamp(image[idx], 0f, 1f));
                }
            }
        });

        return out;
    }

    private static float[] filter(float[] data, int width, int height, Algorithm algorithm, @Nullable FilterRegion region) {
        return algorithm instanceof RegionAlgorithm regionAlgorithm
                ? regionAlgorithm.filter(data, width, height, region)
                : algorithm.filter(data, width, height);
    }

    static byte[] filter(byte[] data, int width, int height, Type type, @Nullable FilterRegion region) {
        return type == Type.None ? data : filter(data, width, height, type.algorithm, region);
    }

    static short[] filterHalfFloat(short[] data, int width, int height, Type type, @Nullable FilterRegion region) {
        return type == Type.None ? data : filterHalfFloat(data, width, height, type.algorithm, region);
    }

}
