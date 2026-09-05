package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.math.Vec3;

public class BufVertex {

    static final int POSITION_COMPONENTS = 4;
    static final int POSITION_BYTES = POSITION_COMPONENTS * Float.BYTES;
    static final int COLOR_COMPONENTS = 4;
    private static final int COLOR_BYTES = COLOR_COMPONENTS * Byte.BYTES;
    static final int BYTES_PER_VERTEX = POSITION_BYTES + COLOR_BYTES;

    private static final int MIN_CAPACITY = 64;

    private final byte[] lastPosition = new byte[POSITION_BYTES];
    private final FloatBuffer lastPositionFloats = ByteBuffer.wrap(lastPosition).order(ByteOrder.nativeOrder()).asFloatBuffer();

    private int count;

    private byte[] array;
    private ByteBuffer buffer;

    public BufVertex() {
        this(0);
    }

    public BufVertex(int capacity) {
        array = new byte[Math.multiplyExact(capacity, BYTES_PER_VERTEX)];
        buffer = ByteBuffer.wrap(array);
    }

    private void ensureCapacity() {
        int capacity = array.length / BYTES_PER_VERTEX;
        if (count < capacity)
            return;

        int newCapacity = Math.max(MIN_CAPACITY, capacity * 2);
        array = Arrays.copyOf(array, Math.multiplyExact(newCapacity, BYTES_PER_VERTEX));
        buffer = ByteBuffer.wrap(array);
    }

    public void putVertex(Vec3 v, byte[] color) {
        putVertex((float) v.x, (float) v.y, (float) v.z, 1, color);
    }

    public void putVertex(float x, float y, float z, float w, byte[] color) {
        lastPositionFloats.put(0, x).put(1, y).put(2, z).put(3, w);
        repeatVertex(color);
    }

    public void startLine(Vec3 v, byte[] color) {
        startLine((float) v.x, (float) v.y, (float) v.z, 1, color);
    }

    public void startLine(float x, float y, float z, float w, byte[] color) {
        putVertex(x, y, z, w, Colors.Null);
        repeatVertex(color);
    }

    public void endLine() {
        repeatVertex(Colors.Null);
    }

    private void repeatVertex(byte[] color) {
        ensureCapacity();
        int offset = count * BYTES_PER_VERTEX;
        System.arraycopy(lastPosition, 0, array, offset, POSITION_BYTES);
        System.arraycopy(color, 0, array, offset + POSITION_BYTES, COLOR_BYTES);

        count++;
    }

    public void putQuad2D(float left, float bottom, float right, float top, byte[] color) {
        putVertex(left, bottom, 0, 1, color);
        putVertex(right, bottom, 0, 1, color);
        putVertex(right, top, 0, 1, color);
        putVertex(left, bottom, 0, 1, color);
        putVertex(right, top, 0, 1, color);
        putVertex(left, top, 0, 1, color);
    }

    public void putQuad2DStrip(float left, float bottom, float right, float top, byte[] color) {
        putVertex(left, bottom, 0, 1, color);
        putVertex(right, bottom, 0, 1, color);
        putVertex(left, top, 0, 1, color);
        putVertex(right, top, 0, 1, color);
    }

    public int getCount() {
        return count;
    }

    private int byteLength() {
        return count * BYTES_PER_VERTEX;
    }

    public void clear() {
        count = 0;
    }

    public ByteBuffer toBuffer() {
        return buffer.limit(byteLength());
    }

}
