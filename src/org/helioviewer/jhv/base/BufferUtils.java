package org.helioviewer.jhv.base;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.jogamp.common.nio.Buffers;

public class BufferUtils {
    public static FloatBuffer genFloatBuffer(int len) {
        return Buffers.newDirectFloatBuffer(len);
    }

    public static IntBuffer genIntBuffer(int len) {
        return Buffers.newDirectIntBuffer(len);
    }

    public static ShortBuffer genShortBuffer(int len) {
        return Buffers.newDirectShortBuffer(len);
    }
}
