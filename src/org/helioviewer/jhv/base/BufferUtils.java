package org.helioviewer.jhv.base;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class BufferUtils {
    public static FloatBuffer genFloatBuffer(int len) {
        ByteBuffer temp = ByteBuffer.allocateDirect(len * 8);
        temp.order(ByteOrder.nativeOrder());
        return temp.asFloatBuffer();
    }

    public static IntBuffer genIntBuffer(int len) {
        ByteBuffer temp = ByteBuffer.allocateDirect(len * 8);
        temp.order(ByteOrder.nativeOrder());
        return temp.asIntBuffer();
    }

    public static ShortBuffer genShortBuffer(int len) {
        ByteBuffer temp = ByteBuffer.allocateDirect(len * 4);
        temp.order(ByteOrder.nativeOrder());
        return temp.asShortBuffer();
    }
}
