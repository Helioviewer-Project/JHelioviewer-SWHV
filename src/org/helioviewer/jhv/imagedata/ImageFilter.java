package org.helioviewer.jhv.imagedata;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import org.helioviewer.jhv.math.MathUtils;

public class ImageFilter {

    public enum Type {
        None("No filter", null), MGN("Multi-scale Gaussian normalization", new FilterMGN()), WOW("Wavelet-optimized whitening", new FilterWOW());

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

    private static ByteBuffer filter(ByteBuffer buf, int width, int height, Algorithm algorithm) {
        int size = width * height;
        float[] data = new float[size];

        byte[] array = buf.array(); // always backed by array
        for (int i = 0; i < size; ++i)
            data[i] = ((array[i] + 256) & 0xFF) / 255f;

        float[] image = algorithm.filter(data, width, height);

        byte[] out = new byte[size];
        for (int i = 0; i < size; ++i)
            out[i] = (byte) MathUtils.clip(image[i] * 255 + .5f, 0, 255);
        return ByteBuffer.wrap(out);
    }

    private static ShortBuffer filter(ShortBuffer buf, int width, int height, Algorithm algorithm) {
        int size = width * height;
        float[] data = new float[size];

        short[] array = buf.array(); // always backed by array
        for (int i = 0; i < size; ++i)
            data[i] = ((array[i] + 65536) & 0xFFFF) / 65535f;

        float[] image = algorithm.filter(data, width, height);

        short[] out = new short[size];
        for (int i = 0; i < size; ++i)
            out[i] = (short) MathUtils.clip(image[i] * 65535 + .5f, 0, 65535);
        return ShortBuffer.wrap(out);
    }

    static Buffer filter(Buffer buf, int width, int height, Type type) throws Exception {
        if (buf instanceof ByteBuffer)
            return filter((ByteBuffer) buf, width, height, type.algorithm);
        else if (buf instanceof ShortBuffer)
            return filter((ShortBuffer) buf, width, height, type.algorithm);
        else
            throw new Exception("Unimplemented data type filtering");
    }

}
