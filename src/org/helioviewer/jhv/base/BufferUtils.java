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

    public static FloatBuffer newFloatBuffer(int len) {
        return newByteBuffer(4 * len).asFloatBuffer();
    }

    public static IntBuffer newIntBuffer(int len) {
        return newByteBuffer(4 * len).asIntBuffer();
    }

    public static ShortBuffer newShortBuffer(int len) {
        return newByteBuffer(2 * len).asShortBuffer();
    }

}
