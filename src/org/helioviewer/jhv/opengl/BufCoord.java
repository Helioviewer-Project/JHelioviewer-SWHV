package org.helioviewer.jhv.opengl;

import java.nio.Buffer;
import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.math.Vec3;

public class BufCoord {

    private static final int chunk = 6 * 4 * 8;

    private int count;
    private int length;
    private int capacity;
    private FloatBuffer buffer;

    public BufCoord(int size) {
        capacity = size * 6;
        buffer = BufferUtils.newFloatBuffer(capacity);
    }

    private void ensure(int delta) {
        if (length + delta <= capacity)
            return;

        FloatBuffer oldBuffer = buffer;
        oldBuffer.position(0);
        oldBuffer.limit(length);

        capacity = Math.max(length + delta, capacity + chunk);
        buffer = BufferUtils.newFloatBuffer(capacity);
        buffer.put(oldBuffer);
        buffer.clear();
    }

    public void putCoord(float x, float y, float z, float w, float c0, float c1) {
        ensure(6);
        buffer.put(length++, x);
        buffer.put(length++, y);
        buffer.put(length++, z);
        buffer.put(length++, w);
        buffer.put(length++, c0);
        buffer.put(length++, c1);

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
        buffer.clear();
    }

    public Buffer toBuffer() { // Call clear() before appending again.
        buffer.position(0);
        return buffer.limit(length);
    }

}
