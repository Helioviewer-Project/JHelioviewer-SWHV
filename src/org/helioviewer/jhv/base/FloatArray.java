package org.helioviewer.jhv.base;

import java.nio.FloatBuffer;
import java.util.Arrays;

import org.helioviewer.jhv.math.Vec3;

public class FloatArray {

    private int len = 0;
    private float[] arr = new float[4 * 256];

    public int length() {
        return len;
    }

    public void put4f(float[] a) {
        int length = arr.length;
        if (len + 4 >= length)
            arr = Arrays.copyOf(arr, 4 * length);
        arr[len]     = a[0];
        arr[len + 1] = a[1];
        arr[len + 2] = a[2];
        arr[len + 3] = a[3];
        len += 4;
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

    public void put4f(Vec3 v) {
        put4f((float) v.x, (float) v.y, (float) v.z, 1);
    }

    public void repeat4f() {
        put4f(arr[len - 4], arr[len - 3], arr[len - 2], arr[len - 1]);
    }

    public FloatBuffer toBuffer() {
        FloatBuffer buf = BufferUtils.newFloatBuffer(len);
        buf.put(arr, 0, len);
        buf.rewind();
        return buf;
    }

}
