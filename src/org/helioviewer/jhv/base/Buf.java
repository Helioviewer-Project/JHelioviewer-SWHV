package org.helioviewer.jhv.base;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import org.helioviewer.jhv.math.Vec3;

public class Buf {

    private final byte[] byteLast = new byte[16];
    private final FloatBuffer bufferLast = ByteBuffer.wrap(byteLast).order(ByteOrder.nativeOrder()).asFloatBuffer();

    private byte[] array;
    private ByteBuffer buffer;
    private int length;
    private int floats;
    private int bytes;

    public Buf(int size) {
        array = new byte[size < 16 ? 16 : size];
        buffer = ByteBuffer.wrap(array);
    }

    private void realloc(int size) {
        array = Arrays.copyOf(array, size);
        buffer = ByteBuffer.wrap(array);
    }

    public Buf put4f(Vec3 v) {
        return put4f((float) v.x, (float) v.y, (float) v.z, 1);
    }

    public Buf put4f(float x, float y, float z, float w) {
        bufferLast.put(0, x).put(1, y).put(2, z).put(3, w);
        return repeat4f();
    }

    public Buf repeat4f() {
        int size = array.length;
        if (length + 16 >= size)
            realloc(2 * size);

        System.arraycopy(byteLast, 0, array, length, 16);
        length += 16;
        floats += 4;
        return this;
    }

    public void put4b(byte[] b) {
        int size = array.length;
        if (length + 4 >= size)
            realloc(2 * size);

        array[length]     = b[0];
        array[length + 1] = b[1];
        array[length + 2] = b[2];
        array[length + 3] = b[3];
        length += 4;
        bytes++;
    }

    public int getFloats() {
        return floats;
    }

    public int getBytes4() {
        return bytes;
    }

    public void rewind() {
        length = 0;
        floats = 0;
        bytes = 0;
    }

    public Buffer toBuffer() {
        return buffer.limit(length);
    }

}
