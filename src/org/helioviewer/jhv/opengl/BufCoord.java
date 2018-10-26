package org.helioviewer.jhv.opengl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import org.helioviewer.jhv.math.Vec3;

public class BufCoord {

    private static final int chunk = 1024;
    private int multiplier = 1;

    private final byte[] byteLast = new byte[16];
    private final FloatBuffer bufferLast = ByteBuffer.wrap(byteLast).order(ByteOrder.nativeOrder()).asFloatBuffer();

    private byte[] array;
    private int length;
    private int floats;

    public BufCoord(int size) {
        array = new byte[size < 16 ? 16 : size];
    }

    private void ensure(int nbytes) {
        int size = array.length;
        if (length + nbytes > size) {
            array = Arrays.copyOf(array, size + chunk * multiplier++);
        }
    }

    public void putCoord(float x, float y, float z, float w, float c0, float c1) {
        put4f(x, y, z, w);
        put2f(c0, c1);
    }

    public void putCoord(float x, float y, float z, float w, float[] c) {
        put4f(x, y, z, w);
        put2f(c[0], c[1]);
    }

    public void putCoord(Vec3 v, float[] c) {
        put4f((float) v.x, (float) v.y, (float) v.z, 1);
        put2f(c[0], c[1]);
    }

    private void put4f(float x, float y, float z, float w) {
        ensure(16);
        bufferLast.put(0, x).put(1, y).put(2, z).put(3, w);
        System.arraycopy(byteLast, 0, array, length, 16);
        length += 16;
        floats += 4;
    }

    private void put2f(float f0, float f1) {
        ensure(8);
        bufferLast.put(2, f0).put(3, f1);
        System.arraycopy(byteLast, 8, array, length, 8);
        length += 8;
        floats += 2;
    }

    public int getFloats() {
        return floats;
    }

    public void clear() {
        length = 0;
        floats = 0;
    }

    public Buffer toBuffer() {
        return ByteBuffer.wrap(array).limit(length);
    }

}
