package org.helioviewer.jhv.base;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class BufferUtils {

    public static ByteBuffer newByteBuffer(int len) {
        return ByteBuffer.allocateDirect(len).order(ByteOrder.nativeOrder());
    }

    public static ShortBuffer newShortBuffer(int len) {
        return newByteBuffer(2 * len).asShortBuffer();
    }

    public static IntBuffer newIntBuffer(int len) {
        return newByteBuffer(4 * len).asIntBuffer();
    }

    public static FloatBuffer newFloatBuffer(int len) {
        return newByteBuffer(4 * len).asFloatBuffer();
    }

    public static ByteBuffer directByteBuffer(ByteBuffer buffer) {
        if (buffer.isDirect())
            return buffer;

        ByteBuffer copy = newByteBuffer(buffer.remaining());
        copy.put(buffer.duplicate());
        return copy.flip();
    }

    public static ShortBuffer directShortBuffer(ShortBuffer buffer) {
        if (buffer.isDirect())
            return buffer;

        ShortBuffer copy = newShortBuffer(buffer.remaining());
        copy.put(buffer.duplicate());
        return copy.flip();
    }

    public static IntBuffer directIntBuffer(IntBuffer buffer) {
        if (buffer.isDirect())
            return buffer;

        IntBuffer copy = newIntBuffer(buffer.remaining());
        copy.put(buffer.duplicate());
        return copy.flip();
    }

    public static FloatBuffer directFloatBuffer(FloatBuffer buffer) {
        if (buffer.isDirect())
            return buffer;

        FloatBuffer copy = newFloatBuffer(buffer.remaining());
        copy.put(buffer.duplicate());
        return copy.flip();
    }

}
