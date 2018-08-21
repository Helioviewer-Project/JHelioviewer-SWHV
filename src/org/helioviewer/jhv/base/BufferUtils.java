package org.helioviewer.jhv.base;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.helioviewer.jhv.math.Vec3;

public class BufferUtils {

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

    public static void put2f(FloatBuffer buf, float[] f) {
        buf.put(f[0]).put(f[1]);
    }

    public static void put2f(FloatBuffer buf, float f0, float f1) {
        buf.put(f0).put(f1);
    }

}
