package org.helioviewer.jhv.opengl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import org.helioviewer.jhv.math.Vec3;

public class BufVertex {

    private static final int chunk = 1024;
    private int multiplier = 1;

    private final byte[] byteLast = new byte[16];
    private final FloatBuffer bufferLast = ByteBuffer.wrap(byteLast).order(ByteOrder.nativeOrder()).asFloatBuffer();

    private byte[] arrayVertx;
    private int lengthVertx;
    private byte[] arrayColor;
    private int lengthColor;

    public BufVertex(int size) {
        arrayVertx = new byte[size < 16 ? 16 : size];
        size /= 4;
        arrayColor = new byte[size < 4 ? 4 : size];
    }

    private void ensureVertx(int nbytes) {
        int size = arrayVertx.length;
        if (lengthVertx + nbytes > size) {
            arrayVertx = Arrays.copyOf(arrayVertx, size + chunk * multiplier++);
        }
    }

    private void ensureColor(int nbytes) {
        int size = arrayColor.length;
        if (lengthColor + nbytes > size) {
            arrayColor = Arrays.copyOf(arrayColor, size + chunk * multiplier++);
        }
    }

    public void putVertex(Vec3 v, byte[] b) {
        put4f((float) v.x, (float) v.y, (float) v.z, 1);
        put4b(b);
    }

    public void putVertex(float x, float y, float z, float w, byte[] b) {
        put4f(x, y, z, w);
        put4b(b);
    }

    public void repeatVertex(byte[] b) {
        repeat4f();
        put4b(b);
    }

    public BufVertex put4f(float x, float y, float z, float w) {
        bufferLast.put(0, x).put(1, y).put(2, z).put(3, w);
        repeat4f();
        return this;
    }

    private void repeat4f() {
        ensureVertx(16);
        System.arraycopy(byteLast, 0, arrayVertx, lengthVertx, 16);
        lengthVertx += 16;
    }

    private void put4b(byte[] b) {
        ensureColor(4);
        System.arraycopy(b, 0, arrayColor, lengthColor, 4);
        lengthColor += 4;
    }

    public int getVertexLength() {
        return lengthVertx;
    }

    public int getColorLength() {
        return lengthColor;
    }

    public void clear() {
        lengthVertx = 0;
        lengthColor = 0;
    }

    public Buffer toVertexBuffer() {
        return ByteBuffer.wrap(arrayVertx).limit(lengthVertx);
    }

    public Buffer toColorBuffer() {
        return ByteBuffer.wrap(arrayColor).limit(lengthColor);
    }

}
