package org.helioviewer.jhv.base;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class BufferUtils {

    public static ByteBuffer newByteBuffer(int len) {
        return ByteBuffer.allocateDirect(len).order(ByteOrder.nativeOrder());
    }

    public static IntBuffer newIntBuffer(int len) {
        return newByteBuffer(Math.multiplyExact(Integer.BYTES, len)).asIntBuffer();
    }

    public static FloatBuffer newFloatBuffer(int len) {
        return newByteBuffer(Math.multiplyExact(Float.BYTES, len)).asFloatBuffer();
    }

    public static ByteBuffer putRemaining(ByteBuffer destination, ByteBuffer source) {
        return putRange(destination, source, source.position(), source.remaining());
    }

    public static ByteBuffer putRange(ByteBuffer destination, ByteBuffer source, int sourcePosition, int count) {
        int position = destination.position();
        return destination.put(position, source, sourcePosition, count).position(position + count);
    }

    public static IntBuffer putRemaining(IntBuffer destination, IntBuffer source) {
        int position = destination.position();
        return destination.put(position, source, source.position(), source.remaining())
                .position(position + source.remaining());
    }

    public static FloatBuffer putRemaining(FloatBuffer destination, FloatBuffer source) {
        int position = destination.position();
        return destination.put(position, source, source.position(), source.remaining())
                .position(position + source.remaining());
    }

    public static ByteBuffer directByteBuffer(ByteBuffer buffer) {
        if (buffer.isDirect())
            return buffer;

        ByteBuffer copy = newByteBuffer(buffer.remaining());
        return putRemaining(copy, buffer).flip();
    }

}
