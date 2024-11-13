package org.helioviewer.jhv.imagedata;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import org.helioviewer.jhv.math.MathUtils;

public class ImageFilter {

    public enum Type {None, WOW, MGN}

    private static ByteBuffer mgn(ByteBuffer buf, int width, int height) {
        int size = width * height;
        float[] data = new float[size];

        byte[] array = buf.array(); // always backed by array
        for (int i = 0; i < size; ++i)
            data[i] = ((array[i] + 256) & 0xFF) / 255f;

        float[] image = FilterMGN.filter(data, width, height);

        byte[] out = new byte[size];
        for (int i = 0; i < size; ++i)
            out[i] = (byte) MathUtils.clip(image[i] * 255 + .5f, 0, 255);
        return ByteBuffer.wrap(out);
    }

    private static ShortBuffer mgn(ShortBuffer buf, int width, int height) {
        int size = width * height;
        float[] data = new float[size];

        short[] array = buf.array(); // always backed by array
        for (int i = 0; i < size; ++i)
            data[i] = ((array[i] + 65536) & 0xFFFF) / 65535f;

        float[] image = FilterMGN.filter(data, width, height);

        short[] out = new short[size];
        for (int i = 0; i < size; ++i)
            out[i] = (short) MathUtils.clip(image[i] * 65535 + .5f, 0, 65535);
        return ShortBuffer.wrap(out);
    }

    static Buffer mgn(Buffer buf, int width, int height) throws Exception {
        if (buf instanceof ByteBuffer)
            return mgn((ByteBuffer) buf, width, height);
        else if (buf instanceof ShortBuffer)
            return mgn((ShortBuffer) buf, width, height);
        else
            throw new Exception("Unimplemented MGN filter");
    }

}
