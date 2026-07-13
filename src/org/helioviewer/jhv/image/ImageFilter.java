package org.helioviewer.jhv.image;

import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;
import org.helioviewer.jhv.thread.ParallelRange;

//import com.google.common.base.Stopwatch;

public class ImageFilter {

    public static final ImageFilter NONE = new ImageFilter(null);
    private static final ImageFilter MGN = new ImageFilter(new FilterMGN());
    private static final ImageFilter WOW = new ImageFilter(new FilterWOW());

    public enum Type {
        None("No filter"),
        MGN("Multi-scale Gaussian normalization"),
        WOW("Wavelet-optimized whitening"),
        RHEF("Radial histogram equalizing filter");

        public final String description;

        Type(String _description) {
            description = _description;
        }
    }

    interface Algorithm {
        float[] filter(float[] data, int width, int height);
    }

    private final Algorithm algorithm;

    private ImageFilter(Algorithm _algorithm) {
        algorithm = _algorithm;
    }

    public static ImageFilter of(Type type) {
        return switch (type) {
            case None -> NONE;
            case MGN -> MGN;
            case WOW -> WOW;
            case RHEF -> new ImageFilter(new FilterRHEF(null));
        };
    }

    public static ImageFilter of(Type type, Region imageRegion, MetaData metaData) {
        return type == Type.RHEF
                ? new ImageFilter(new FilterRHEF(SunCenteredRegion.fromImageRegion(imageRegion, metaData.getSunShift())))
                : of(type);
    }

    boolean isNone() {
        return algorithm == null;
    }

    private static final float BDIV = 1 / 255f;

    static byte[] filter(byte[] array, int width, int height, ImageFilter filter) {
        if (filter.isNone())
            return array;

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

        float[] image = filter.algorithm.filter(data, width, height);

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

    static short[] filterHalfFloat(short[] array, int width, int height, ImageFilter filter) {
        if (filter.isNone())
            return array;

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

        float[] image = filter.algorithm.filter(data, width, height);

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

}
