package org.helioviewer.jhv.base;

import java.util.Arrays;

public class FloatArray {

    private int len = 0;
    private float[] arr = new float[4 * 256];

    public int length() {
        return len;
    }

    public void put4f(float[] a) {
        int length = arr.length;
        if (len + 4 >= length)
            arr = Arrays.copyOf(arr, 2 * length);
        arr[len]     = a[0];
        arr[len + 1] = a[1];
        arr[len + 2] = a[2];
        arr[len + 3] = a[3];
        len += 4;
    }

    public void put3f(float[] a) {
        int length = arr.length;
        if (len + 3 >= length)
            arr = Arrays.copyOf(arr, 2 * length);
        arr[len]     = a[0];
        arr[len + 1] = a[1];
        arr[len + 2] = a[2];
        len += 3;
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

    public float[] toArray() {
        return arr;
    }

}
