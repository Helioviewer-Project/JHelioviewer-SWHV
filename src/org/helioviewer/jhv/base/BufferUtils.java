package org.helioviewer.jhv.base;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.helioviewer.jhv.base.math.Vec3;

import com.jogamp.common.nio.Buffers;

public class BufferUtils {

    public static FloatBuffer newFloatBuffer(int len) {
        return Buffers.newDirectFloatBuffer(len);
    }

    public static IntBuffer newIntBuffer(int len) {
        return Buffers.newDirectIntBuffer(len);
    }

    public static void put3f(FloatBuffer buf, float x, float y, float z) {
        buf.put(x);
        buf.put(y);
        buf.put(z);
    }

    public static void put3f(FloatBuffer buf, Vec3 v) {
        buf.put((float) v.x);
        buf.put((float) v.y);
        buf.put((float) v.z);
    }

    public static void put4f(FloatBuffer buf, float r, float g, float b, float a) {
        buf.put(r);
        buf.put(g);
        buf.put(b);
        buf.put(a);
    }

}
