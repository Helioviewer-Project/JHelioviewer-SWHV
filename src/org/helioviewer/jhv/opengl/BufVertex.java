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

    private int count;
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
        putVertex((float) v.x, (float) v.y, (float) v.z, 1, b);
    }

    public void putVertex(float x, float y, float z, float w, byte[] b) {
        bufferLast.put(0, x).put(1, y).put(2, z).put(3, w);
        repeatVertex(b);
    }

    public void repeatVertex(byte[] b) {
        ensureVertx(16);
        System.arraycopy(byteLast, 0, arrayVertx, lengthVertx, 16);
        lengthVertx += 16;

        ensureColor(4);
        System.arraycopy(b, 0, arrayColor, lengthColor, 4);
        lengthColor += 4;

        count++;
    }

    public int getCount() {
        return lengthVertx;
    }

    public void clear() {
        lengthVertx = 0;
        lengthColor = 0;
        count = 0;
    }

    public Buffer toVertexBuffer() {
        return ByteBuffer.wrap(arrayVertx).limit(lengthVertx);
    }

    public Buffer toColorBuffer() {
        return ByteBuffer.wrap(arrayColor).limit(lengthColor);
    }

}
