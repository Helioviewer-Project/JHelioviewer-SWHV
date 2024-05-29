package org.helioviewer.jhv.opengl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

import org.helioviewer.jhv.math.Vec3;

public class BufVertex {

    private static final int chunk = 1024;
    private int multiplier = 1;

    private final byte[] byteLast = new byte[16];
    private final FloatBuffer bufferLast = ByteBuffer.wrap(byteLast).order(ByteOrder.nativeOrder()).asFloatBuffer();

    private int count;

    private ByteBuffer vertxBuffer;
    private byte[] arrayVertx;
    private int lengthVertx;

    private ByteBuffer colorBuffer;
    private byte[] arrayColor;
    private int lengthColor;

    public BufVertex(int size) {
        arrayVertx = new byte[Math.max(size, 16)];
        vertxBuffer = ByteBuffer.wrap(arrayVertx);
        size /= 4;
        arrayColor = new byte[Math.max(size, 4)];
        colorBuffer = ByteBuffer.wrap(arrayColor);
    }

    private void ensureVertx(int nbytes) {
        int size = arrayVertx.length;
        if (lengthVertx + nbytes > size) {
            arrayVertx = Arrays.copyOf(arrayVertx, size + chunk * multiplier++);
            vertxBuffer = ByteBuffer.wrap(arrayVertx);
        }
    }

    private void ensureColor(int nbytes) {
        int size = arrayColor.length;
        if (lengthColor + nbytes > size) {
            arrayColor = Arrays.copyOf(arrayColor, size + chunk * multiplier++);
            colorBuffer = ByteBuffer.wrap(arrayColor);
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
        arrayColor[lengthColor++] = b[0];
        arrayColor[lengthColor++] = b[1];
        arrayColor[lengthColor++] = b[2];
        arrayColor[lengthColor++] = b[3];

        count++;
    }

    public int getCount() {
        return count;
    }

    public void clear() {
        lengthVertx = 0;
        lengthColor = 0;
        count = 0;
    }

    public Buffer toVertexBuffer() {
        return vertxBuffer.limit(lengthVertx);
    }

    public Buffer toColorBuffer() {
        return colorBuffer.limit(lengthColor);
    }

    public static BufVertex join(List<BufVertex> list) {
        int listSize = list.size();
        if (listSize == 0)
            throw new IllegalArgumentException("Empty BufVertex list");
        if (listSize == 1)
            return list.get(0);

        int retLengthVertx = 0, retLengthColor = 0, retCount = 0, toCopy;
        for (BufVertex b : list) {
            retLengthVertx += b.lengthVertx;
        }
        BufVertex ret = new BufVertex(retLengthVertx);

        retLengthVertx = 0;
        for (BufVertex b : list) {
            toCopy = b.lengthVertx;
            ret.vertxBuffer.put(retLengthVertx, b.vertxBuffer, 0, toCopy);
            retLengthVertx += toCopy;

            toCopy = b.lengthColor;
            ret.colorBuffer.put(retLengthColor, b.colorBuffer, 0, toCopy);
            retLengthColor += toCopy;

            retCount += b.count;
        }
        ret.lengthVertx = retLengthVertx;
        ret.lengthColor = retLengthColor;
        ret.count = retCount;

        return ret;
    }

}
