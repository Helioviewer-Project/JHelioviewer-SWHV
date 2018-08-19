package org.helioviewer.jhv.base;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ByteArray {

    private int len = 0;
    private byte[] arr = new byte[4 * 256];

    public int length() {
        return len;
    }

    public void put4b(byte[] a) {
        int length = arr.length;
        if (len + 4 >= length)
            arr = Arrays.copyOf(arr, 4 * length);
        arr[len]     = a[0];
        arr[len + 1] = a[1];
        arr[len + 2] = a[2];
        arr[len + 3] = a[3];
        len += 4;
    }

    public ByteBuffer toBuffer() {
        return ByteBuffer.wrap(arr, 0, len);
    }

}
