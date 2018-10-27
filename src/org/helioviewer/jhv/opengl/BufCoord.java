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

    private int count;
    private byte[] arrayVertx;
    private int lengthVertx;
    private byte[] arrayCoord;
    private int lengthCoord;

    public BufCoord(int size) {
        arrayVertx = new byte[size < 16 ? 16 : size];
        size /= 2;
        arrayCoord = new byte[size < 8 ?  8 : size];
    }

    private void ensureVertx(int nbytes) {
        int size = arrayVertx.length;
        if (lengthVertx + nbytes > size) {
            arrayVertx = Arrays.copyOf(arrayVertx, size + chunk * multiplier++);
        }
    }

    private void ensureCoord(int nbytes) {
        int size = arrayCoord.length;
        if (lengthCoord + nbytes > size) {
            arrayCoord = Arrays.copyOf(arrayCoord, size + chunk * multiplier++);
        }
    }

    public void putCoord(float x, float y, float z, float w, float c0, float c1) {
        ensureVertx(16);
        bufferLast.put(0, x).put(1, y).put(2, z).put(3, w);
        System.arraycopy(byteLast, 0, arrayVertx, lengthVertx, 16);
        lengthVertx += 16;

        ensureCoord(8);
        bufferLast.put(2, c0).put(3, c1);
        System.arraycopy(byteLast, 8, arrayCoord, lengthCoord, 8);
        lengthCoord += 8;

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
        lengthVertx = 0;
        lengthCoord = 0;
        count = 0;
    }

    public Buffer toVertexBuffer() {
        return ByteBuffer.wrap(arrayVertx).limit(lengthVertx);
    }

    public Buffer toCoordBuffer() {
        return ByteBuffer.wrap(arrayCoord).limit(lengthCoord);
    }

}
