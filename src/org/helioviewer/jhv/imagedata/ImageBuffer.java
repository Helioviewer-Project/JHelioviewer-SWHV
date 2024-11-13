package org.helioviewer.jhv.imagedata;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class ImageBuffer {

    public static final int BAD_PIXEL = Integer.MIN_VALUE;

    public enum Format {
        Gray8(1), Gray16(2), ARGB32(4);

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

    public ImageBuffer(int _width, int _height, Format _format, Buffer _buffer) {
        this(_width, _height, _format, _buffer, null);
    }

    public ImageBuffer(int _width, int _height, Format _format, Buffer _buffer, float[] _lut) {
        width = _width;
        height = _height;
        format = _format;
        buffer = _buffer;
        lut = _lut;
    }

    public static ImageBuffer filter(ImageBuffer ib, ImageFilter.Type filterType) throws Exception {
        if (filterType == ImageFilter.Type.None || ib.format == Format.ARGB32)
            return ib;
        return new ImageBuffer(ib.width, ib.height, ib.format, ImageFilter.mgn(ib.buffer, ib.width, ib.height), ib.lut);
    }

    private int getPixelInternal(int x, int y) {
        if (x < 0 || x > width - 1 || y < 0 || y > height - 1 || format == Format.ARGB32)
            return BAD_PIXEL;

        int idx = x + y * width;
        if (buffer instanceof ByteBuffer)
            return (((ByteBuffer) buffer).get(idx) + 256) & 0xFF;
        if (buffer instanceof ShortBuffer)
            return (((ShortBuffer) buffer).get(idx) + 65536) & 0xFFFF;
        return BAD_PIXEL;
    }

    float getPixel(int x, int y, float[] metaLUT) {
        int p = getPixelInternal(x, y);
        if (p == BAD_PIXEL)
            return BAD_PIXEL;

        if (lut != null)
            return lut[p];
        if (metaLUT != null)
            return metaLUT[p];
        return p;
    }

    boolean hasLUT() {
        return lut != null;
    }

}
