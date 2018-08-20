package org.helioviewer.jhv.base;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.helioviewer.jhv.math.Vec3;

public class Buf {

    private final ByteBuf buf;
    private final float[] last = new float[4];

    private int floats;
    private int bytes;

    public Buf(int len) {
        buf = Unpooled.directBuffer(len);
    }

    public void put2f(float[] f) {
        put2f(f[0], f[1]);
    }

    public void put2f(float x, float y) {
        buf.writeFloatLE(x).writeFloatLE(y);
        floats += 2;
    }

    public Buf put4f(Vec3 v) {
        return put4f((float) v.x, (float) v.y, (float) v.z, 1);
    }

    public Buf put4f(float x, float y, float z, float w) {
        last[0] = x;
        last[1] = y;
        last[2] = z;
        last[3] = w;
        return repeat4f();
    }

    public Buf repeat4f() {
        buf.writeFloatLE(last[0]).writeFloatLE(last[1]).writeFloatLE(last[2]).writeFloatLE(last[3]);
        floats += 4;
        return this;
    }

    public void put4b(byte[] b) {
        buf.writeBytes(b, 0, 4);
        bytes++;
    }

    public int getFloats() {
        return floats;
    }

    public int getBytes4() {
        return bytes;
    }

    public void rewind() {
        buf.setIndex(0, 0);
        floats = 0;
        bytes = 0;
    }

    public ByteBuffer toBuffer() {
        return buf.nioBuffer();
    }

}
