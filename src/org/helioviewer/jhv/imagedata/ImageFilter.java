package org.helioviewer.jhv.imagedata;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.stream.IntStream;

@SuppressWarnings("serial")
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

    private static final float BDIV = 1 / 255f, SDIV = 1 / 65535f;

    private static ByteBuffer filter(ByteBuffer buf, int width, int height, Algorithm algorithm) {
        int length = width * height;
        byte[] array = buf.array(); // always backed by array

        float[] data = new float[length];
        IntStream.range(0, length).parallel().forEach(i -> data[i] = ((array[i] + 256) & 0xFF) * BDIV);

        float[] image = algorithm.filter(data, width, height);

        byte[] out = new byte[length];
        IntStream.range(0, length).parallel().forEach(i -> out[i] = (byte) Math.clamp(image[i] * 255 + .5f, 0, 255));

        return ByteBuffer.wrap(out);
    }

    private static ShortBuffer filter(ShortBuffer buf, int width, int height, Algorithm algorithm) {
        int length = width * height;
        short[] array = buf.array(); // always backed by array

        float[] data = new float[length];
        IntStream.range(0, length).parallel().forEach(i -> data[i] = ((array[i] + 65536) & 0xFFFF) * SDIV);

        float[] image = algorithm.filter(data, width, height);

        short[] out = new short[length];
        IntStream.range(0, length).parallel().forEach(i -> out[i] = (short) Math.clamp(image[i] * 65535 + .5f, 0, 65535));

        return ShortBuffer.wrap(out);
    }

    static Buffer filter(Buffer buf, int width, int height, Type type) throws Exception {
        if (buf instanceof ByteBuffer bb)
            return filter(bb, width, height, type.algorithm);
        else if (buf instanceof ShortBuffer sb)
            return filter(sb, width, height, type.algorithm);
        else
            throw new Exception("Unimplemented data type filtering");
    }

}
