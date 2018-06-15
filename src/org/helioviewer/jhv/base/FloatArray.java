package org.helioviewer.jhv.base;

import java.nio.FloatBuffer;
import java.util.Arrays;

public class FloatArray {

    private int len = 0;
    private float[] arr = new float[4 * 128];

    public int length() {
        return len;
    }

    public float[] get() {
        return arr;
    }

    public void put4f(float[] c) {
        put4f(c[0], c[1], c[2], c[3]);
    }

    public void put4f(float x, float y, float z, float w) {
        int length = arr.length;
        if (len + 4 >= length)
            arr = Arrays.copyOf(arr, 2 * length);
        arr[len]     = x;
        arr[len + 1] = y;
        arr[len + 2] = z;
        arr[len + 3] = w;
        len += 4;
    }

    public void put3f(float x, float y, float z) {
        int length = arr.length;
        if (len + 3 >= length)
            arr = Arrays.copyOf(arr, 2 * length);
        arr[len]     = x;
        arr[len + 1] = y;
        arr[len + 2] = z;
        len += 3;
    }

    public void repeat3f() {
        put3f(arr[len - 3], arr[len - 2], arr[len - 1]);
    }

    public FloatBuffer toBuffer() {
        FloatBuffer buf = BufferUtils.newFloatBuffer(len);
        buf.put(arr, 0, len);
        buf.rewind();
        return buf;
    }

}
