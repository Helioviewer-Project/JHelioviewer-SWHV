package org.helioviewer.jhv.opengl;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

import org.helioviewer.jhv.math.Vec3;

public class BufCoord {

    private static final int chunk = 72;

    private int count;
    private int length;
    private float[] array;
    private Buffer buffer;

    public BufCoord(int size) {
        array = new float[size * 6];
        buffer = FloatBuffer.wrap(array);
    }

    private void ensure(int delta) {
        int size = array.length;
        if (length + delta > size) {
            array = Arrays.copyOf(array, Math.max(length + delta, size + chunk));
            buffer = FloatBuffer.wrap(array);
        }
    }

    public void putCoord(float x, float y, float z, float w, float c0, float c1) {
        ensure(6);
        array[length] = x;
        array[length + 1] = y;
        array[length + 2] = z;
        array[length + 3] = w;
        array[length + 4] = c0;
        array[length + 5] = c1;
        length += 6;

        count++;
    }

    public void putCoord(float x, float y, float z, float w, float[] c) {
        putCoord(x, y, z, w, c[0], c[1]);
    }

    public void putCoord(Vec3 v, float[] c) {
        putCoord((float) v.x, (float) v.y, (float) v.z, 1, c[0], c[1]);
    }

    public int getCount() {
        return count;
    }

    public void clear() {
        length = 0;
        count = 0;
    }

    public Buffer toBuffer() {
        return buffer.limit(length);
    }

}
