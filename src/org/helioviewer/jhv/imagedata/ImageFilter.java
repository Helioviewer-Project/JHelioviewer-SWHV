package org.helioviewer.jhv.imagedata;

import java.util.stream.IntStream;

//import com.google.common.base.Stopwatch;

public class ImageFilter {

    public enum Type {
        None("No filter", null),
        MGN("Multi-scale Gaussian normalization", new FilterMGN()),
        WOW("Wavelet-optimized whitening", new FilterWOW());

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

    private static final float BDIV = 1 / 255f, SDIV = 1 / 65535f;

    private static byte[] filter(byte[] array, int width, int height, Algorithm algorithm) {
        int length = width * height;

        float[] data = new float[length];
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowBase = y * width;
            int rowEnd = rowBase + width;
            for (int idx = rowBase; idx < rowEnd; idx++) {
                data[idx] = ((array[idx] + 256) & 0xFF) * BDIV;
            }
        });

        float[] image = algorithm.filter(data, width, height);

        byte[] out = new byte[length];
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowBase = y * width;
            int rowEnd = rowBase + width;
            for (int idx = rowBase; idx < rowEnd; idx++) {
                out[idx] = (byte) Math.clamp(image[idx] * 255 + .5f, 0, 255);
            }
        });

        return out;
    }

    private static short[] filter(short[] array, int width, int height, Algorithm algorithm) {
        int length = width * height;

        float[] data = new float[length];
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowBase = y * width;
            int rowEnd = rowBase + width;
            for (int idx = rowBase; idx < rowEnd; idx++) {
                data[idx] = ((array[idx] + 65536) & 0xFFFF) * SDIV;
            }
        });

        float[] image = algorithm.filter(data, width, height);

        short[] out = new short[length];
        IntStream.range(0, height).parallel().forEach(y -> {
            int rowBase = y * width;
            int rowEnd = rowBase + width;
            for (int idx = rowBase; idx < rowEnd; idx++) {
                out[idx] = (short) Math.clamp(image[idx] * 65535 + .5f, 0, 65535);
            }
        });

        return out;
    }

    static byte[] filter(byte[] data, int width, int height, Type type) {
        return type == Type.None ? data : filter(data, width, height, type.algorithm);
    }

    static short[] filter(short[] data, int width, int height, Type type) {
        return type == Type.None ? data : filter(data, width, height, type.algorithm);
    }

}
