package org.helioviewer.jhv.base;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.helioviewer.jhv.math.Vec3;

public class BufferUtils {

    public static byte[] colorBytes(Color c) {
        return new byte[]{(byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue(), (byte) 255};
    }

    public static byte[] colorBytes(Color c, double alpha) {
        return new byte[]{(byte) (c.getRed() * alpha), (byte) (c.getGreen() * alpha), (byte) (c.getBlue() * alpha), (byte) (255 * alpha)};
    }

    public static byte[] colorBytes(int r, int g, int b) {
        return new byte[]{(byte) r, (byte) g, (byte) b, (byte) 255};
    }

    public static float[] colorFloat(Color c) {
        return new float[]{c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 1};
    }

    public static float[] colorFloat(Color c, double alpha) {
        return new float[]{(float) (c.getRed() * alpha / 255), (float) (c.getGreen() * alpha / 255), (float) (c.getBlue() * alpha / 255), (float) alpha};
    }

    public static final byte[] colorNull = {0, 0, 0, 0};
    public static final byte[] colorBlack = colorBytes(Color.BLACK);
    public static final byte[] colorRed = colorBytes(Color.RED);
    public static final byte[] colorGreen = colorBytes(Color.GREEN);
    public static final byte[] colorBlue = colorBytes(Color.BLUE);
    public static final byte[] colorWhite = colorBytes(Color.WHITE);
    public static final byte[] colorYellow = colorBytes(Color.YELLOW);
    public static final byte[] colorGray = colorBytes(Color.GRAY);
    public static final byte[] colorDarkGray = colorBytes(Color.DARK_GRAY);
    public static final byte[] colorLightGray = colorBytes(Color.LIGHT_GRAY);

    public static final float[] colorWhiteFloat = {1, 1, 1, 1};

    public static ByteBuffer newByteBuffer(int len) {
        return ByteBuffer.allocateDirect(len).order(ByteOrder.nativeOrder());
    }

    public static FloatBuffer newFloatBuffer(int len) {
        return newByteBuffer(4 * len).asFloatBuffer();
    }

    public static IntBuffer newIntBuffer(int len) {
        return newByteBuffer(4 * len).asIntBuffer();
    }

    public static ShortBuffer newShortBuffer(int len) {
        return newByteBuffer(2 * len).asShortBuffer();
    }

    public static void put4f(FloatBuffer buf, Vec3 v) {
        buf.put((float) v.x).put((float) v.y).put((float) v.z).put(1);
    }

    public static void put4f(FloatBuffer buf, float x, float y, float z, float w) {
        buf.put(x).put(y).put(z).put(w);
    }

}
