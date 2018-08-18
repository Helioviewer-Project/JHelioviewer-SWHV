package org.helioviewer.jhv.base;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.helioviewer.jhv.math.Vec3;

public class BufferUtils {

    public static final float[] colorNull = {0, 0, 0, 0};
    public static final float[] colorBlack = {0, 0, 0, 1};
    public static final byte[] colorBlackByte = {(byte) Color.BLACK.getRed(), (byte) Color.BLACK.getGreen(), (byte) Color.BLACK.getBlue(), (byte) 255};

    public static final float[] colorRed = {Color.RED.getRed() / 255f, Color.RED.getGreen() / 255f, Color.RED.getBlue() / 255f, 1};
    public static final byte[] colorRedByte = {(byte) Color.RED.getRed(), (byte) Color.RED.getGreen(), (byte) Color.RED.getBlue(), (byte) 255};

    public static final float[] colorGreen = {Color.GREEN.getRed() / 255f, Color.GREEN.getGreen() / 255f, Color.GREEN.getBlue() / 255f, 1};
    public static final float[] colorBlue = {Color.BLUE.getRed() / 255f, Color.BLUE.getGreen() / 255f, Color.BLUE.getBlue() / 255f, 1};
    public static final byte[] colorBlueByte = {(byte) Color.BLUE.getRed(), (byte) Color.BLUE.getGreen(), (byte) Color.BLUE.getBlue(), (byte) 255};

    public static final float[] colorWhite = {Color.WHITE.getRed() / 255f, Color.WHITE.getGreen() / 255f, Color.WHITE.getBlue() / 255f, 1};
    public static final float[] colorYellow = {Color.YELLOW.getRed() / 255f, Color.YELLOW.getGreen() / 255f, Color.YELLOW.getBlue() / 255f, 1};
    public static final byte[] colorYellowByte = {(byte) Color.YELLOW.getRed(), (byte) Color.YELLOW.getGreen(), (byte) Color.YELLOW.getBlue(), (byte) 255};

    public static final float[] colorDarkGray = {Color.DARK_GRAY.getRed() / 255f, Color.DARK_GRAY.getGreen() / 255f, Color.DARK_GRAY.getBlue() / 255f, 1f};

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
