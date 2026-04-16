package org.helioviewer.jhv.imagedata;

import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class ImageBuffer {

    public static final int BAD_PIXEL = Integer.MIN_VALUE;

    public enum Format {
        Gray8(1), Gray16(2), RGBA32(4);

        public final int bytes;

        Format(int _bytes) {
            bytes = _bytes;
        }
    }

    public final int width;
    public final int height;
    public final Format format;
    public final Buffer buffer;
    private final float[] lut;

    private Arena arena;

    public ImageBuffer(int _width, int _height, Format _format, byte[] data) {
        this(_width, _height, _format, data, ImageFilter.Type.None, null);
    }

    public ImageBuffer(int _width, int _height, Format _format, byte[] data, ImageFilter.Type filterType, float[] _lut) {
        width = _width;
        height = _height;
        format = _format;
        lut = _lut;

        arena = Arena.ofShared();
        buffer = allocate(arena, filter(format, data, _width, _height, filterType));
    }

    public ImageBuffer(int _width, int _height, Format _format, short[] data) {
        this(_width, _height, _format, data, ImageFilter.Type.None, null);
    }

    public ImageBuffer(int _width, int _height, Format _format, short[] data, ImageFilter.Type filterType, float[] _lut) {
        width = _width;
        height = _height;
        format = _format;
        lut = _lut;

        arena = Arena.ofShared();
        buffer = allocate(arena, filter(format, data, _width, _height, filterType));
    }

    private int getPixelInternal(int x, int y) {
        if (x < 0 || x > width - 1 || y < 0 || y > height - 1 || format == Format.RGBA32)
            return BAD_PIXEL;

        int idx = x + y * width;
        if (buffer instanceof ByteBuffer bb)
            return (bb.get(idx) + 256) & 0xFF;
        if (buffer instanceof ShortBuffer sb)
            return (sb.get(idx) + 65536) & 0xFFFF;
        return BAD_PIXEL;
    }

    float getPixel(int x, int y, float[] metaLUT) {
        int p = getPixelInternal(x, y);
        if (p == BAD_PIXEL)
            return BAD_PIXEL;

        if (lut != null) {
            return lut[Math.clamp(p, 0, lut.length - 1)];
        }
        if (metaLUT != null) {
            return metaLUT[Math.clamp(p, 0, metaLUT.length - 1)];
        }
        return p;
    }

    boolean hasLUT() {
        return lut != null;
    }

    public int byteSize() {
        return width * height * format.bytes;
    }

    public void free() {
        if (arena == null)
            return;
        arena.close();
        arena = null;
    }

    private static Buffer allocate(Arena arena, byte[] data) {
        ByteBuffer buffer = arena.allocateFrom(ValueLayout.JAVA_BYTE, data).asByteBuffer();
        return buffer;
    }

    private static Buffer allocate(Arena arena, short[] data) {
        ShortBuffer buffer = arena.allocateFrom(ValueLayout.JAVA_SHORT, data)
                .asByteBuffer()
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        return buffer;
    }

    private static byte[] filter(Format format, byte[] data, int width, int height, ImageFilter.Type filterType) {
        return format == Format.RGBA32 ? data : ImageFilter.filter(data, width, height, filterType);
    }

    private static short[] filter(Format format, short[] data, int width, int height, ImageFilter.Type filterType) {
        return format == Format.RGBA32 ? data : ImageFilter.filter(data, width, height, filterType);
    }

}
